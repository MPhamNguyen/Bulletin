"""Opt-in tests for a DISPOSABLE Bulletin database with the retention migration.

Requires psycopg 3 and BULLETIN_TEST_DATABASE_URL. Fixtures are committed while
separate connections race, then removed. Never point this at production.
"""

import os
import time
import unittest
from concurrent.futures import ThreadPoolExecutor

DATABASE_URL = os.environ.get("BULLETIN_TEST_DATABASE_URL")
AUTHOR = "b0000000-0000-0000-0000-000000000001"
SURVIVOR = "b0000000-0000-0000-0000-000000000002"
REPORTER = "b0000000-0000-0000-0000-000000000003"
CONVERSATION = "b0000000-0000-0000-0000-000000000101"
INCOMING = "b0000000-0000-0000-0000-000000000201"
OUTGOING = "b0000000-0000-0000-0000-000000000202"
REPORT = "b0000000-0000-0000-0000-000000000301"
REPORT_SQL = """insert into public.reports(id,reporter_id,message_id,type,reason)
    values (%s,%s,%s,'message','Spam') returning id"""
EDIT_SQL = "select * from public.edit_message(%s,%s,'Replacement',null)"


@unittest.skipUnless(DATABASE_URL, "Set BULLETIN_TEST_DATABASE_URL to a disposable database")
class MessageRetentionConcurrencyTests(unittest.TestCase):
    def setUp(self):
        import psycopg
        self.psycopg = psycopg
        self.db = psycopg.connect(DATABASE_URL, autocommit=True)
        self.addCleanup(self.db.close)
        # Register cleanup only after inserting all unique Auth fixtures successfully.
        # A collision must not remove pre-existing rows owned by a different test run.
        with self.db.transaction():
            for user_id in (AUTHOR, SURVIVOR, REPORTER):
                self.db.execute("insert into auth.users(id,email) values (%s,%s)",
                                (user_id, user_id + "@retention-test.example.edu"))
        self.addCleanup(self.remove_fixtures)
        self.db.execute("insert into public.conversations(id) values (%s)", (CONVERSATION,))
        self.db.execute("""insert into public.conversation_participants(conversation_id,user_id)
            values (%s,%s),(%s,%s)""", (CONVERSATION, AUTHOR, CONVERSATION, SURVIVOR))
        self.db.execute("""insert into public.messages(id,conversation_id,sender_id,body)
            values (%s,%s,%s,'Incoming'),(%s,%s,%s,'Outgoing')""",
                        (INCOMING, CONVERSATION, AUTHOR, OUTGOING, CONVERSATION, SURVIVOR))

    def remove_fixtures(self):
        self.db.execute("""update public.reports set status='dismissed'
            where id=%s and status in ('open','in_review')""", (REPORT,))
        self.db.execute("delete from auth.users where id in (%s,%s,%s)", (AUTHOR, SURVIVOR, REPORTER))

    def race(self, winning_sql, winning_args, waiting_sql, waiting_args, actor=None, after_wait=None):
        with self.psycopg.connect(DATABASE_URL, autocommit=True) as other:
            if actor:
                other.execute("set role authenticated")
                other.execute("select set_config('request.jwt.claim.sub',%s,false)", (actor,))
                other.execute("select set_config('request.jwt.claim.role','authenticated',false)")
            with ThreadPoolExecutor(max_workers=1) as pool:
                with self.db.transaction():
                    self.db.execute(winning_sql, winning_args)
                    future = pool.submit(self.execute_result, other, waiting_sql, waiting_args)
                    self.wait_for_advisory_lock(other.info.backend_pid)
                    if after_wait:
                        self.db.execute(*after_wait)
                return future.result(timeout=10)

    def execute_result(self, connection, command, parameters):
        try:
            return "ok", connection.execute(command, parameters).fetchone()
        except self.psycopg.Error as error:
            return error.sqlstate, None

    def wait_for_advisory_lock(self, pid):
        deadline = time.monotonic() + 5
        while time.monotonic() < deadline:
            waiting = self.db.execute("""select exists(select 1 from pg_stat_activity
                where pid=%s and wait_event_type='Lock' and wait_event='advisory')""", (pid,)).fetchone()[0]
            if waiting:
                return
            time.sleep(0.01)
        self.fail("Concurrent operation did not wait on the lifecycle advisory lock")

    def test_send_waiting_for_deletion_cannot_use_a_stale_two_member_snapshot(self):
        status, _ = self.race(
            "delete from auth.users where id=%s", (AUTHOR,),
            "insert into public.messages(conversation_id,body) values (%s,'Must fail') returning id",
            (CONVERSATION,), SURVIVOR)
        self.assertEqual(status, "42501")
        self.assertEqual(self.db.execute("select count(*) from public.messages where conversation_id=%s",
                                         (CONVERSATION,)).fetchone()[0], 2)
        self.assertIsNone(self.db.execute("select sender_id from public.messages where id=%s",
                                          (INCOMING,)).fetchone()[0])

    def test_edit_waiting_for_deletion_is_read_only_without_deadlocking(self):
        status, _ = self.race("delete from auth.users where id=%s", (AUTHOR,),
                              EDIT_SQL, (CONVERSATION, OUTGOING), SURVIVOR)
        self.assertEqual(status, "42501")
        self.assertEqual(self.db.execute("select body from public.messages where id=%s",
                                         (OUTGOING,)).fetchone()[0], "Outgoing")

    def test_active_report_committed_first_blocks_account_deletion(self):
        status, _ = self.race(REPORT_SQL, (REPORT, REPORTER, INCOMING),
                              "delete from auth.users where id=%s returning id", (AUTHOR,))
        self.assertEqual(status, "23514")
        self.assertIsNotNone(self.db.execute("select id from auth.users where id=%s", (AUTHOR,)).fetchone())

    def test_deletion_committed_first_still_allows_a_report_on_retained_evidence(self):
        status, _ = self.race("delete from auth.users where id=%s", (AUTHOR,),
                              REPORT_SQL, (REPORT, SURVIVOR, INCOMING), SURVIVOR)
        self.assertEqual(status, "ok")
        self.assertEqual(self.db.execute("select status from public.reports where id=%s",
                                         (REPORT,)).fetchone()[0], "open")

    def test_simultaneous_last_account_deletions_remove_the_conversation(self):
        status, _ = self.race("delete from auth.users where id=%s", (AUTHOR,),
                              "delete from auth.users where id=%s returning id", (SURVIVOR,))
        self.assertEqual(status, "ok")
        self.assertIsNone(self.db.execute("select id from public.conversations where id=%s",
                                          (CONVERSATION,)).fetchone())

    def test_report_completion_before_last_deletion_allows_cleanup(self):
        self.db.execute("delete from auth.users where id=%s", (AUTHOR,))
        self.db.execute(REPORT_SQL, (REPORT, SURVIVOR, INCOMING))
        status, _ = self.race("update public.reports set status='resolved' where id=%s", (REPORT,),
                              "delete from auth.users where id=%s returning id", (SURVIVOR,))
        self.assertEqual(status, "ok")
        self.assertIsNone(self.db.execute("select id from public.conversations where id=%s",
                                          (CONVERSATION,)).fetchone())

    def test_last_deletion_before_report_completion_retains_then_purges_evidence(self):
        self.db.execute("delete from auth.users where id=%s", (AUTHOR,))
        self.db.execute(REPORT_SQL, (REPORT, SURVIVOR, INCOMING))
        status, _ = self.race("delete from auth.users where id=%s", (SURVIVOR,),
                              "update public.reports set status='resolved' where id=%s returning id", (REPORT,))
        self.assertEqual(status, "ok")
        self.assertIsNone(self.db.execute("select id from public.conversations where id=%s",
                                          (CONVERSATION,)).fetchone())

    def test_edit_and_reporting_use_a_consistent_lock_order(self):
        status, _ = self.race(REPORT_SQL, (REPORT, REPORTER, OUTGOING),
                              EDIT_SQL, (CONVERSATION, OUTGOING), SURVIVOR)
        self.assertEqual(status, "BM003")

    def test_profile_reconciliation_waits_before_locking_the_auth_row(self):
        status, _ = self.race(
            "select private.lock_conversation_lifecycle()", (),
            "select public.ensure_my_profile()", (), AUTHOR,
            after_wait=("delete from auth.users where id=%s", (AUTHOR,)))
        self.assertEqual(status, "28000")

    def test_auth_email_updates_wait_before_locking_the_auth_row(self):
        status, _ = self.race(
            "select private.lock_conversation_lifecycle()", (),
            "update auth.users set email='updated@retention-test.example.edu' where id=%s returning id", (AUTHOR,),
            after_wait=("delete from auth.users where id=%s", (AUTHOR,)))
        self.assertEqual(status, "ok")


if __name__ == "__main__":
    unittest.main()
