import unittest
from fakes import ADMIN, TARGET, REPORT, FakeSupabase
from portal.account_access import require_active_account
from portal.backend import PortalError
from portal.web import ACCESS_COOKIE, create_app, version_serializer
from portal.moderation import snapshot


class AccountAccessTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()

    def test_null_and_expired_bans_allow_an_active_account(self):
        for expires in (None, "", "2020-01-01T00:00:00Z"):
            with self.subTest(expires=expires):
                self.backend.admin_account["banned_until"] = expires
                require_active_account(self.backend, ADMIN)

    def test_future_ban_blocks_account_access(self):
        self.backend.admin_account["banned_until"] = "9999-12-31T23:59:59+00:00"
        with self.assertRaises(PortalError) as caught:
            require_active_account(self.backend, ADMIN)
        self.assertEqual(403, caught.exception.status)

    def test_invalid_ban_timestamp_fails_closed(self):
        for expires in ("invalid", "2026-10-21T00:00:00", 123, 0, False):
            with self.subTest(expires=expires):
                self.backend.admin_account["banned_until"] = expires
                with self.assertRaises(PortalError) as caught:
                    require_active_account(self.backend, ADMIN)
                self.assertEqual(503, caught.exception.status)

    def test_missing_or_mismatched_auth_account_fails_closed(self):
        for account in (None, {"id": TARGET, "banned_until": None}):
            self.backend.admin_account = account
            with self.assertRaises(PortalError) as caught:
                require_active_account(self.backend, ADMIN)
            self.assertEqual(403, caught.exception.status)

    def test_suspended_admin_cannot_log_in(self):
        app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        client = app.test_client()
        client.get("/login")
        with client.session_transaction() as session:
            token = session["csrf"]
        self.backend.admin_account["banned_until"] = "9999-12-31T23:59:59Z"
        response = client.post("/login", data=dict(csrf=token, email="admin@example.test", password="correct"))
        self.assertEqual(403, response.status_code)
        self.assertIsNone(client.get_cookie(ACCESS_COOKIE))

    def test_existing_admin_session_cannot_read_or_write_after_suspension(self):
        app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        client = app.test_client()
        client.set_cookie(ACCESS_COOKIE, "verified-token")
        client.get("/reports/" + REPORT)
        with client.session_transaction() as session:
            token = session["csrf"]
        with app.app_context():
            version = version_serializer().dumps(snapshot(self.backend.report))
        self.backend.admin_account["banned_until"] = "9999-12-31T23:59:59Z"
        self.assertEqual(403, client.get("/").status_code)
        response = client.post("/reports/" + REPORT + "/status", data=dict(
            csrf=token, version=version, status="resolved",
        ))
        self.assertEqual(403, response.status_code)
        self.assertFalse(self.backend.patches)

    def test_restoring_admin_access_is_observed_on_next_request(self):
        app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        client = app.test_client()
        client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.backend.admin_account["banned_until"] = "9999-12-31T23:59:59Z"
        self.assertEqual(403, client.get("/").status_code)
        self.backend.admin_account["banned_until"] = None
        self.assertEqual(200, client.get("/").status_code)


if __name__ == "__main__":
    unittest.main()
