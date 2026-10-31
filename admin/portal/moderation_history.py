"""Report-scoped moderation notes and activity history."""

from .backend import PortalError
from .moderation import identifier

EVENT_COLUMNS = "id,report_id,moderator_id,event_type,comment,created_at"
MAX_COMMENT_LENGTH = 4000


def report_events(backend, report_id):
    report_id = identifier(report_id)
    events = backend.rows(
        "moderation_events", select=EVENT_COLUMNS,
        report_id="eq." + report_id, order="created_at.desc,id.desc", limit=500,
    )
    if not isinstance(events, list) or any(not isinstance(event, dict) for event in events):
        raise PortalError("Moderation history could not be loaded. Please try again.", 502)
    for event in events:
        moderator_id = event.get("moderator_id")
        event["moderator"] = (
            backend.one("profiles", identifier(moderator_id), "id,full_name,email")
            if moderator_id else None
        )
    return events


def add_report_event(backend, report_id, moderator_id, event_type, comment=None):
    report_id = identifier(report_id)
    moderator_id = identifier(moderator_id)
    if not isinstance(event_type, str) or not event_type.strip() or len(event_type) > 80:
        raise PortalError("Choose a valid moderation event.")
    comment = normalize_comment(comment, allow_empty=True)
    result = backend.request(
        "POST", "/rest/v1/moderation_events", query={"select": EVENT_COLUMNS},
        data={"report_id": report_id, "moderator_id": moderator_id,
              "event_type": event_type.strip(), "comment": comment},
    )
    if (not isinstance(result, list) or len(result) != 1 or
            not isinstance(result[0], dict) or not result[0].get("id") or
            not result[0].get("created_at")):
        raise PortalError("The moderation event could not be confirmed. Reload before retrying.", 502)
    return result[0]


def record_action_event(backend, report_id, moderator_id, event_type, action_name, comment=None):
    """Report a failed audit write without hiding that the action already ran."""
    try:
        return add_report_event(backend, report_id, moderator_id, event_type, comment)
    except PortalError:
        raise PortalError(
            action_name + " was applied, but its history entry could not be confirmed. "
            "Reload before retrying.", 502
        ) from None


def normalize_comment(comment, allow_empty=False):
    if comment is None:
        return None
    if not isinstance(comment, str):
        raise PortalError("Enter a valid moderation comment.")
    comment = comment.strip()
    if not comment and allow_empty:
        return None
    if not comment or len(comment) > MAX_COMMENT_LENGTH:
        raise PortalError("Comments must be between 1 and 4000 characters.")
    return comment
