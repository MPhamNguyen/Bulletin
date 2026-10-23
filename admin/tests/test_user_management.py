import re
import unittest
from unittest.mock import patch

from fakes import ADMIN, TARGET, REPORT, FakeSupabase
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app, version_serializer


class UserManagementTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.backend.auth_account.update(banned_until="9999-12-31T00:00:00Z",
                                         email="target@example.test")
        self.accounts = [self.backend.auth_account]
        self.backend.auth_users = lambda page, search: self.accounts
        self.original_rows = self.backend.rows
        self.backend.rows = lambda table, **query: (
            list(self.backend.profiles.values()) if table == "profiles"
            else [] if table == "reports" and self.backend.report is None
            else self.original_rows(table, **query)
        )
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.path = "/users/" + TARGET

    def form(self, path=None):
        response = self.client.get(path or self.path)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        return {name: re.search(r'name="' + name + r'" value="([^"]+)"', html).group(1)
                for name in ("csrf", "version")}

    def test_home_links_to_manage_users(self):
        self.assertIn('href="/users"', self.client.get("/").get_data(as_text=True))

    def test_account_links_to_existing_report_ticket(self):
        response = self.client.get(self.path)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        self.assertIn("Report tickets", html)
        self.assertIn('href="/reports/' + REPORT + '"', html)
        self.assertEqual(200, self.client.get("/reports/" + REPORT).status_code)
        query = self.backend.queries[0][1]
        self.assertEqual("eq." + TARGET, query["reported_messages.sender_id"])
        self.assertEqual("eq." + TARGET, query["reported_listings.user_id"])
        self.assertEqual("(reported_user_id.eq." + TARGET
                         + ",reported_messages.not.is.null,reported_listings.not.is.null)", query["or"])
        self.assertNotIn("status", query)

    def test_report_tickets_paginate_without_changing_account(self):
        self.backend.page_rows = [self.backend.report] * 26
        response = self.client.get(self.path + "?report_page=2")
        html = response.get_data(as_text=True)
        self.assertIn("Previous reports", html)
        self.assertIn("Next reports", html)
        self.assertEqual(25, html.count("View report " + REPORT))
        query = self.backend.queries[-1][1]
        self.assertEqual(25, query["offset"])
        self.assertEqual(26, query["limit"])
        self.assertFalse(self.backend.bans)

    def test_empty_or_unavailable_reports_preserve_account_actions(self):
        self.backend.report = None
        response = self.client.get(self.path)
        self.assertIn("No matching report tickets remain", response.get_data(as_text=True))
        with patch.object(self.backend, "rows", side_effect=PortalError("Unavailable", 502)):
            response = self.client.get(self.path)
        html = response.get_data(as_text=True)
        self.assertEqual(200, response.status_code)
        self.assertIn("Report tickets could not be loaded", html)
        self.assertIn("Apply account action", html)
        self.assertNotIn("No matching report tickets remain", html)

    def test_invalid_report_page_is_rejected(self):
        for value in ("0", "-1", "abc", "100001"):
            self.assertEqual(400, self.client.get(self.path + "?report_page=" + value).status_code)

    def test_only_current_suspensions_are_listed(self):
        self.accounts = [dict(self.backend.auth_account),
                         dict(id=ADMIN, banned_until=None),
                         dict(id=ADMIN, banned_until="2020-01-01T00:00:00Z")]
        response = self.client.get("/users")
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        self.assertIn('href="' + self.path + '"', html)
        self.assertNotIn('href="/users/' + ADMIN + '"', html)

    def test_empty_batch_does_not_hide_next_batch(self):
        self.accounts = [dict(id=ADMIN, banned_until=None)] * 100
        response = self.client.get("/users")
        html = response.get_data(as_text=True)
        self.assertIn("No suspended users in this account batch", html)
        self.assertIn("Next batch", html)
        self.assertNotIn("Previous batch", html)

    def test_search_and_pagination_reach_backend(self):
        with patch.object(self.backend, "auth_users", return_value=[]) as listing:
            response = self.client.get("/users?page=2&q=example.test")
        self.assertEqual(200, response.status_code)
        listing.assert_called_once_with(2, "example.test")
        self.assertIn("Previous batch", response.get_data(as_text=True))

    def test_invalid_page_or_search_is_rejected(self):
        for query in ("page=0", "page=-1", "page=abc", "page=100001", "q=" + "a" * 321):
            self.assertEqual(400, self.client.get("/users?" + query).status_code)

    def test_restore_and_change_duration_do_not_need_a_report(self):
        self.backend.report = None
        for action, duration in (("restore", "none"), ("suspend", "120m")):
            data = self.form()
            data.update(action=action, duration="2", duration_unit="hours", confirm="yes")
            result = self.client.post(self.path + "/action", data=data)
            self.assertEqual(303, result.status_code)
            self.assertEqual("/users", result.location)
            self.assertEqual((TARGET, duration), self.backend.bans[-1])
        self.assertFalse(self.backend.patches)

    def test_csrf_confirmation_and_valid_duration_are_required(self):
        data = self.form()
        data.update(action="restore", confirm="yes", csrf="forged")
        self.assertEqual(400, self.client.post(self.path + "/action", data=data).status_code)
        data = self.form()
        data.update(action="restore")
        self.assertEqual(400, self.client.post(self.path + "/action", data=data).status_code)
        data.update(action="suspend", duration="0", duration_unit="days", confirm="yes")
        self.assertEqual(400, self.client.post(self.path + "/action", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_changed_expired_and_tampered_forms_are_rejected(self):
        data = self.form()
        data.update(action="restore", confirm="yes")
        self.backend.auth_account["banned_until"] = "9998-01-01T00:00:00Z"
        self.assertEqual(409, self.client.post(self.path + "/action", data=data).status_code)
        data = self.form()
        data.update(action="restore", confirm="yes")
        self.backend.auth_account["banned_until"] = None
        self.assertEqual(409, self.client.post(self.path + "/action", data=data).status_code)
        data["version"] = "forged"
        self.assertEqual(409, self.client.post(self.path + "/action", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_report_version_cannot_be_used_for_account_action(self):
        data = self.form()
        with self.app.app_context():
            data["version"] = version_serializer().dumps({"id": TARGET})
        data.update(action="restore", confirm="yes")
        self.assertEqual(409, self.client.post(self.path + "/action", data=data).status_code)

    def test_protected_or_deleted_profile_cannot_be_modified(self):
        for field, value, status in (("is_admin", True, 403), ("is_admin", None, 403),
                                     ("deleted_at", "2026-01-01", 409)):
            data = self.form()
            data.update(action="restore", confirm="yes")
            self.backend.profiles[TARGET][field] = value
            self.assertEqual(status, self.client.post(self.path + "/action", data=data).status_code)
            self.backend.profiles[TARGET].update(is_admin=False, deleted_at=None)
        self.assertFalse(self.backend.bans)

    def test_missing_profile_or_auth_account_is_handled(self):
        data = self.form()
        data.update(action="restore", confirm="yes")
        del self.backend.profiles[TARGET]
        self.assertEqual(409, self.client.post(self.path + "/action", data=data).status_code)
        self.backend.auth_account = None
        self.assertEqual(404, self.client.get(self.path).status_code)
        self.assertFalse(self.backend.bans)

    def test_management_requires_an_active_admin(self):
        data = self.form()
        data.update(action="restore", confirm="yes")
        self.backend.profiles[ADMIN]["is_admin"] = False
        for path in ("/users", self.path):
            self.assertEqual(403, self.client.get(path).status_code)
        self.assertEqual(403, self.client.post(self.path + "/action", data=data).status_code)
        self.backend.profiles[ADMIN]["is_admin"] = True
        self.backend.admin_account["banned_until"] = "9999-12-31T00:00:00Z"
        self.assertEqual(403, self.client.get("/users").status_code)
        self.client.delete_cookie(ACCESS_COOKIE)
        self.assertEqual(303, self.client.get("/users").status_code)

    def test_backend_errors_never_show_success(self):
        data = self.form()
        data.update(action="restore", confirm="yes")
        with patch.object(self.backend, "set_ban", side_effect=PortalError("Unavailable", 502)):
            response = self.client.post(self.path + "/action", data=data)
        self.assertEqual(502, response.status_code)
        self.assertNotIn("Account access restored", response.get_data(as_text=True))

    def test_user_supplied_fields_are_escaped(self):
        self.backend.profiles[TARGET]["full_name"] = "<script>bad</script>"
        for path in ("/users", self.path):
            html = self.client.get(path).get_data(as_text=True)
            self.assertIn("&lt;script&gt;", html)
            self.assertNotIn("<script>bad", html)

    def test_unverifiable_suspension_does_not_appear_as_empty_list(self):
        self.backend.auth_account["banned_until"] = "invalid"
        self.assertEqual(502, self.client.get("/users").status_code)


if __name__ == "__main__":
    unittest.main()
