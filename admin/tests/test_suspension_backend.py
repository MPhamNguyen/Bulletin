import io
import json
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

from portal.backend import PortalError, Supabase


class SuspensionBackendTests(unittest.TestCase):
    def setUp(self):
        self.backend = Supabase("https://project.supabase.co", "anon-test", "service-test")

    def restore(self):
        return self.backend.set_ban("target-id", "none", actor_id="admin-id", operation="restore",
                                    version={"banned_until": "2099-01-01T00:00:00Z",
                                             "updated_at": "2026-01-01T00:00:00Z", "suspended": True})

    @patch("portal.backend.urlopen")
    def test_restore_sends_version_and_no_duration(self, open_url):
        open_url.return_value = io.BytesIO(b'"target-id"')
        self.assertEqual("target-id", self.restore())
        data = json.loads(open_url.call_args.args[0].data)
        self.assertIsNone(data["p_minutes"])
        self.assertTrue(data["p_expected_suspended"])
        self.assertEqual("2099-01-01T00:00:00Z", data["p_expected_banned_until"])
        self.assertEqual("2026-01-01T00:00:00Z", data["p_expected_updated_at"])

    @patch("portal.backend.urlopen")
    def test_rpc_conflicts_and_authorization_failures_are_translated(self, open_url):
        for code, status in (("PT409", 409), ("42501", 403), ("PGRST202", 502)):
            with self.subTest(code=code):
                open_url.side_effect = HTTPError("url", 400, "private", {},
                                                io.BytesIO(json.dumps({"code": code}).encode()))
                with self.assertRaises(PortalError) as caught:
                    self.restore()
                self.assertEqual(status, caught.exception.status)

    @patch("portal.backend.urlopen")
    def test_unconfirmed_rpc_response_is_not_success(self, open_url):
        for body in (b'null', b'[]', b'{}', b'"wrong-id"'):
            open_url.return_value = io.BytesIO(body)
            with self.assertRaises(PortalError) as caught:
                self.restore()
            self.assertEqual(502, caught.exception.status)


if __name__ == "__main__":
    unittest.main()
