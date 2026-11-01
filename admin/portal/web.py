"""Flask routes, cookie authentication, and CSRF protection."""

import logging
import os
import secrets
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import urlsplit

from flask import (
    Flask, Blueprint, current_app, flash, g, redirect, render_template,
    request, send_from_directory, session, url_for,
)
from itsdangerous import BadSignature, URLSafeTimedSerializer
from werkzeug.exceptions import HTTPException

from .backend import PortalError, Supabase
from .account_access import account_snapshot
from .listing_review import delete_reported_listing, listing_images, listing_snapshot
from .moderation import (
    ACCOUNT_ACTIONS, ALIASES, PROFILE_COLUMNS, STATUSES, act_on_account,
    admin_profile, identifier, report_by_id, reports_page, snapshot, update_status,
    target_context,
)
from .moderation_history import (
    add_report_event, normalize_comment, record_action_event, report_events,
)

routes = Blueprint("portal", __name__)
ACCESS_COOKIE = "bulletin_admin_access"


def create_app(config=None, backend=None):
    app = Flask(__name__, template_folder="templates", static_folder=None)
    app.config.from_mapping(
        SECRET_KEY=os.environ.get("ADMIN_SECRET_KEY", ""),
        SESSION_COOKIE_NAME="bulletin_admin_csrf",
        SESSION_COOKIE_HTTPONLY=True,
        SESSION_COOKIE_SAMESITE="Strict",
        SESSION_COOKIE_SECURE=(bool(os.environ.get("VERCEL")) or
                               os.environ.get("ADMIN_LOCAL_HTTP") != "true"),
        PERMANENT_SESSION_LIFETIME=timedelta(hours=1),
        MAX_CONTENT_LENGTH=16384,
        MAX_FORM_MEMORY_SIZE=16384,
    )
    if config:
        app.config.update(config)
    app.logger.setLevel(logging.INFO)
    app.extensions["supabase"] = backend or Supabase(
        os.environ.get("SUPABASE_URL", ""), os.environ.get("SUPABASE_ANON_KEY", ""),
        os.environ.get("SUPABASE_SERVICE_ROLE_KEY", ""),
    )
    app.before_request(authorize_request)
    app.after_request(security_headers)
    app.context_processor(template_context)
    app.add_template_filter(utc_timestamp, "utc_timestamp")
    app.register_error_handler(PortalError, portal_error)
    app.register_error_handler(HTTPException, http_error)
    register_routes(app)
    return app


def register_routes(app):
    from .conversation_web import conversation_routes
    from .directory_web import directory_routes
    from .moderation_history_web import moderation_history_routes
    from .users_web import user_routes

    for blueprint in (routes, user_routes, directory_routes, conversation_routes,
                      moderation_history_routes):
        app.register_blueprint(blueprint)


def utc_timestamp(value):
    if not value:
        return "Not recorded"
    try:
        timestamp = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if timestamp.tzinfo is None:
            return "Unavailable"
        return timestamp.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    except (AttributeError, TypeError, ValueError, OverflowError):
        return "Unavailable"


def csrf_token():
    if "csrf" not in session:
        session["csrf"] = secrets.token_urlsafe(32)
        session.permanent = True
    return session["csrf"]


def template_context():
    return dict(csrf_token=csrf_token, statuses=STATUSES, aliases=ALIASES,
                account_actions=ACCOUNT_ACTIONS)


def authorize_request():
    if request.endpoint is None or request.endpoint in ("portal.asset", "portal.favicon"):
        return None
    g.backend = current_app.extensions["supabase"]
    if len(current_app.secret_key or "") < 32 or not g.backend.configured():
        raise PortalError("The admin portal is not configured. Contact the site operator.", 503)
    if request.method == "POST":
        supplied = request.form.get("csrf", "")
        expected = session.get("csrf", "")
        if not expected or not secrets.compare_digest(supplied, expected):
            raise PortalError("This form expired. Reload the page and try again.", 400)
    if request.endpoint in ("portal.login", "portal.logout"):
        return None
    token = request.cookies.get(ACCESS_COOKIE)
    if not token:
        raise PortalError("Please sign in.", 401)
    user = g.backend.current_user(token)
    g.admin = admin_profile(g.backend, user)
    return None


