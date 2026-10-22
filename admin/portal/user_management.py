"""Suspended-account browsing and moderation independent of reports."""

from datetime import datetime, timezone

from .account_access import account_snapshot
from .backend import PortalError
from .moderation import (
    PAGE_SIZE, PROFILE_COLUMNS, REPORT_COLUMNS, account_ban_duration,
    ensure_account_action_allowed, identifier,
)


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


def suspended_users_page(backend, page, search):
    accounts = backend.auth_users(page, search)
    suspended = [account for account in accounts if is_suspended(account)]
    ids = [identifier(account.get("id")) for account in suspended]
    profiles = backend.rows("profiles", select=PROFILE_COLUMNS,
                            id="in.(" + ",".join(ids) + ")", limit=100) if ids else []
    by_id = {profile["id"]: profile for profile in profiles}
    return [{"account": account, "profile": by_id.get(account["id"])}
            for account in suspended], len(accounts) == 100


def managed_account(backend, user_id):
    user_id = identifier(user_id)
    account = backend.auth_user(user_id)
    if not account:
        raise PortalError("Account not found.", 404)
    if not isinstance(account, dict) or account.get("id") != user_id:
        raise PortalError("The account could not be verified.", 502)
    profile = backend.one("profiles", user_id, PROFILE_COLUMNS)
    return account, profile


def account_reports_page(backend, user_id, page):
    user_id = identifier(user_id)
    rows = backend.rows("reports", **{
        "select": REPORT_COLUMNS + ",reported_messages:messages(),reported_listings:listings()",
        "reported_messages.sender_id": "eq." + user_id,
        "reported_listings.user_id": "eq." + user_id,
        "or": "(reported_user_id.eq." + user_id
              + ",reported_messages.not.is.null,reported_listings.not.is.null)",
        "order": "created_at.desc,id.desc",
        "offset": (page - 1) * PAGE_SIZE,
        "limit": PAGE_SIZE + 1,
    })
    return rows[:PAGE_SIZE], len(rows) > PAGE_SIZE


def update_suspended_account(backend, user_id, actor_id, version, action, amount, unit):
    duration = account_ban_duration(action, amount, unit)
    account, profile = managed_account(backend, user_id)
    ensure_account_action_allowed(profile, account, actor_id)
    if version != account_snapshot(account) or not is_suspended(account):
        raise PortalError("This account's suspension changed. Reload before acting.", 409)
    backend.set_ban(user_id, duration)
