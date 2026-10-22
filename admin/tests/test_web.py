import re
import unittest
from datetime import datetime, timezone
from unittest.mock import patch

from fakes import ADMIN, LISTING, MESSAGE, REPORT, TARGET, FakeSupabase
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app


class PortalTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()

    def csrf(self):
        self.client.get("/login")
        with self.client.session_transaction() as session:
            return session["csrf"]

    def authenticate(self):
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")

    def review_form(self):
        self.authenticate()
        response = self.client.get("/reports/" + REPORT)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        return {name: re.search(r'name="' + name + r'" value="([^"]+)"', html).group(1)
                for name in ("csrf", "version")}

    def test_admin_login_sets_protected_cookie_and_rotates_csrf(self):
        old_csrf = self.csrf()
        result = self.client.post("/login", data=dict(csrf=old_csrf, email="admin@example.test", password="correct"))
        self.assertEqual(303, result.status_code)
        cookies = result.headers.getlist("Set-Cookie")
        access = next(value for value in cookies if value.startswith(ACCESS_COOKIE))
        for flag in ("HttpOnly", "Secure", "SameSite=Strict", "Max-Age=3600"):
            self.assertIn(flag, access)
        with self.client.session_transaction() as session:
            self.assertNotEqual(old_csrf, session["csrf"])

    def test_non_admin_values_cannot_log_in(self):
        for value in (False, None, "true", 1):
            with self.subTest(value=value):
                self.backend.profiles[ADMIN]["is_admin"] = value
                result = self.client.post("/login", data=dict(csrf=self.csrf(), email="admin@example.test", password="correct"))
                self.assertEqual(403, result.status_code)
                self.assertNotIn(ACCESS_COOKIE + "=verified-token", " ".join(result.headers.getlist("Set-Cookie")))

    def test_deleted_admin_cannot_log_in(self):
        self.backend.profiles[ADMIN]["deleted_at"] = "2026-10-01T00:00:00Z"
        result = self.client.post("/login", data=dict(csrf=self.csrf(), email="admin@example.test", password="correct"))
        self.assertEqual(403, result.status_code)

    def test_invalid_credentials_do_not_create_access_cookie(self):
        result = self.client.post("/login", data=dict(csrf=self.csrf(), email="admin@example.test", password="wrong"))
        self.assertEqual(401, result.status_code)
        self.assertNotIn("verified-token", result.get_data(as_text=True))

    def test_browser_favicon_request_preserves_the_rendered_login_form(self):
        token = self.csrf()
        favicon = self.client.get("/favicon.ico", follow_redirects=True)
        self.assertEqual(204, favicon.status_code)
        with self.client.session_transaction() as session:
            self.assertEqual(token, session["csrf"])
        result = self.client.post("/login", data=dict(
            csrf=token, email="admin@example.test", password="correct",
        ))
        self.assertEqual(303, result.status_code)
        self.assertEqual("/", result.location)

    def test_missing_page_does_not_clear_an_anonymous_login_session(self):
        token = self.csrf()
        result = self.client.get("/missing-page", follow_redirects=True)
        self.assertEqual(404, result.status_code)
        with self.client.session_transaction() as session:
            self.assertEqual(token, session["csrf"])

    def test_missing_page_does_not_sign_out_an_administrator(self):
        data = self.review_form()
        result = self.client.get("/missing-page", follow_redirects=True)
        self.assertEqual(404, result.status_code)
        self.assertEqual("verified-token", self.client.get_cookie(ACCESS_COOKIE).value)
        with self.client.session_transaction() as session:
            self.assertEqual(data["csrf"], session["csrf"])

    def test_every_protected_route_requires_authentication(self):
        for path in ("/", "/reports", "/reports/" + REPORT):
            result = self.client.get(path)
            self.assertEqual(303, result.status_code)
            self.assertEqual("/login", result.location)

    def test_home_links_to_reports_without_loading_the_report_queue(self):
        self.authenticate()
        response = self.client.get("/")
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        self.assertIn("Admin home", html)
        self.assertIn('href="/reports">', html)
        self.assertIn('<span>View Reports</span>', html)
        self.assertFalse(self.backend.queries)
        reports = self.client.get("/reports")
        self.assertEqual(200, reports.status_code)
        self.assertIn("Review " + REPORT, reports.get_data(as_text=True))

    def test_non_admin_cannot_access_home_or_reports(self):
        self.authenticate()
        self.backend.profiles[ADMIN]["is_admin"] = False
        for path in ("/", "/reports"):
            with self.subTest(path=path):
                self.assertEqual(403, self.client.get(path).status_code)

    def test_revoking_admin_role_blocks_read_and_write_immediately(self):
        data = self.review_form()
        self.backend.profiles[ADMIN]["is_admin"] = False
        self.assertEqual(403, self.client.get("/").status_code)
        data["status"] = "dismissed"
        self.assertEqual(403, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertFalse(self.backend.patches)

    def test_expired_session_is_cleared(self):
        self.client.set_cookie(ACCESS_COOKIE, "expired")
        result = self.client.get("/")
        self.assertEqual(303, result.status_code)
        self.assertIsNone(self.client.get_cookie(ACCESS_COOKIE))

    def test_missing_and_forged_csrf_block_all_post_actions(self):
        self.authenticate()
        for suffix in ("/login", "/logout", "/reports/" + REPORT + "/status", "/reports/" + REPORT + "/account"):
            for csrf in (None, "forged"):
                result = self.client.post(suffix, data={} if csrf is None else {"csrf": csrf})
                self.assertEqual(400, result.status_code)
        self.assertFalse(self.backend.patches)
        self.assertFalse(self.backend.bans)

    def test_status_and_utc_modified_time_are_written_in_one_conditional_patch(self):
        data = self.review_form()
        before = datetime.now(timezone.utc)
        data["status"] = "in_review"
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        after = datetime.now(timezone.utc)
        self.assertEqual(303, result.status_code)
        self.assertEqual(1, len(self.backend.patches))
        patch = self.backend.patches[0]
        self.assertEqual({"status", "modified_at"}, set(patch["data"]))
        self.assertEqual("in_review", patch["data"]["status"])
        stamp = datetime.fromisoformat(patch["data"]["modified_at"])
        self.assertTrue(before <= stamp <= after)
        self.assertEqual(timezone.utc, stamp.tzinfo)
        self.assertEqual("is.null", patch["query"]["modified_at"])
        self.assertEqual("eq.open", patch["query"]["status"])

    def test_existing_modified_time_is_used_in_compare_and_swap(self):
        self.backend.report["modified_at"] = "2026-10-01T12:00:00+00:00"
        data = self.review_form()
        data["status"] = "dismissed"
        self.assertEqual(303, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertEqual("eq.2026-10-01T12:00:00+00:00", self.backend.patches[0]["query"]["modified_at"])

    def test_stale_report_form_cannot_overwrite_another_reviewer(self):
        data = self.review_form()
        self.backend.report["status"] = "dismissed"
        data["status"] = "resolved"
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertFalse(self.backend.patches)

    def test_race_during_patch_returns_conflict(self):
        data = self.review_form()
        self.backend.race = True
        data["status"] = "dismissed"
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertEqual("open", self.backend.report["status"])

    def test_same_status_does_not_change_modified_at(self):
        data = self.review_form()
        data["status"] = "open"
        self.assertEqual(303, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertFalse(self.backend.patches)

    def test_action_taken_form_sends_resolved_and_updates_modified_at(self):
        data = self.review_form()
        html = self.client.get("/reports/" + REPORT).get_data(as_text=True)
        self.assertRegex(html, r'<option value="resolved"[^>]*>Action taken</option>')
        self.assertNotRegex(html, r'<option value="(?:action_taken|under_review|duplicate)"')
        data["status"] = "resolved"
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(303, result.status_code)
        self.assertEqual("resolved", self.backend.report["status"])
        self.assertIsNotNone(self.backend.report["modified_at"])

    def test_unsupported_database_statuses_are_rejected_before_a_write(self):
        data = self.review_form()
        for status in ("action_taken", "under_review", "duplicate"):
            with self.subTest(status=status):
                data["status"] = status
                result = self.client.post("/reports/" + REPORT + "/status", data=data)
                self.assertEqual(400, result.status_code)
        self.assertFalse(self.backend.patches)

    def test_invalid_status_and_tampered_version_are_rejected(self):
        data = self.review_form()
        data["status"] = "bad-status"
        self.assertEqual(400, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        data.update(status="dismissed", version="forged")
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/status", data=data).status_code)
        self.assertFalse(self.backend.patches)

    def test_account_target_is_derived_from_report_for_each_type(self):
        for kind in ("user", "message", "listing"):
            with self.subTest(kind=kind):
                self.backend.report.update(type=kind, message_id=MESSAGE, listing_id=LISTING)
                data = self.review_form()
                data.update(action="suspend", duration="7", duration_unit="days", confirm="yes", target_id=ADMIN)
                result = self.client.post("/reports/" + REPORT + "/account", data=data)
                self.assertEqual(303, result.status_code)
                self.assertEqual((TARGET, "10080m"), self.backend.bans[-1])
                self.assertEqual("open", self.backend.report["status"])
        self.assertFalse(self.backend.patches)

    def test_restoring_access_uses_supabase_unban_operation(self):
        data = self.review_form()
        data.update(action="restore", confirm="yes")
        self.assertEqual(303, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertEqual([(TARGET, "none")], self.backend.bans)

    def test_custom_suspension_units_are_converted_to_minutes(self):
        for amount, unit, expected in (("2", "days", "2880m"),
                                       ("3", "hours", "180m"),
                                       ("15", "minutes", "15m"),
                                       ("525600", "minutes", "525600m")):
            with self.subTest(amount=amount, unit=unit):
                data = self.review_form()
                data.update(action="suspend", duration=amount, duration_unit=unit, confirm="yes")
                result = self.client.post("/reports/" + REPORT + "/account", data=data)
                self.assertEqual(303, result.status_code)
                self.assertEqual((TARGET, expected), self.backend.bans[-1])

    def test_invalid_suspension_duration_never_changes_account(self):
        invalid = (("", "days"), ("0", "hours"), ("-1", "minutes"),
                   ("1.5", "days"), ("abc", "days"), ("1h", "hours"),
                   ("1", "weeks"), ("366", "days"), ("8761", "hours"),
                   ("525601", "minutes"), ("9" * 100, "minutes"),
                   ("NaN", "minutes"), ("\u0661", "days"), (None, None))
        for amount, unit in invalid:
            with self.subTest(amount=amount, unit=unit):
                data = self.review_form()
                data.update(action="suspend", confirm="yes")
                if amount is not None:
                    data.update(duration=amount, duration_unit=unit)
                result = self.client.post("/reports/" + REPORT + "/account", data=data)
                self.assertEqual(400, result.status_code)
        self.assertFalse(self.backend.bans)
        self.assertFalse(self.backend.patches)

    def test_restore_ignores_unused_duration_fields(self):
        data = self.review_form()
        data.update(action="restore", duration="invalid", duration_unit="invalid", confirm="yes")
        result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(303, result.status_code)
        self.assertEqual([(TARGET, "none")], self.backend.bans)

    def test_review_offers_custom_duration_and_loads_script_under_csp(self):
        self.authenticate()
        response = self.client.get("/reports/" + REPORT)
        html = response.get_data(as_text=True)
        self.assertIn('<option value="suspend">Suspend account</option>', html)
        self.assertNotIn('value="suspend_day"', html)
        self.assertIn('id="suspension-duration" hidden disabled', html)
        self.assertIn('name="duration"', html)
        self.assertIn('name="duration_unit"', html)
        self.assertIn('src="/assets/account-action.js" defer', html)
        self.assertIn("script-src 'self';", response.headers["Content-Security-Policy"])
        self.assertNotIn("'unsafe-inline'", response.headers["Content-Security-Policy"])
        script = self.client.get("/assets/account-action.js")
        self.assertEqual(200, script.status_code)
        script.close()

    def test_admin_target_is_protected_even_if_form_was_already_open(self):
        data = self.review_form()
        self.backend.profiles[TARGET]["is_admin"] = True
        data.update(action="suspend", duration="1", duration_unit="days", confirm="yes")
        self.assertEqual(403, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_confirmation_and_known_action_are_required(self):
        data = self.review_form()
        data["action"] = "suspend"
        self.assertEqual(400, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        data.update(confirm="yes", action="delete")
        self.assertEqual(400, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_missing_target_does_not_offer_or_perform_account_actions(self):
        del self.backend.profiles[TARGET]
        data = self.review_form()
        data.update(action="suspend", duration="1", duration_unit="days", confirm="yes")
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_deleted_auth_account_keeps_report_reviewable(self):
        self.backend.auth_account = None
        data = self.review_form()
        data.update(action="suspend", duration="1", duration_unit="days", confirm="yes")
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_reported_account_last_sign_in_is_displayed_in_utc_for_each_report_type(self):
        self.backend.auth_account["last_sign_in_at"] = "2026-10-21T14:35:09+02:00"
        self.backend.admin_account["last_sign_in_at"] = "2026-01-01T00:00:00Z"
        self.authenticate()
        for kind in ("user", "message", "listing"):
            with self.subTest(kind=kind):
                self.backend.report.update(type=kind, message_id=MESSAGE, listing_id=LISTING)
                response = self.client.get("/reports/" + REPORT)
                self.assertEqual(200, response.status_code)
                html = response.get_data(as_text=True)
                self.assertIn("Last signed in: 2026-10-21 12:35:09 UTC", html)
                self.assertNotIn("2026-01-01", html)

    def test_absent_or_invalid_last_sign_in_does_not_break_review(self):
        self.authenticate()
        for value, label in ((None, "Not recorded"), ("", "Not recorded"),
                             ("invalid", "Unavailable"), (123, "Unavailable"),
                             ("2026-10-21T12:00:00", "Unavailable")):
            with self.subTest(value=value):
                self.backend.auth_account["last_sign_in_at"] = value
                response = self.client.get("/reports/" + REPORT)
                self.assertEqual(200, response.status_code)
                self.assertIn("Last signed in: " + label, response.get_data(as_text=True))
        self.backend.auth_account = None
        response = self.client.get("/reports/" + REPORT)
        self.assertEqual(200, response.status_code)
        self.assertIn("Last signed in: Unavailable", response.get_data(as_text=True))

    def test_deleted_profile_cannot_be_restored_by_auth_action(self):
        data = self.review_form()
        self.backend.profiles[TARGET]["deleted_at"] = "2026-10-21T00:00:00Z"
        data.update(action="restore", confirm="yes")
        self.assertEqual(409, self.client.post("/reports/" + REPORT + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_account_backend_failure_does_not_close_report_or_show_success(self):
        data = self.review_form()
        data.update(action="suspend", duration="1", duration_unit="days", confirm="yes")
        with patch.object(self.backend, "set_ban", side_effect=PortalError("Action could not be confirmed.", 502)):
            result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(502, result.status_code)
        self.assertNotIn("Account action completed", result.get_data(as_text=True))
        self.assertEqual("open", self.backend.report["status"])
        self.assertFalse(self.backend.patches)

    def test_failed_report_update_does_not_show_success(self):
        data = self.review_form()
        data["status"] = "resolved"
        with patch.object(self.backend, "request", side_effect=PortalError("Update unavailable", 502)):
            result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(502, result.status_code)
        self.assertEqual("open", self.backend.report["status"])
        self.assertNotIn("Report status saved", result.get_data(as_text=True))

    def test_modified_time_must_be_confirmed_in_write_response(self):
        data = self.review_form()
        data["status"] = "dismissed"
        with patch.object(self.backend, "request", return_value=[dict(id=REPORT, status="dismissed")]):
            result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(502, result.status_code)
        self.assertNotIn("Report status saved", result.get_data(as_text=True))

    def test_local_stylesheet_is_available_without_backend_configuration(self):
        self.backend.available = False
        response = self.client.get("/assets/style.css")
        self.assertEqual(200, response.status_code)
        self.assertEqual("text/css", response.mimetype)
        response.close()

    def test_deleted_report_returns_404(self):
        self.authenticate()
        self.backend.report = None
        self.assertEqual(404, self.client.get("/reports/" + REPORT).status_code)

    def test_report_content_is_escaped_and_secrets_are_absent(self):
        self.backend.report["description"] = '<script>alert("x")</script>'
        self.review_form()
        html = self.client.get("/reports/" + REPORT).get_data(as_text=True)
        self.assertIn("&lt;script&gt;", html)
        self.assertNotIn("<script>", html)
        self.assertNotIn("verified-token", html)
        self.assertNotIn(self.app.secret_key, html)

    def test_filters_and_pagination_reach_backend(self):
        self.authenticate()
        self.backend.page_rows = [self.backend.report] * 26
        result = self.client.get("/reports?status=in_review&type=message&page=2")
        self.assertEqual(200, result.status_code)
        query = self.backend.queries[-1][1]
        self.assertEqual("eq.in_review", query["status"])
        self.assertEqual("eq.message", query["type"])
        self.assertEqual(50, query["offset"])
        self.assertIn("Next", result.get_data(as_text=True))

    def test_invalid_filters_and_pages_fail_without_backend_query(self):
        self.authenticate()
        for query in ("status=bogus", "type=bogus", "page=-1", "page=abc"):
            self.assertEqual(400, self.client.get("/reports?" + query).status_code)
        self.assertFalse(self.backend.queries)

    def test_security_headers_and_service_failure(self):
        self.authenticate()
        self.backend.failure = PortalError("Backend unavailable", 502)
        result = self.client.get("/")
        self.assertEqual(502, result.status_code)
        self.assertIn("no-store", result.headers["Cache-Control"])
        self.assertIn("frame-ancestors 'none'", result.headers["Content-Security-Policy"])
        self.assertIn("Strict-Transport-Security", result.headers)

    def test_missing_configuration_fails_closed(self):
        self.backend.available = False
        self.assertEqual(503, self.client.get("/login").status_code)

    def test_logout_revokes_session_and_removes_cookie(self):
        self.authenticate()
        result = self.client.post("/logout", data={"csrf": self.csrf()})
        self.assertEqual(303, result.status_code)
        self.assertEqual(["verified-token"], self.backend.logged_out)
        self.assertIsNone(self.client.get_cookie(ACCESS_COOKIE))


if __name__ == "__main__":
    unittest.main()
