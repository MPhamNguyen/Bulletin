"""Opt-in PostgreSQL contract tests; requires psycopg 3 and a DISPOSABLE database.

Install the moderate_account_suspension RPC on an existing Bulletin schema first, then run:
  BULLETIN_TEST_DATABASE_URL=... python -B -m unittest discover -s supabase/tests -p test_account_suspension.py -v
The configured database must allow fixture writes to auth.users and SET ROLE.
"""

import os
import time
import unittest
from concurrent.futures import ThreadPoolExecutor

DATABASE_URL = os.environ.get("BULLETIN_TEST_DATABASE_URL")
ACTOR = "00000000-0000-0000-0000-0000000000a7"
TARGET = "00000000-0000-0000-0000-0000000000b7"
COMMAND = "select public.moderate_account_suspension(%s, %s, %s, %s, %s, %s, %s)"
SIGNATURE = "public.moderate_account_suspension(uuid,uuid,timestamptz,timestamptz,boolean,text,integer)"


@unittest.skipUnless(DATABASE_URL, "Set BULLETIN_TEST_DATABASE_URL to a disposable database")
class SuspensionDatabaseTests(unittest.TestCase):
    def setUp(self):
        import psycopg
        self.psycopg = psycopg
        self.db = psycopg.connect(DATABASE_URL, autocommit=True)
        self.addCleanup(self.db.close)
        self.addCleanup(self.remove_fixtures)
        for user_id, admin in ((ACTOR, True), (TARGET, False)):
            email = user_id + "@suspension-test.example.edu"
            self.db.execute("insert into auth.users(id,email) values (%s,%s)", (user_id, email))
            self.db.execute("""insert into public.profiles(id,email,full_name,is_admin)
                values (%s,%s,'Suspension fixture',%s)
                on conflict (id) do update set is_admin=excluded.is_admin, deleted_at=null""",
                            (user_id, email, admin))

    def remove_fixtures(self):
        self.db.execute("delete from public.profiles where id in (%s,%s)", (ACTOR, TARGET))
        self.db.execute("delete from auth.users where id in (%s,%s)", (ACTOR, TARGET))

    def snapshot(self):
        return self.db.execute("""select banned_until, updated_at,
            coalesce(banned_until > clock_timestamp(),false) from auth.users where id=%s""",
                               (TARGET,)).fetchone()

    def command(self, operation, version=None, minutes=None, connection=None):
        version = self.snapshot() if version is None else version
        return (connection or self.db).execute(
            COMMAND, (TARGET, ACTOR, *version, operation, minutes)).fetchone()[0]

    def expect_error(self, code, operation, version=None, minutes=None):
        with self.assertRaises(self.psycopg.Error) as caught:
            self.command(operation, version, minutes)
        self.assertEqual(code, caught.exception.sqlstate)

    def test_create_update_restore_and_replay(self):
        initial = self.snapshot()
        self.assertEqual(str(self.command("suspend", initial, 60)), TARGET)
        suspended = self.snapshot()
        self.assertTrue(suspended[2])
        self.expect_error("PT409", "suspend", initial, 60)
        self.expect_error("PT409", "suspend", suspended, 60)
        self.command("update", suspended, 120)
        self.assertGreater(self.snapshot()[0], suspended[0])
        active = self.snapshot()
        self.command("restore", active)
        self.assertIsNone(self.snapshot()[0])
        self.expect_error("PT409", "restore", active)
        self.expect_error("PT409", "restore")
        self.expect_error("PT409", "update", minutes=1)

    def test_expiry_is_checked_even_without_a_version_field_change(self):
        self.db.execute("update auth.users set banned_until=clock_timestamp()-interval '1 minute' where id=%s",
                        (TARGET,))
        expiry, updated, suspended = self.snapshot()
        self.assertFalse(suspended)
        # The same stored fields with the derived state from a pre-expiry form.
        self.expect_error("PT409", "restore", (expiry, updated, True))
        self.expect_error("PT409", "update", (expiry, updated, True), 1)
        self.command("suspend", minutes=1)

    def test_invalid_commands_do_not_change_account(self):
        initial = self.snapshot()
        for operation, minutes in (("suspend", 0), ("suspend", 525601), ("update", None),
                                   ("restore", 1), ("unknown", 1), (None, 1)):
            self.expect_error("22023", operation, minutes=minutes)
            self.assertEqual(initial, self.snapshot())
        for minutes in (1, 525600):
            self.command("suspend", minutes=minutes)
            self.command("restore")

    def test_actor_and_target_protections_are_enforced_in_database(self):
        for user_id, assignment in ((ACTOR, "is_admin=false"), (TARGET, "is_admin=true"),
                                    (TARGET, "deleted_at=clock_timestamp()")):
            with self.db.transaction(force_rollback=True):
                self.db.execute("update public.profiles set " + assignment + " where id=%s", (user_id,))
                self.expect_error("42501", "suspend", minutes=1)
        with self.db.transaction(force_rollback=True):
            self.db.execute("update auth.users set banned_until=clock_timestamp()+interval '1 hour' where id=%s",
                            (ACTOR,))
            self.expect_error("42501", "suspend", minutes=1)

    def test_only_service_role_can_execute(self):
        for role, allowed in (("anon", False), ("authenticated", False), ("service_role", True)):
            self.assertEqual(allowed, self.db.execute(
                "select has_function_privilege(%s,%s,'EXECUTE')", (role, SIGNATURE)).fetchone()[0])
        version = self.snapshot()
        with self.db.transaction():
            self.db.execute("set local role service_role")
            self.command("suspend", version, 1)

    def test_waiting_command_cannot_overwrite_winning_decision(self):
        self.command("suspend", minutes=60)
        version = self.snapshot()
        with self.psycopg.connect(DATABASE_URL, autocommit=True) as other:
            pid = other.info.backend_pid
            with ThreadPoolExecutor(max_workers=1) as pool:
                with self.db.transaction():
                    self.command("restore", version)
                    future = pool.submit(self.command, "update", version, 120, other)
                    self.wait_for_lock(pid)
                with self.assertRaises(self.psycopg.Error) as caught:
                    future.result(timeout=10)
                self.assertEqual("PT409", caught.exception.sqlstate)
        self.assertIsNone(self.snapshot()[0])

    def test_expiry_while_waiting_for_lock_rejects_unchanged_account(self):
        self.db.execute("update auth.users set banned_until=clock_timestamp()+interval '2 seconds' where id=%s",
                        (TARGET,))
        version = self.snapshot()
        self.assertTrue(version[2])
        with self.psycopg.connect(DATABASE_URL, autocommit=True) as other:
            with ThreadPoolExecutor(max_workers=1) as pool:
                with self.db.transaction():
                    self.db.execute("select id from auth.users where id=%s for update", (TARGET,))
                    future = pool.submit(self.command, "restore", version, None, other)
                    self.wait_for_lock(other.info.backend_pid)
                    deadline = time.monotonic() + 10
                    while self.db.execute("select clock_timestamp() < %s", (version[0],)).fetchone()[0]:
                        if time.monotonic() > deadline:
                            self.fail("Database clock did not reach suspension expiry")
                        time.sleep(0.01)
                with self.assertRaises(self.psycopg.Error) as caught:
                    future.result(timeout=10)
                self.assertEqual("PT409", caught.exception.sqlstate)
        self.assertEqual(version[:2], self.snapshot()[:2])

    def wait_for_lock(self, pid):
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            waiting = self.db.execute(
                "select wait_event_type='Lock' from pg_stat_activity where pid=%s", (pid,)).fetchone()
            if waiting and waiting[0]:
                return
            time.sleep(0.01)
        self.fail("Competing command did not reach the row lock")


if __name__ == "__main__":
    unittest.main()
