"""Read chat evidence only through the conversation of a reported message."""

from .backend import PortalError
from .moderation import identifier, report_by_id

CHAT_PAGE_SIZE = 50
MESSAGE_COLUMNS = "id,conversation_id,sender_id,body,created_at"


def reported_conversation(backend, report_id):
    report = report_by_id(backend, report_id)
    if report.get("type") != "message" or not report.get("message_id"):
        raise PortalError("Conversation history is available only for message reports.", 404)
    message = backend.one("messages", identifier(report["message_id"]), MESSAGE_COLUMNS)
    if not message or not message.get("conversation_id"):
        raise PortalError("The reported message or its conversation is no longer available.", 404)
    return report, message


def conversation_messages_page(backend, reported_message, page):
    conversation_id = identifier(reported_message["conversation_id"])
    rows = backend.rows("messages", select=MESSAGE_COLUMNS,
                        conversation_id="eq." + conversation_id,
                        order="created_at.asc,id.asc", offset=(page - 1) * CHAT_PAGE_SIZE,
                        limit=CHAT_PAGE_SIZE + 1)
    messages = rows[:CHAT_PAGE_SIZE]
    # Also check the response before exposing private conversation content.
    if any(row.get("conversation_id") != conversation_id for row in messages):
        raise PortalError("The conversation history could not be verified.", 502)
    return messages, len(rows) > CHAT_PAGE_SIZE


def conversation_senders(backend, messages):
    ids = sorted({identifier(message["sender_id"]) for message in messages if message.get("sender_id")})
    if not ids:
        return {}
    profiles = backend.rows("profiles", select="id,full_name", id="in.(" + ",".join(ids) + ")",
                            limit=CHAT_PAGE_SIZE)
    return {profile["id"]: profile.get("full_name") or "Unnamed user" for profile in profiles}
