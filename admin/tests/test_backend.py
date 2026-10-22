import io
import json
import unittest
from unittest.mock import patch
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlsplit

from portal.backend import PortalError, Supabase


class Response(io.BytesIO):
    pass


class SupabaseTests(unittest.TestCase):
    def setUp(self):
        self.backend = Supabase("https://project.supabase.co", "anon-test", "service-test")

    @patch("portal.backend.urlopen")
    def test_login_uses_anon_key_and_sends_credentials_only_to_auth(self, open_url):
        open_url.return_value = Response(b'{"access_token":"issued"}')
        self.assertEqual("issued", self.backend.sign_in("admin@example.test", "password")["access_token"])
        request = open_url.call_args.args[0]
        self.assertEqual("anon-test", request.headers["Apikey"])
        self.assertNotIn("Authorization", request.headers)
        self.assertEqual("/auth/v1/token", urlsplit(request.full_url).path)
        self.assertEqual(["password"], parse_qs(urlsplit(request.full_url).query)["grant_type"])
        self.assertEqual("password", json.loads(request.data)["password"])

    @patch("portal.backend.urlopen")
    def test_identity_uses_user_token_not_service_role(self, open_url):
        open_url.return_value = Response(b'{"id":"user-id"}')
        self.backend.current_user("user-token")
        request = open_url.call_args.args[0]
        self.assertEqual("anon-test", request.headers["Apikey"])
        self.assertEqual("Bearer user-token", request.headers["Authorization"])

    @patch("portal.backend.urlopen")
    def test_conditional_patch_encodes_filters_and_requests_returned_row(self, open_url):
        open_url.return_value = Response(b'[{"id":"report-id"}]')
        rows = self.backend.request("PATCH", "/rest/v1/reports", query={
            "id": "eq.report-id", "modified_at": "eq.2026-10-21T01:00:00+00:00",
        }, data={"status": "dismissed", "modified_at": "2026-10-21T02:00:00+00:00"})
        request = open_url.call_args.args[0]
        self.assertEqual("Bearer service-test", request.headers["Authorization"])
        self.assertEqual("return=representation", request.headers["Prefer"])
        self.assertEqual("PATCH", request.method)
        self.assertEqual(["eq.2026-10-21T01:00:00+00:00"], parse_qs(urlsplit(request.full_url).query)["modified_at"])
        self.assertEqual([{"id": "report-id"}], rows)

    @patch("portal.backend.urlopen")
    def test_ban_is_server_only_auth_admin_put(self, open_url):
        open_url.return_value = Response(b'{"id":"target-id"}')
        self.backend.set_ban("target-id", "24h")
        request = open_url.call_args.args[0]
        self.assertEqual("PUT", request.method)
        self.assertEqual("/auth/v1/admin/users/target-id", urlsplit(request.full_url).path)
        self.assertEqual({"ban_duration": "24h"}, json.loads(request.data))

    @patch("portal.backend.urlopen")
    def test_http_failures_do_not_expose_backend_response_or_credentials(self, open_url):
        for code in (400, 401, 403, 409, 429, 500):
            with self.subTest(code=code):
                open_url.side_effect = HTTPError("url", code, "private-details", {}, io.BytesIO(b"secret-data"))
                with self.assertRaises(PortalError) as caught:
                    self.backend.rows("reports", select="id")
                self.assertNotIn("private-details", str(caught.exception))
                self.assertNotIn("secret-data", str(caught.exception))
                self.assertIn(caught.exception.status, (429, 502))

    @patch("portal.backend.urlopen")
    def test_timeout_and_network_failures_are_safe_and_not_retried(self, open_url):
        for error in (TimeoutError(), URLError("private-host")):
            open_url.reset_mock()
            open_url.side_effect = error
            with self.assertRaises(PortalError) as caught:
                self.backend.set_ban("target-id", "24h")
            self.assertEqual(502, caught.exception.status)
            self.assertNotIn("private-host", str(caught.exception))
            self.assertEqual(1, open_url.call_count)

    def test_configuration_requires_https_and_server_keys(self):
        self.assertTrue(self.backend.configured())
        self.assertFalse(Supabase("http://project.test", "anon", "service").configured())
        self.assertFalse(Supabase("https://project.test", "anon", "").configured())

    @patch("portal.backend.urlopen")
    def test_listing_delete_requests_confirmation_and_uses_conditional_filters(self, open_url):
        open_url.return_value = Response(b'[{"id":"listing-id"}]')
        result = self.backend.request("DELETE", "/rest/v1/listings", query={
            "id": "eq.listing-id", "updated_at": "eq.2026-10-21T01:00:00+00:00", "select": "id",
        })
        request = open_url.call_args.args[0]
        self.assertEqual("DELETE", request.method)
        self.assertEqual("return=representation", request.headers["Prefer"])
        self.assertIsNone(request.data)
        self.assertEqual([{"id": "listing-id"}], result)

    @patch("portal.backend.urlopen")
    def test_storage_cleanup_uses_exact_paths_in_server_only_delete_request(self, open_url):
        open_url.return_value = Response(b'[]')
        self.backend.request("DELETE", "/storage/v1/object/listing_imgs", data={"prefixes": ["listing-id/image.jpg"]})
        request = open_url.call_args.args[0]
        self.assertEqual("DELETE", request.method)
        self.assertEqual("Bearer service-test", request.headers["Authorization"])
        self.assertEqual({"prefixes": ["listing-id/image.jpg"]}, json.loads(request.data))
        self.assertNotIn("Prefer", request.headers)

    @patch("portal.backend.urlopen")
    def test_restricted_listing_deletion_is_a_safe_conflict(self, open_url):
        body = json.dumps({"code": "23503", "details": "private report details"}).encode()
        open_url.side_effect = HTTPError("url", 409, "Conflict", {}, io.BytesIO(body))
        with self.assertRaises(PortalError) as caught:
            self.backend.request("DELETE", "/rest/v1/listings", query={"id": "eq.listing-id"})
        self.assertEqual(409, caught.exception.status)
        self.assertIn("reports were retained", str(caught.exception))
        self.assertNotIn("private", str(caught.exception))

    @patch("portal.backend.urlopen")
    def test_rejected_report_status_names_the_rule_without_exposing_report_data(self, open_url):
        body = json.dumps({
            "code": "23514",
            "message": 'new row violates check constraint "reports_status_check"',
            "details": "private report content and account data",
        }).encode()
        open_url.side_effect = HTTPError("url", 400, "Bad Request", {}, io.BytesIO(body))
        with self.assertLogs("portal.backend", level="WARNING") as logs:
            with self.assertRaises(PortalError) as caught:
                self.backend.request("PATCH", "/rest/v1/reports", data={"status": "action_taken"})
        self.assertEqual(422, caught.exception.status)
        self.assertIn("reports_status_check", str(caught.exception))
        self.assertIn("23514", str(caught.exception))
        self.assertNotIn("private report content", str(caught.exception))
        self.assertNotIn("private report content", " ".join(logs.output))

    @patch("portal.backend.urlopen")
    def test_other_database_failures_expose_only_validated_codes(self, open_url):
        for code in ("42501", "42703", "PGRST204", "private-report-content"):
            with self.subTest(code=code):
                body = json.dumps({"code": code, "details": "private report content"}).encode()
                open_url.side_effect = HTTPError("url", 400, "Bad Request", {}, io.BytesIO(body))
                with self.assertRaises(PortalError) as caught:
                    self.backend.request("PATCH", "/rest/v1/reports", data={"status": "action_taken"})
                message = str(caught.exception)
                self.assertNotIn("private", message)
                if code != "private-report-content":
                    self.assertIn(code, message)

    @patch("portal.backend.urlopen")
    def test_deleted_auth_user_is_missing_not_a_failed_review(self, open_url):
        open_url.side_effect = HTTPError("url", 404, "not-found", {}, io.BytesIO(b"{}"))
        self.assertIsNone(self.backend.auth_user("target-id"))
        with self.assertRaises(PortalError):
            self.backend.set_ban("target-id", "24h")

    @patch("portal.backend.urlopen")
    def test_malformed_table_response_is_an_explicit_failure(self, open_url):
        open_url.return_value = Response(b'{"unexpected":"object"}')
        with self.assertRaises(PortalError) as caught:
            self.backend.rows("reports", select="id")
        self.assertEqual(502, caught.exception.status)


if __name__ == "__main__":
    unittest.main()