def security_headers(response):
    storage = urlsplit(current_app.extensions["supabase"].url)
    image_origin = " " + storage.scheme + "://" + storage.netloc if storage.scheme == "https" else ""
    response.headers.update({
        "Cache-Control": "no-store, private",
        "Pragma": "no-cache",
        "Content-Security-Policy": (
            "default-src 'none'; script-src 'self'; style-src 'self'; "
            "img-src 'self' https://static.vecteezy.com" + image_origin + "; "
            "form-action 'self'; base-uri 'none'; frame-ancestors 'none'"
        ),
        "X-Content-Type-Options": "nosniff",
        "X-Frame-Options": "DENY",
        "Referrer-Policy": "no-referrer",
        "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
    })
    if current_app.config["SESSION_COOKIE_SECURE"]:
        response.headers["Strict-Transport-Security"] = "max-age=31536000"
    return response


def portal_error(error):
    if error.status == 401 and request.endpoint != "portal.login":
        session.clear()
        response = redirect(url_for("portal.login"), code=303)
        response.delete_cookie(ACCESS_COOKIE)
        return response
    return render_template("error.html", message=str(error), status=error.status), error.status


def http_error(error):
    return render_template("error.html", message=error.name, status=error.code), error.code


def version_serializer(salt="report-version"):
    return URLSafeTimedSerializer(current_app.secret_key, salt=salt)


def submitted_version(report_id, salt="report-version", field="version"):
    try:
        version = version_serializer(salt).loads(request.form.get(field, ""), max_age=3600)
    except BadSignature:
        raise PortalError("This report form expired. Reload before saving.", 409) from None
    if not isinstance(version, dict) or version.get("id") != report_id:
        raise PortalError("The form does not match this report.", 400)
    return version


@routes.get("/assets/<path:filename>")
def asset(filename):
    # Vercel serves public/assets directly; this route supports local Flask runs.
    return send_from_directory(Path(__file__).resolve().parents[1] / "public" / "assets", filename)


@routes.get("/favicon.ico")
def favicon():
    return "", 204


@routes.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "GET":
        return render_template("login.html")
    email = request.form.get("email", "").strip()
    password = request.form.get("password", "")
    if not email or not password or len(email) > 320 or len(password) > 4096:
        return render_template("login.html", error="Enter your email and password."), 400
    try:
        result = g.backend.sign_in(email, password)
        token = result["access_token"]
        admin_profile(g.backend, g.backend.current_user(token))
    except PortalError as error:
        return render_template("login.html", error=str(error)), error.status
    session.clear()
    csrf_token()
    response = redirect(url_for("portal.home"), code=303)
    response.set_cookie(
        ACCESS_COOKIE, token, max_age=min(int(result.get("expires_in", 3600)), 3600),
        secure=current_app.config["SESSION_COOKIE_SECURE"], httponly=True,
        samesite="Strict", path="/",
    )
    return response


@routes.post("/logout")
def logout():
    token = request.cookies.get(ACCESS_COOKIE)
    if token:
        try:
            g.backend.sign_out(token)
        except PortalError:
            pass  # Local cookies must still be removed if Supabase is unavailable.
    session.clear()
    response = redirect(url_for("portal.login"), code=303)
    response.delete_cookie(ACCESS_COOKIE)
    return response


@routes.get("/")
def home():
    return render_template("home.html")


@routes.get("/reports")
def reports():
    status = request.args.get("status", "active")
    report_type = request.args.get("type", "all")
    try:
        page = int(request.args.get("page", "0"))
        if not 0 <= page <= 100000:
            raise ValueError
    except ValueError:
        raise PortalError("Invalid page number.") from None
    rows, has_next = reports_page(g.backend, status, report_type, page)
    return render_template("reports.html", reports=rows, status=status,
                           report_type=report_type, page=page, has_next=has_next)


@routes.get("/reports/<uuid:report_id>")
def review(report_id):
    report = report_by_id(g.backend, str(report_id))
    context = target_context(g.backend, report)
    reporter = None
    if report.get("reporter_id"):
        reporter = g.backend.one("profiles", identifier(report["reporter_id"]), PROFILE_COLUMNS)
    images, image_error, delete_version = listing_review_details(report, context["content"])
    history_error = None
    try:
        events = report_events(g.backend, report["id"])
    except PortalError:
        events = []
        history_error = "Moderation history could not be loaded. Reload to try again."
    account_version = version_serializer("report-account").dumps({
        "id": report["id"], "account": account_snapshot(context["account"]) if context["account"] else None,
    })
    return render_template(
        "review.html", report=report, reporter=reporter,
        version=version_serializer().dumps(snapshot(report)), images=images,
        account_version=account_version,
        image_error=image_error, delete_version=delete_version,
        moderation_events=events, history_error=history_error, **context,
    )


