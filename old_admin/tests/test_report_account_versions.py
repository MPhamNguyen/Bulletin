import re
import unittest

from fakes import ADMIN, LISTING, MESSAGE, REPORT, REPORTER, TARGET, FakeSupabase
from portal.web import ACCESS_COOKIE, create_app


class ReportAccountVersionTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.path = "/reports/" + REPORT

    def form(self):
        html = self.client.get(self.path).get_data(as_text=True)
        return {name: re.search(r'name="' + name + r'" value="([^"]+)"', html).group(1)
                for name in ("csrf", "version", "account_version")}

    def test_stale_restore_or_suspend_cannot_replace_newer_decision(self):
        for kind in ("user", "message", "listing"):
            for action in ("restore", "suspend"):
                with self.subTest(kind=kind, action=action):
                    self.backend.report.update(type=kind, message_id=MESSAGE, listing_id=LISTING)
                    self.backend.auth_account["banned_until"] = "2098-01-01T00:00:00Z"
                    data = self.form()
                    self.backend.auth_account["banned_until"] = "2099-01-01T00:00:00Z"
                    data.update(action=action, confirm="yes", duration="1", duration_unit="minutes")
                    response = self.client.post(self.path + "/account", data=data)
                    self.assertEqual(409, response.status_code)
                    self.assertFalse(self.backend.bans)
                    self.assertFalse(self.backend.patches)

    def test_changed_target_cannot_receive_action_from_old_form(self):
        self.backend.report.update(type="listing", listing_id=LISTING)
        data = self.form()
        self.backend.listing["user_id"] = REPORTER
        self.backend.auth_account = dict(id=REPORTER, banned_until=None)
        data.update(action="restore", confirm="yes")
        self.assertEqual(409, self.client.post(self.path + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_account_update_invalidates_form_even_when_ban_expiry_matches(self):
        data = self.form()
        self.backend.auth_account["updated_at"] = "2026-10-22T12:00:00Z"
        data.update(action="restore", confirm="yes")
        self.assertEqual(409, self.client.post(self.path + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_missing_tampered_or_report_token_cannot_authorize_account_action(self):
        for token in (None, "forged", "use-report-token"):
            data = self.form()
            if token is None:
                del data["account_version"]
            else:
                data["account_version"] = data["version"] if token == "use-report-token" else token
            data.update(action="restore", confirm="yes")
            self.assertEqual(409, self.client.post(self.path + "/account", data=data).status_code)
        self.assertFalse(self.backend.bans)

    def test_new_form_can_restore_after_stale_submission_is_rejected(self):
        data = self.form()
        self.backend.auth_account["banned_until"] = "2099-01-01T00:00:00Z"
        data.update(action="restore", confirm="yes")
        self.assertEqual(409, self.client.post(self.path + "/account", data=data).status_code)
        data = self.form()
        data.update(action="restore", confirm="yes")
        self.assertEqual(303, self.client.post(self.path + "/account", data=data).status_code)
        self.assertEqual([(TARGET, "none")], self.backend.bans)


if __name__ == "__main__":
    unittest.main()
