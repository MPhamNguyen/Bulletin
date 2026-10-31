"""Moderation history routes for report comments."""

from flask import Blueprint, flash, g, redirect, request, url_for

from .moderation import report_by_id
from .moderation_history import add_report_event, normalize_comment

moderation_history_routes = Blueprint("moderation_history", __name__)


@moderation_history_routes.post("/reports/<uuid:report_id>/comments")
def add_report_comment(report_id):
    report = report_by_id(g.backend, str(report_id))
    comment = normalize_comment(request.form.get("comment"))
    add_report_event(g.backend, report["id"], g.admin["id"], "note_added", comment)
    flash("Moderation comment added.")
    return redirect(url_for("portal.review", report_id=report_id), code=303)
