import re
import unittest

from fakes import REPORT, TARGET, FakeSupabase
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app


class ModerationHistoryWebTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")

    def review_form(self):
        response = self.client.get("/reports/" + REPORT)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        data = {name: re.search(r'name="' + name + r'" value="([^"]+)"', html).group(1)
                for name in ("csrf", "version")}
        match = re.search(r'name="account_version" value="([^"]+)"', html)
        if match:
            data["account_version"] = match.group(1)
        return data

    def test_comment_is_added_to_report_history_and_shown_with_moderator(self):
        data = self.review_form()
        data["comment"] = "Reviewed the screenshots."
        result = self.client.post("/reports/" + REPORT + "/comments", data=data)
        self.assertEqual(303, result.status_code)
        page = self.client.get("/reports/" + REPORT).get_data(as_text=True)
        self.assertIn("Reviewed the screenshots.", page)
        self.assertIn("Admin", page)

    def test_status_change_is_recorded_with_optional_comment(self):
        data = self.review_form()
        data.update(status="in_review", comment="Beginning review.")
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(303, result.status_code)
        self.assertEqual("status_change_attempted_to_in_review", self.backend.events[0]["event_type"])
        self.assertEqual("Beginning review.", self.backend.events[0]["comment"])
        self.assertEqual("status_changed_to_in_review", self.backend.events[1]["event_type"])

    def test_comment_with_unchanged_status_is_recorded_as_a_note(self):
        data = self.review_form()
        data.update(status="open", comment="Still reviewing context.")
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(303, result.status_code)
        self.assertEqual("open", self.backend.report["status"])
        self.assertEqual("note_added", self.backend.events[0]["event_type"])
        self.assertEqual("Still reviewing context.", self.backend.events[0]["comment"])

    def test_status_change_is_blocked_if_audit_attempt_cannot_be_saved(self):
        self.backend.event_failure = PortalError("Audit storage unavailable", 502)
        data = self.review_form()
        data.update(status="in_review", comment="Starting review.")
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(502, result.status_code)
        self.assertEqual("open", self.backend.report["status"])
        self.assertFalse(self.backend.events)

    def test_failed_completion_audit_leaves_audited_attempt_and_reports_partial_result(self):
        self.backend.event_failure_on_call = 2
        data = self.review_form()
        data.update(status="in_review", comment="Starting review.")
        result = self.client.post("/reports/" + REPORT + "/status", data=data)
        self.assertEqual(502, result.status_code)
        self.assertEqual("in_review", self.backend.report["status"])
        self.assertIn("status change was applied", result.get_data(as_text=True))
        self.assertEqual("status_change_attempted_to_in_review", self.backend.events[0]["event_type"])

    def test_account_action_is_recorded_with_optional_comment(self):
        data = self.review_form()
        data.update(action="restore", confirm="yes", comment="Appeal approved.")
        result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(303, result.status_code)
        self.assertEqual("account_restore_attempted", self.backend.events[0]["event_type"])
        self.assertEqual("Appeal approved.", self.backend.events[0]["comment"])
        self.assertEqual("account_restore", self.backend.events[1]["event_type"])

    def test_account_action_failure_does_not_create_a_success_event(self):
        self.backend.ban_failure = PortalError("Account service unavailable", 502)
        data = self.review_form()
        data.update(action="restore", confirm="yes", comment="Appeal approved.")
        result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(502, result.status_code)
        self.assertEqual("account_restore_attempted", self.backend.events[0]["event_type"])

    def test_account_action_is_blocked_if_audit_attempt_cannot_be_saved(self):
        self.backend.event_failure = PortalError("Audit storage unavailable", 502)
        data = self.review_form()
        data.update(action="restore", confirm="yes")
        result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(502, result.status_code)
        self.assertFalse(self.backend.bans)
        self.assertFalse(self.backend.events)

    def test_account_action_audit_failure_is_reported_after_action_was_applied(self):
        self.backend.event_failure_on_call = 2
        data = self.review_form()
        data.update(action="restore", confirm="yes", comment="Appeal approved.")
        result = self.client.post("/reports/" + REPORT + "/account", data=data)
        self.assertEqual(502, result.status_code)
        self.assertEqual([(TARGET, "none")], self.backend.bans)
        self.assertIn("account action was applied", result.get_data(as_text=True))
        self.assertEqual("account_restore_attempted", self.backend.events[0]["event_type"])

    def test_history_read_failure_keeps_review_page_available(self):
        self.backend.history_failure = PortalError("History unavailable", 502)
        result = self.client.get("/reports/" + REPORT)
        self.assertEqual(200, result.status_code)
        self.assertIn("Moderation history could not be loaded", result.get_data(as_text=True))
        self.assertIn("Save report status", result.get_data(as_text=True))

    def test_blank_comment_is_rejected_without_creating_an_event(self):
        data = self.review_form()
        data["comment"] = "  "
        result = self.client.post("/reports/" + REPORT + "/comments", data=data)
        self.assertEqual(400, result.status_code)
        self.assertFalse(self.backend.events)


if __name__ == "__main__":
    unittest.main()
