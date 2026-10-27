"""Search public profile information and browse a user's listings."""

import json
from urllib.parse import urlsplit
from uuid import UUID

from .backend import PortalError
from .listing_review import public_image_urls, validated_image_paths
from .moderation import PAGE_SIZE, identifier

PUBLIC_PROFILE_COLUMNS = "id,full_name,major,university,bio,avatar_url,is_verified,deleted_at"
DIRECTORY_LISTING_COLUMNS = (
    "id,user_id,name,description,price,category,condition,is_sold,created_at,"
    "listing_images(object_path,position)"
)


def search_profiles(backend, search, page):
    query = dict(select=PUBLIC_PROFILE_COLUMNS, order="full_name.asc.nullslast,id.asc",
                 offset=(page - 1) * PAGE_SIZE, limit=PAGE_SIZE + 1)
    if search:
        pattern = "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        quoted = json.dumps(pattern, ensure_ascii=False)
        terms = ["full_name.ilike." + quoted, "email.ilike." + quoted]
        try:
            terms.append("id.eq." + str(UUID(search)))
        except ValueError:
            pass
        query["or"] = "(" + ",".join(terms) + ")"
    rows = backend.rows("profiles", **query)
    return rows[:PAGE_SIZE], len(rows) > PAGE_SIZE


def public_profile(backend, user_id):
    profile = backend.one("profiles", identifier(user_id), PUBLIC_PROFILE_COLUMNS)
    if not profile:
        raise PortalError("User profile not found.", 404)
    return profile


def profile_avatar(backend, profile):
    value = profile.get("avatar_url")
    if not isinstance(value, str):
        return None
    try:
        avatar, origin = urlsplit(value), urlsplit(backend.url)
    except ValueError:
        return None
    if (avatar.scheme == "https" and avatar.netloc == origin.netloc
            and avatar.path.startswith("/storage/v1/object/public/pfp/")
            and not avatar.username and not avatar.fragment):
        return value
    return None


def profile_listings_page(backend, user_id, page):
    rows = backend.rows("listings", select=DIRECTORY_LISTING_COLUMNS,
                        user_id="eq." + identifier(user_id), order="created_at.desc,id.desc",
                        offset=(page - 1) * PAGE_SIZE, limit=PAGE_SIZE + 1,
                        **{"listing_images.order": "position.asc"})
    listings = []
    for row in rows[:PAGE_SIZE]:
        photos, error = [], None
        try:
            paths = validated_image_paths(row.get("listing_images") or [], identifier(row["id"]))
            photos = public_image_urls(backend, paths)
        except PortalError:
            error = "Listing photos could not be loaded."
        listings.append({"listing": row, "photos": photos, "photo_error": error})
    return listings, len(rows) > PAGE_SIZE
