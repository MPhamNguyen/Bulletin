"""Listing evidence and destructive moderation through existing cascade rules."""

from urllib.parse import quote

from .backend import PortalError
from .moderation import identifier, snapshot

IMAGE_BUCKET = "listing_imgs"


def listing_snapshot(listing):
    return {key: listing.get(key) for key in ("id", "user_id", "updated_at")}


def image_paths(backend, listing_id):
    listing_id = identifier(listing_id)
    rows = backend.rows("listing_images", listing_id="eq." + listing_id,
                        select="object_path,position", order="position.asc,id.asc")
    return validated_image_paths(rows, listing_id)


def validated_image_paths(rows, listing_id):
    paths = []
    for row in rows:
        path = row.get("object_path")
        if (not isinstance(path, str) or not path.startswith(listing_id + "/")
                or any(part in ("", ".", "..") for part in path.split("/"))
                or "\\" in path or any(ord(char) < 32 for char in path)):
            raise PortalError("A listing photo has an invalid storage path. No deletion was attempted.", 502)
        if path not in paths:
            paths.append(path)
    return paths


def listing_images(backend, listing):
    paths = image_paths(backend, listing["id"])
    return public_image_urls(backend, paths)


def public_image_urls(backend, paths):
    base = backend.url + "/storage/v1/object/public/" + IMAGE_BUCKET + "/"
    return [base + quote(path, safe="/") for path in paths]


def cleanup_images(backend, paths):
    if not paths:
        return True
    # Exact object removals are idempotent. Never delete a bucket or a directory.
    for _ in range(2):
        try:
            backend.request("DELETE", "/storage/v1/object/" + IMAGE_BUCKET,
                            data={"prefixes": paths})
            return True
        except PortalError:
            continue
    return False


def delete_reported_listing(backend, report, version):
    if report.get("type") != "listing" or not report.get("listing_id"):
        raise PortalError("Only a listing report can be used to delete a listing.")
    if version.get("report") != snapshot(report):
        raise PortalError("This report changed. Reload before deleting the listing.", 409)
    listing_id = identifier(report["listing_id"])
    listing = backend.one("listings", listing_id, "id,user_id,updated_at")
    if not listing:
        raise PortalError("The reported listing is no longer available.", 409)
    if version.get("listing") != listing_snapshot(listing):
        raise PortalError("This listing changed. Reload and review it before deleting.", 409)
    paths = image_paths(backend, listing_id)
    modified = listing.get("updated_at")
    rows = backend.request("DELETE", "/rest/v1/listings", query={
        "id": "eq." + listing_id, "user_id": "eq." + identifier(listing["user_id"]),
        "updated_at": "is.null" if modified is None else "eq." + modified,
        "select": "id",
    })
    if not rows:
        raise PortalError("The listing changed or was deleted. Reload before retrying.", 409)
    if not isinstance(rows, list) or len(rows) != 1 or rows[0] != {"id": listing_id}:
        raise PortalError("Deletion could not be confirmed. Refresh before retrying.", 502)
    # reports.listing_id and listing_images.listing_id both use ON DELETE CASCADE.
    # Never remove reports separately: a failed listing delete must retain evidence.
    return listing_id, cleanup_images(backend, paths)
