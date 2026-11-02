"""Admin-only public profile directory and listing views."""

from flask import Blueprint, g, render_template, request

from .backend import PortalError
from .user_directory import profile_avatar, profile_listings_page, public_profile, search_profiles

directory_routes = Blueprint("directory", __name__)


def directory_page():
    try:
        page = int(request.args.get("page", "1"))
        if not 1 <= page <= 100000:
            raise ValueError
    except ValueError:
        raise PortalError("Invalid page number.") from None
    return page


@directory_routes.get("/directory")
def index():
    page = directory_page()
    search = request.args.get("q", "").strip()
    if len(search) > 320 or any(ord(char) < 32 for char in search):
        raise PortalError("Enter a search of up to 320 characters.")
    users, has_next = search_profiles(g.backend, search, page)
    return render_template("directory.html", users=users, search=search, page=page, has_next=has_next)


@directory_routes.get("/directory/<uuid:user_id>")
def detail(user_id):
    page = directory_page()
    profile = public_profile(g.backend, str(user_id))
    listings, has_next, listing_error = [], False, None
    try:
        listings, has_next = profile_listings_page(g.backend, str(user_id), page)
    except PortalError:
        listing_error = "Listings could not be loaded. Reload the page to try again."
    return render_template("public_user.html", profile=profile, avatar=profile_avatar(g.backend, profile),
                           listings=listings, page=page, has_next=has_next, listing_error=listing_error)
