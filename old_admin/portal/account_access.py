"""Current account checks for server operations that bypass database RLS."""

from datetime import datetime, timezone

from .backend import PortalError


def account_snapshot(account):
    return {key: account.get(key) for key in ("id", "banned_until", "updated_at")}


def require_active_account(backend, user_id):
    account = backend.auth_user(user_id)
    if not isinstance(account, dict) or account.get("id") != user_id:
        raise PortalError("The administrator account is unavailable.", 403)
    banned_until = account.get("banned_until")
    if banned_until is None or banned_until == "":
        return
    try:
        expires = datetime.fromisoformat(banned_until.replace("Z", "+00:00"))
        if expires.tzinfo is None:
            raise ValueError
    except (AttributeError, TypeError, ValueError):
        raise PortalError("The administrator account's suspension state could not be verified.", 503) from None
    if expires > datetime.now(timezone.utc):
        raise PortalError("This administrator account is suspended.", 403)
