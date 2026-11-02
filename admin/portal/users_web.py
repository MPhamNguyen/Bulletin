"""Administrator routes for managing suspended users."""

from flask import Blueprint, current_app, flash, g, redirect, render_template, request, url_for
from itsdangerous import BadSignature

from .backend import PortalError
from .user_management import (
    account_reports_page, account_snapshot, is_suspended, managed_account, suspended_users_page,
    update_suspended_account,
)
from .web import version_serializer

user_routes = Blueprint("users", __name__)


@user_routes.get("/users")
def index():
    search = request.args.get("q", "").strip()
    try:
        page = int(request.args.get("page", "1"))
        if not 1 <= page <= 100000 or len(search) > 320:
            raise ValueError
    except ValueError:
        raise PortalError("Invalid account search or page number.") from None
    users, has_next = suspended_users_page(g.backend, page, search)
    return render_template("users.html", users=users, page=page, search=search, has_next=has_next)


@user_routes.get("/users/<uuid:user_id>")
def detail(user_id):
    try:
        page = int(request.args.get("report_page", "1"))
        if not 1 <= page <= 100000:
            raise ValueError
    except ValueError:
        raise PortalError("Invalid report page number.") from None
    account, profile = managed_account(g.backend, str(user_id))
    reports, has_next, report_error = [], False, None
    try:
        reports, has_next = account_reports_page(g.backend, str(user_id), page)
    except PortalError:
        report_error = "Report tickets could not be loaded. Reload the page to try again."
    return render_template(
        "user.html", account=account, target=profile, suspended=is_suspended(account),
        version=version_serializer("managed-account").dumps(account_snapshot(account)),
        reports=reports, report_page=page, has_next=has_next, report_error=report_error,
    )


@user_routes.post("/users/<uuid:user_id>/action")
def action(user_id):
    try:
        version = version_serializer("managed-account").loads(
            request.form.get("version", ""), max_age=3600,
        )
    except BadSignature:
        raise PortalError("This account form expired. Reload before acting.", 409) from None
    if not isinstance(version, dict) or version.get("id") != str(user_id):
        raise PortalError("The form does not match this account.")
    if request.form.get("confirm") != "yes":
        raise PortalError("Confirm the account action before applying it.")
    selected = request.form.get("action")
    update_suspended_account(
        g.backend, str(user_id), g.admin["id"], version, selected,
        request.form.get("duration"), request.form.get("duration_unit"),
    )
    current_app.logger.info("managed_account_action actor=%s target=%s action=%s",
                            g.admin["id"], user_id, selected)
    flash("Account access restored." if selected == "restore" else "Suspension duration updated.")
    return redirect(url_for("users.index"), code=303)
