"""Admin-only conversation evidence attached to a message report."""

from flask import Blueprint, g, render_template, request

from .backend import PortalError
from .conversation_review import (
    conversation_messages_page, conversation_senders, reported_conversation,
)

conversation_routes = Blueprint("conversation", __name__)


@conversation_routes.get("/reports/<uuid:report_id>/conversation")
def review(report_id):
    try:
        page = int(request.args.get("page", "1"))
        if not 1 <= page <= 100000:
            raise ValueError
    except ValueError:
        raise PortalError("Invalid conversation page number.") from None
    report, reported_message = reported_conversation(g.backend, str(report_id))
    messages, has_next = conversation_messages_page(g.backend, reported_message, page)
    try:
        senders = conversation_senders(g.backend, messages)
    except PortalError:
        senders = {}  # Message sender IDs still identify authors if names cannot load.
    return render_template("conversation.html", report=report, reported_message=reported_message,
                           messages=messages, senders=senders, page=page, has_next=has_next)
