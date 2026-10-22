"""Report review rules and target resolution, independent of HTML routes."""

from datetime import datetime, timezone
from uuid import UUID

from .account_access import account_snapshot, require_active_account
from .backend import PortalError

STATUSES = {
    "open": "Open",
    "in_review": "Under review",
    "resolved": "Action taken",
    "dismissed": "Dismissed",
}
ALIASES = {"under_review": "in_review", "closed": "resolved", "action_taken": "resolved"}
ACCOUNT_ACTIONS = {
    "suspend": "Suspend account",
    "restore": "Restore account access",
}
SUSPENSION_UNITS = {"days": 1440, "hours": 60, "minutes": 1}
MAX_SUSPENSION_MINUTES = 365 * 1440
REPORT_COLUMNS = (
    "id,type,status,reporter_id,reported_user_id,listing_id,message_id,"
    "reason,description,created_at,modified_at"
)
PROFILE_COLUMNS = "id,full_name,email,is_admin,deleted_at"
PAGE_SIZE = 25


def identifier(value):
    try:
        return str(UUID(str(value)))
    except (ValueError, TypeError, AttributeError):
        raise PortalError("Invalid record ID.", 400) from None


def admin_profile(backend, user):
    profile = backend.one("profiles", identifier(user.get("id")), PROFILE_COLUMNS)
    if not profile or profile.get("is_admin") is not True or profile.get("deleted_at"):
        raise PortalError("Access is restricted to active administrator accounts.", 403)
    require_active_account(backend, profile["id"])
    return profile


def report_by_id(backend, report_id):
    report = backend.one("reports", identifier(report_id), REPORT_COLUMNS)
    if not report:
        raise PortalError("Report not found.", 404)
    return report


def reports_page(backend, status, report_type, page):
    query = dict(select=REPORT_COLUMNS, order="created_at.desc,id.desc",
                 offset=page * PAGE_SIZE, limit=PAGE_SIZE + 1)
    if status == "active":
        query["status"] = "in.(open,in_review)"
    elif status in STATUSES:
        query["status"] = "eq." + status
    elif status != "all":
        raise PortalError("Invalid report status filter.")
    if report_type in ("user", "message", "listing"):
        query["type"] = "eq." + report_type
    elif report_type != "all":
        raise PortalError("Invalid report type filter.")
    rows = backend.rows("reports", **query)
    return rows[:PAGE_SIZE], len(rows) > PAGE_SIZE


def target_context(backend, report):
    kind = report.get("type")
    content = None
    target_id = None
    if kind == "user":
        target_id = report.get("reported_user_id")
    elif kind == "message" and report.get("message_id"):
        content = backend.one("messages", identifier(report["message_id"]),
                              "id,sender_id,body,created_at,conversation_id")
        target_id = content.get("sender_id") if content else None
    elif kind == "listing" and report.get("listing_id"):
        content = backend.one("listings", identifier(report["listing_id"]),
                              "id,user_id,name,description,price,is_sold,updated_at")
        target_id = content.get("user_id") if content else None
    profile = None
    account = None
    if target_id:
        target_id = identifier(target_id)
        profile = backend.one("profiles", target_id, PROFILE_COLUMNS)
        if profile:
            account = backend.auth_user(target_id)
    return {"content": content, "target": profile, "account": account}


def update_status(backend, report, status, version):
    if status not in STATUSES:
        raise PortalError("Choose a valid report status.")
    if version != snapshot(report):
        raise PortalError("Another reviewer changed this report. Reload before saving.", 409)
    if status == report.get("status"):
        return False
    query = {"id": "eq." + identifier(report["id"]), "select": "id,status,modified_at"}
    for field in ("status", "modified_at"):
        value = report.get(field)
        query[field] = "is.null" if value is None else "eq." + value
    rows = backend.request(
        "PATCH", "/rest/v1/reports", query=query,
        data={"status": status, "modified_at": datetime.now(timezone.utc).isoformat()},
    )
    if not rows:
        raise PortalError("Another reviewer changed this report. Reload before saving.", 409)
    if (not isinstance(rows, list) or len(rows) != 1 or not isinstance(rows[0], dict)
            or rows[0].get("id") != report["id"] or rows[0].get("status") != status
            or not rows[0].get("modified_at")):
        raise PortalError("The saved report could not be confirmed. Reload before retrying.", 502)
    return True


def snapshot(report):
    return {key: report.get(key) for key in ("id", "status", "modified_at")}


def account_ban_duration(action, amount, unit):
    if action not in ACCOUNT_ACTIONS:
        raise PortalError("Choose a valid account action.")
    if action == "restore":
        return "none"
    if (not isinstance(amount, str) or not amount.isascii() or not amount.isdigit()
            or len(amount) > 6 or unit not in SUSPENSION_UNITS):
        raise PortalError("Enter a whole-number suspension duration in days, hours, or minutes.")
    minutes = int(amount) * SUSPENSION_UNITS[unit]
    if not 1 <= minutes <= MAX_SUSPENSION_MINUTES:
        raise PortalError("Suspension must last at least 1 minute and no more than 365 days.")
    return str(minutes) + "m"


def act_on_account(backend, report, actor_id, action, amount=None, unit=None, account_version=None):
    duration = account_ban_duration(action, amount, unit)
    context = target_context(backend, report)
    target = context["target"]
    ensure_account_action_allowed(target, context["account"], actor_id)
    if account_version != account_snapshot(context["account"]):
        raise PortalError("This account changed since you opened the report. Reload before acting.", 409)
    # Auth Admin has no conditional update API; this rejects stale forms but
    # cannot make the check and subsequent remote update one atomic operation.
    backend.set_ban(identifier(target["id"]), duration)
    return target["id"]


def ensure_account_action_allowed(target, account, actor_id):
    if not target or not account:
        raise PortalError("The reported account is unavailable. No action was taken.", 409)
    if target.get("deleted_at"):
        raise PortalError("This profile was deleted. Account actions are unavailable.", 409)
    if target["id"] == actor_id or target.get("is_admin") is not False:
        raise PortalError("Actions against your own account or administrator accounts are blocked.", 403)
