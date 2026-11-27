"""Evidence snapshot and conditional filters for reviewed listing content."""

from .backend import PortalError

LISTING_REVIEW_FIELDS = ("id", "user_id", "name", "description", "price", "is_sold", "edited_at")
LISTING_REVIEW_COLUMNS = ",".join(LISTING_REVIEW_FIELDS)


def listing_snapshot(listing):
    return {key: listing.get(key) for key in LISTING_REVIEW_FIELDS}


def listing_delete_filters(listing):
    # Null means never edited; an absent field means we cannot confirm the evidence.
    if any(key not in listing for key in LISTING_REVIEW_FIELDS):
        raise PortalError("The listing evidence could not be confirmed. Reload before deleting.", 502)
    filters = {}
    for key in ("name", "description", "price", "is_sold", "edited_at"):
        value = listing[key]
        if value is None:
            filters[key] = "is.null"
        elif isinstance(value, bool):
            filters[key] = "eq." + str(value).lower()
        elif key in ("name", "description"):
            # PostgREST quoted literals preserve punctuation and escaped quotes/backslashes.
            filters[key] = 'eq."' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'
        else:
            filters[key] = "eq." + str(value)
    return filters
