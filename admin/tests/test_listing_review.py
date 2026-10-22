import re
import unittest
from unittest.mock import patch

from fakes import ADMIN, LISTING, REPORT, FakeSupabase
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app


class ListingReviewTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.backend.report.update(type="listing", reported_user_id=None, listing_id=LISTING)
        self.backend.images = [dict(object_path=LISTING + "/first image.jpg", position=0)]
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.path = "/reports/" + REPORT

    def form(self):
        response = self.client.get(self.path)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        match = re.search(r'<form method="post" action="' + self.path + r'/listing/delete">(.*?)</form>', html, re.S)
        self.assertIsNotNone(match)
        return {name: re.search(r'name="' + name + r'" value="([^"]+)"', match.group(1)).group(1)
                for name in ("csrf", "version")}

    def delete(self, data):
        return self.client.post(self.path + "/listing/delete", data=data)

    def test_listing_photos_use_public_bucket_urls_and_allowed_storage_origin(self):
        response = self.client.get(self.path)
        html = response.get_data(as_text=True)
        self.assertIn("https://project.supabase.co/storage/v1/object/public/listing_imgs/" + LISTING + "/first%20image.jpg", html)
        self.assertIn("Open full image", html)
        self.assertIn("listing-photos", html)
        self.assertIn("img-src 'self' https://project.supabase.co;", response.headers["Content-Security-Policy"])
        self.assertNotIn("verified-token", html)
        query = next(query for table, query in self.backend.queries if table == "listing_images")
        self.assertEqual("eq." + LISTING, query["listing_id"])
        self.assertEqual("position.asc,id.asc", query["order"])

    def test_no_photos_has_an_explicit_empty_state(self):
        self.backend.images = []
        self.assertIn("This listing has no photos.", self.client.get(self.path).get_data(as_text=True))

    def test_photo_read_failure_keeps_report_and_status_form_usable(self):
        with patch.object(self.backend, "rows", side_effect=PortalError("Storage metadata unavailable", 502)):
            response = self.client.get(self.path)
        self.assertEqual(200, response.status_code)
        self.assertIn("Listing photos could not be loaded", response.get_data(as_text=True))
        self.assertIn("Save report status", response.get_data(as_text=True))

    def test_confirmation_deletes_listing_and_all_associated_reports(self):
        self.backend.listing_reports[LISTING].append("another-report")
        data = self.form()
        data.update(confirm="delete", listing_id=ADMIN)
        result = self.delete(data)
        self.assertEqual(303, result.status_code)
        self.assertEqual("/reports", result.location)
        self.assertIsNone(self.backend.listing)
        self.assertIsNone(self.backend.report)
        self.assertNotIn(LISTING, self.backend.listing_reports)
        self.assertEqual(["unrelated-report"], self.backend.listing_reports["unrelated-listing"])
        self.assertEqual("eq." + LISTING, self.backend.deletes[0]["id"])
        self.assertEqual("eq.2026-10-01T00:00:00Z", self.backend.deletes[0]["updated_at"])
        self.assertEqual([{"prefixes": [LISTING + "/first image.jpg"]}], self.backend.storage_deletes)

    def test_missing_confirmation_does_not_delete_anything(self):
        data = self.form()
        self.assertEqual(400, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)
        self.assertFalse(self.backend.storage_deletes)

    def test_authentication_role_and_csrf_are_required_for_deletion(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.profiles[ADMIN]["is_admin"] = False
        self.assertEqual(403, self.delete(data).status_code)
        self.backend.profiles[ADMIN]["is_admin"] = True
        self.client.delete_cookie(ACCESS_COOKIE)
        self.assertEqual(303, self.delete(data).status_code)
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        data["csrf"] = "forged"
        self.assertEqual(400, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)

    def test_status_form_token_cannot_authorize_deletion(self):
        response = self.client.get(self.path).get_data(as_text=True)
        data = self.form()
        data.update(confirm="delete", version=re.search(r'name="version" value="([^"]+)"', response).group(1))
        self.assertEqual(409, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)

    def test_changed_report_and_changed_listing_require_a_new_review(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.report["status"] = "in_review"
        self.assertEqual(409, self.delete(data).status_code)
        data = self.form()
        data["confirm"] = "delete"
        self.backend.listing["updated_at"] = "2026-10-21T00:00:00Z"
        self.assertEqual(409, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)

    def test_deletion_race_retains_report_and_photo_files(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.delete_conflict = True
        self.assertEqual(409, self.delete(data).status_code)
        self.assertIsNotNone(self.backend.report)
        self.assertFalse(self.backend.storage_deletes)

    def test_foreign_key_failure_retains_report_and_photo_files(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.delete_blocked = True
        self.assertEqual(409, self.delete(data).status_code)
        self.assertIsNotNone(self.backend.report)
        self.assertIsNotNone(self.backend.listing)
        self.assertFalse(self.backend.storage_deletes)

    def test_storage_cleanup_failure_reports_partial_result_after_database_success(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.storage_failures = 2
        self.assertEqual(303, self.delete(data).status_code)
        self.assertIsNone(self.backend.report)
        self.assertEqual(2, len(self.backend.storage_deletes))
        with self.client.session_transaction() as session:
            message = " ".join(message for _, message in session["_flashes"])
        self.assertIn("reports were deleted", message)
        self.assertIn("photo files could not be removed", message)

    def test_idempotent_storage_cleanup_can_recover_from_one_failure(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.storage_failures = 1
        self.assertEqual(303, self.delete(data).status_code)
        self.assertEqual(2, len(self.backend.storage_deletes))
        with self.client.session_transaction() as session:
            self.assertNotIn("could not be removed", str(session["_flashes"]))

    def test_unrelated_storage_paths_cannot_be_displayed_or_deleted(self):
        data = self.form()
        data["confirm"] = "delete"
        for path in (ADMIN + "/image.jpg", LISTING + "/../image.jpg", "https://evil.test/image.jpg"):
            self.backend.images = [dict(object_path=path, position=0)]
            response = self.client.get(self.path)
            self.assertNotIn('src="https://evil.test', response.get_data(as_text=True))
            self.assertEqual(502, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)
        self.assertFalse(self.backend.storage_deletes)

    def test_user_reports_cannot_delete_a_listing(self):
        data = self.form()
        data["confirm"] = "delete"
        self.backend.report.update(type="user", listing_id=None)
        self.assertEqual(400, self.delete(data).status_code)
        self.assertFalse(self.backend.deletes)


if __name__ == "__main__":
    unittest.main()
