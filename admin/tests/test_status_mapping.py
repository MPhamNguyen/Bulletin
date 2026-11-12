import unittest

from portal.moderation import KOTLIN_TO_PORTAL_STATUS, PORTAL_TO_KOTLIN_STATUS, STATUSES


class ReportStatusMappingTests(unittest.TestCase):
    def test_every_kotlin_status_has_an_explicit_portal_mapping_or_drop(self):
        self.assertEqual(
            {
                "OPEN": "open",
                "UNDER_REVIEW": "in_review",
                "ACTION_TAKEN": "resolved",
                "DISMISSED": "dismissed",
                "UNKNOWN": None,
            },
            KOTLIN_TO_PORTAL_STATUS,
        )

    def test_every_portal_status_maps_back_to_its_kotlin_status(self):
        self.assertEqual(set(STATUSES), set(PORTAL_TO_KOTLIN_STATUS))
        self.assertEqual(
            {
                "open": "OPEN",
                "in_review": "UNDER_REVIEW",
                "resolved": "ACTION_TAKEN",
                "dismissed": "DISMISSED",
            },
            PORTAL_TO_KOTLIN_STATUS,
        )


if __name__ == "__main__":
    unittest.main()
