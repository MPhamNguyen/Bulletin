import unittest

from fakes import ADMIN, REPORT, FakeSupabase
from portal.backend import PortalError
from portal.moderation_history import add_report_event, normalize_comment, report_events


class ModerationHistoryTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()

    def test_event_is_saved_with_report_moderator_type_and_comment(self):
        result = add_report_event(
            self.backend, REPORT, ADMIN, "note_added", "  Reviewed evidence.  "
        )
        self.assertEqual(REPORT, result["report_id"])
        self.assertEqual(ADMIN, result["moderator_id"])
        self.assertEqual("note_added", result["event_type"])
        self.assertEqual("Reviewed evidence.", result["comment"])

    def test_history_is_report_scoped_and_resolves_moderator_identity(self):
        add_report_event(self.backend, REPORT, ADMIN, "note_added", "Internal note")
        events = report_events(self.backend, REPORT)
        self.assertEqual(1, len(events))
        self.assertEqual("Admin", events[0]["moderator"]["full_name"])
        self.assertEqual("eq." + REPORT, self.backend.queries[-1][1]["report_id"])
        self.assertEqual("created_at.desc,id.desc", self.backend.queries[-1][1]["order"])

    def test_comments_are_trimmed_and_empty_or_overlong_comments_rejected(self):
        self.assertEqual("Why", normalize_comment(" Why "))
        self.assertEqual(4000, len(normalize_comment("x" * 4000)))
        for value in ("   ", "x" * 4001):
            with self.subTest(length=len(value)):
                with self.assertRaises(PortalError):
                    normalize_comment(value)

    def test_comment_event_rejects_invalid_identifiers(self):
        with self.assertRaises(PortalError):
            add_report_event(self.backend, "bad-id", ADMIN, "note_added", "Note")


if __name__ == "__main__":
    unittest.main()
