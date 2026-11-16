"""Time-derived suspension state and explicit moderation transitions."""

from datetime import datetime, timezone

from .backend import PortalError


def is_suspended(account):
    value = account.get("banned_until")
    if value is None or value == "":
        return False
    try:
        expiry = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if expiry.tzinfo is None:
            raise ValueError
    except (AttributeError, TypeError, ValueError):
        raise PortalError("An account's suspension state could not be verified.", 502) from None
    return expiry > datetime.now(timezone.utc)


def require_suspension_transition(account, operation):
    suspended = is_suspended(account)
    if operation == "suspend" and suspended:
        raise PortalError("This account is already suspended. Manage its existing suspension from Manage users.", 409)
    if operation in ("update", "restore") and not suspended:
        raise PortalError("This account is no longer suspended. Reload before acting.", 409)
