from copy import deepcopy
import unittest
from unittest.mock import patch

from fakes import ADMIN, TARGET, REPORTER, REPORT, MESSAGE, LISTING, FakeSupabase
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app


class ConversationReviewTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.backend.report.update(type="message", message_id=MESSAGE, reported_user_id=None)
        self.messages = [dict(self.backend.message)]
        self.original_rows = self.backend.rows
        self.backend.rows = self.rows
        self.queries = []
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.path = "/reports/" + REPORT + "/conversation"

    def rows(self, table, **query):
        self.queries.append((table, query))
        if table == "messages":
            return deepcopy(self.messages)
        if table == "profiles":
            return [deepcopy(profile) for profile in self.backend.profiles.values()]
        return self.original_rows(table, **query)

    def test_message_report_links_to_full_conversation(self):
        response = self.client.get("/reports/" + REPORT)
        self.assertIn('href="' + self.path + '"', response.get_data(as_text=True))

    def test_chat_is_derived_from_report_not_query_parameters(self):
        response = self.client.get(self.path + "?conversation_id=" + ADMIN)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        self.assertIn("Conversation history", html)
        self.assertIn("Message content", html)
        self.assertIn("Target", html)
        self.assertIn("2026-10-01 00:00:00 UTC", html)
        self.assertIn('class="chat-message reported-message"', html)
        table, query = self.queries[0]
        self.assertEqual("messages", table)
        self.assertEqual("eq." + LISTING, query["conversation_id"])
        self.assertEqual("created_at.asc,id.asc", query["order"])
        self.assertEqual(51, query["limit"])
        self.assertNotIn("history_visible_after", query)
        self.assertNotIn("sender_id", query)

    def test_both_senders_are_displayed(self):
        self.messages.append(dict(self.backend.message, id=REPORTER, sender_id=REPORTER, body="Reply"))
        html = self.client.get(self.path).get_data(as_text=True)
        self.assertIn("Reporter", html)
        self.assertIn("Reply", html)

    def test_pagination_exposes_more_than_one_page(self):
        self.messages *= 51
        html = self.client.get(self.path + "?page=2").get_data(as_text=True)
        self.assertEqual(50, html.count('class="chat-message reported-message"'))
        self.assertIn("Previous messages", html)
        self.assertIn("Next messages", html)
        self.assertEqual(50, self.queries[0][1]["offset"])

    def test_other_report_types_cannot_open_chat(self):
        for kind in ("user", "listing"):
            self.backend.report["type"] = kind
            self.assertEqual(404, self.client.get(self.path).status_code)
        self.assertFalse(self.queries)

    def test_missing_report_or_reported_message_is_handled(self):
        self.backend.message = None
        self.assertEqual(404, self.client.get(self.path).status_code)
        self.backend.report = None
        self.assertEqual(404, self.client.get(self.path).status_code)
        self.assertFalse(self.queries)

    def test_missing_conversation_is_handled(self):
        self.backend.message["conversation_id"] = None
        self.assertEqual(404, self.client.get(self.path).status_code)

    def test_missing_sender_profile_keeps_messages_visible(self):
        del self.backend.profiles[TARGET]
        html = self.client.get(self.path).get_data(as_text=True)
        self.assertIn("Unavailable profile", html)
        self.assertIn(TARGET, html)
        self.assertIn("Message content", html)

    def test_messages_from_another_chat_are_never_rendered(self):
        self.messages[0].update(conversation_id=ADMIN, body="Unrelated private message")
        response = self.client.get(self.path)
        self.assertEqual(502, response.status_code)
        self.assertNotIn("Unrelated private message", response.get_data(as_text=True))

    def test_private_message_html_is_escaped(self):
        self.messages[0]["body"] = "<script>bad</script>"
        html = self.client.get(self.path).get_data(as_text=True)
        self.assertIn("&lt;script&gt;bad&lt;/script&gt;", html)
        self.assertNotIn("<script>bad", html)
        self.assertNotIn("verified-token", html)

    def test_only_active_admins_can_read_chat(self):
        self.backend.profiles[ADMIN]["is_admin"] = False
        self.assertEqual(403, self.client.get(self.path).status_code)
        self.backend.profiles[ADMIN]["is_admin"] = True
        self.backend.admin_account["banned_until"] = "9999-12-31T00:00:00Z"
        self.assertEqual(403, self.client.get(self.path).status_code)
        self.client.delete_cookie(ACCESS_COOKIE)
        self.assertEqual("/login", self.client.get(self.path).location)
        self.assertFalse(self.queries)

    def test_invalid_pages_do_not_query_chat(self):
        for page in ("0", "-1", "abc", "100001"):
            self.assertEqual(400, self.client.get(self.path + "?page=" + page).status_code)
        self.assertFalse(self.queries)

    def test_backend_failure_does_not_look_like_empty_history(self):
        with patch.object(self.backend, "rows", side_effect=PortalError("Unavailable", 502)):
            response = self.client.get(self.path)
        self.assertEqual(502, response.status_code)
        self.assertNotIn("No messages on this page", response.get_data(as_text=True))

    def test_conversation_view_is_read_only_and_not_cached(self):
        response = self.client.get(self.path)
        self.assertIn("no-store", response.headers["Cache-Control"])
        self.assertFalse(self.backend.patches)
        self.assertFalse(self.backend.bans)


if __name__ == "__main__":
    unittest.main()