def listing_review_details(report, content):
    if report.get("type") != "listing" or not content:
        return [], None, None
    delete_version = version_serializer("listing-deletion").dumps({
        "id": report["id"], "report": snapshot(report), "listing": listing_snapshot(content),
    })
    try:
        return listing_images(g.backend, content), None, delete_version
    except PortalError:
        return [], "Listing photos could not be loaded. Reload the page to try again.", delete_version


@routes.post("/reports/<uuid:report_id>/listing/delete")
def delete_listing(report_id):
    report_id = str(report_id)
    version = submitted_version(report_id, salt="listing-deletion")
    if request.form.get("confirm") != "delete":
        raise PortalError("Confirm permanent deletion before deleting the listing.")
    report = report_by_id(g.backend, report_id)
    listing_id, files_removed = delete_reported_listing(g.backend, report, version)
    current_app.logger.info("listing_deleted actor=%s report=%s listing=%s files_removed=%s",
                            g.admin["id"], report_id, listing_id, files_removed)
    if files_removed:
        flash("Listing and its associated reports, photos, bookmarks, and reviews deleted.")
    else:
        flash("Listing and its associated reports were deleted, but stored photo files could not be removed. Contact the storage administrator to finish cleanup.")
    return redirect(url_for("portal.reports"), code=303)


@routes.post("/reports/<uuid:report_id>/status")
def save_status(report_id):
    report_id = str(report_id)
    version = submitted_version(report_id)
    report = report_by_id(g.backend, report_id)
    comment = normalize_comment(request.form.get("comment", ""), allow_empty=True)
    status = request.form.get("status")
    if status not in STATUSES:
        raise PortalError("Choose a valid report status.")
    if version != snapshot(report):
        raise PortalError("Another reviewer changed this report. Reload before saving.", 409)
    changed = status != report.get("status")
    if changed:
        add_report_event(
            g.backend, report_id, g.admin["id"],
            "status_change_attempted_to_" + status, comment or None,
        )
    elif comment:
        add_report_event(g.backend, report_id, g.admin["id"], "note_added", comment)
    changed = update_status(g.backend, report, status, version)
    if changed:
        record_action_event(
            g.backend, report_id, g.admin["id"],
            "status_changed_to_" + status,
            "The report status change",
        )
    if changed:
        current_app.logger.info("report_status actor=%s report=%s status=%s",
                                g.admin["id"], report_id, status)
    if changed:
        flash("Report status saved.")
    elif comment:
        flash("Comment added. The report status was unchanged.")
    else:
        flash("The report already has this status.")
    return redirect(url_for("portal.review", report_id=report_id), code=303)


@routes.post("/reports/<uuid:report_id>/account")
def account_action(report_id):
    report_id = str(report_id)
    version = submitted_version(report_id)
    account_version = submitted_version(report_id, salt="report-account", field="account_version")
    report = report_by_id(g.backend, report_id)
    if version != snapshot(report):
        raise PortalError("Another reviewer changed this report. Reload before acting.", 409)
    if request.form.get("confirm") != "yes":
        raise PortalError("Confirm the account action before applying it.")
    comment = normalize_comment(request.form.get("comment", ""), allow_empty=True)
    action = request.form.get("action")
    if action not in ACCOUNT_ACTIONS:
        raise PortalError("Choose a valid account action.")
    add_report_event(
        g.backend, report_id, g.admin["id"], "account_" + action + "_attempted",
        comment,
    )
    target_id = act_on_account(
        g.backend, report, g.admin["id"], action,
        request.form.get("duration"), request.form.get("duration_unit"),
        account_version=account_version.get("account"),
    )
    record_action_event(
        g.backend, report_id, g.admin["id"], "account_" + action,
        "The account action",
    )
    current_app.logger.info("account_action actor=%s report=%s target=%s action=%s",
                            g.admin["id"], report_id, target_id, action)
    flash("Account action completed. Update the report status when your review is finished.")
    return redirect(url_for("portal.review", report_id=report_id), code=303)
