import re
import unittest
from datetime import datetime, timezone
from unittest.mock import patch

from fakes import REPORT, TARGET, FakeSupabase
from portal.web import ACCESS_COOKIE, create_app


class AccountSuspensionTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.report_path = "/reports/" + REPORT
        self.user_path = "/users/" + TARGET

    def form(self, path):
        response = self.client.get(path)
        self.assertEqual(200, response.status_code)
        return dict(re.findall(r'name="(csrf|version|account_version)" value="([^"]+)"',
                               response.get_data(as_text=True)))

    def submit(self, path, action, data):
        data.update(action=action, confirm="yes", duration="2", duration_unit="hours")
        suffix = "/account" if path == self.report_path else "/action"
        return self.client.post(path + suffix, data=data)

    def test_report_cannot_overwrite_an_active_suspension_even_with_fresh_form(self):
        self.backend.auth_account["banned_until"] = "2099-01-01T00:00:00Z"
        data = self.form(self.report_path)
        response = self.submit(self.report_path, "suspend", data)
        self.assertEqual(409, response.status_code)
        self.assertFalse(self.backend.bans)
        self.assertFalse(any(e["event_type"] == "account_suspend" for e in self.backend.events))

    def test_restore_requires_active_suspension_in_both_paths(self):
        for expiry in (None, "2020-01-01T00:00:00Z"):
            for path in (self.report_path, self.user_path):
                with self.subTest(expiry=expiry, path=path):
                    self.backend.auth_account["banned_until"] = expiry
                    self.assertEqual(409, self.submit(path, "restore", self.form(path)).status_code)
        self.assertFalse(self.backend.bans)

    def test_report_can_suspend_an_expired_account(self):
        self.backend.auth_account["banned_until"] = "2020-01-01T00:00:00Z"
        html = self.client.get(self.report_path).get_data(as_text=True)
        self.assertIn("Suspended until: Not suspended", html)
        self.assertIn('<option value="suspend">', html)
        self.assertNotIn('<option value="restore">', html)
        self.assertEqual(303, self.submit(self.report_path, "suspend", self.form(self.report_path)).status_code)

    def test_active_report_links_to_duration_management(self):
        self.backend.auth_account["banned_until"] = "2099-01-01T00:00:00Z"
        html = self.client.get(self.report_path).get_data(as_text=True)
        self.assertIn('href="' + self.user_path + '"', html)
        self.assertNotIn('<option value="suspend">', html)
        self.assertIn('<option value="restore">', html)

    def test_expiry_without_a_write_invalidates_both_forms(self):
        for path, action in ((self.report_path, "restore"), (self.user_path, "restore"),
                             (self.user_path, "suspend")):
            with self.subTest(path=path, action=action):
                self.backend.auth_account["banned_until"] = "2030-01-01T00:00:00Z"
                with patch("portal.account_suspension.datetime", wraps=datetime) as clock:
                    clock.now.return_value = datetime(2029, 12, 31, tzinfo=timezone.utc)
                    data = self.form(path)
                    clock.now.return_value = datetime(2030, 1, 1, tzinfo=timezone.utc)
                    self.assertEqual(409, self.submit(path, action, data).status_code)
        self.assertFalse(self.backend.bans)

    def test_concurrent_write_after_application_check_rejects_both_paths(self):
        for path, action in ((self.report_path, "suspend"), (self.user_path, "suspend"),
                             (self.report_path, "restore"), (self.user_path, "restore")):
            with self.subTest(path=path, action=action):
                self.backend.auth_account["banned_until"] = (
                    None if path == self.report_path and action == "suspend" else "2098-01-01T00:00:00Z")
                data = self.form(path)
                write = self.backend.set_ban

                def competing_write(*args, **kwargs):
                    self.backend.auth_account["banned_until"] = "2099-01-01T00:00:00Z"
                    return write(*args, **kwargs)

                with patch.object(self.backend, "set_ban", side_effect=competing_write):
                    self.assertEqual(409, self.submit(path, action, data).status_code)
        self.assertFalse(self.backend.bans)


if __name__ == "__main__":
    unittest.main()
