"""Opt-in sale/write races against a DISPOSABLE Bulletin database.

Requires psycopg 3, BULLETIN_TEST_DATABASE_URL, the deployed baseline, and
20261123000100_lock_sold_listing_conversations.sql. Fixtures are committed so
independent connections can race, then removed. Never use a production database.
"""

import os
import time
import unittest
from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

DATABASE_URL = os.environ.get("BULLETIN_TEST_DATABASE_URL")


@unittest.skipUnless(DATABASE_URL, "Set BULLETIN_TEST_DATABASE_URL to a disposable database")
class SoldConversationConcurrencyTests(unittest.TestCase):
    def setUp(self):
        import psycopg
        self.psycopg = psycopg
        self.db = psycopg.connect(DATABASE_URL, autocommit=True)
        self.addCleanup(self.db.close)
        self.seller, self.buyer, self.listing, self.thread, self.message = (
            str(uuid4()) for _ in range(5)
        )
        with self.db.transaction():
            for user in (self.seller, self.buyer):
                self.db.execute("insert into auth.users(id,email) values (%s,%s)",
                                (user, user + "@sold-conversation-test.example.edu"))
        self.addCleanup(self.remove_fixtures)
        with self.db.transaction():
            self.db.execute("""insert into public.listings
                (id,user_id,name,description,category,condition,price,status)
                values (%s,%s,'Test textbook','Fixture','TEXTBOOKS','NEW',10,'AVAILABLE')""",
                            (self.listing, self.seller))
            self.db.execute("insert into public.conversations(id,listing_id) values (%s,%s)",
                            (self.thread, self.listing))
            self.db.execute("""insert into public.conversation_participants(conversation_id,user_id)
                values (%s,%s),(%s,%s)""", (self.thread, self.seller, self.thread, self.buyer))
            self.db.execute("""insert into public.messages(id,conversation_id,sender_id,body)
                values (%s,%s,%s,'Original')""", (self.message, self.thread, self.buyer))

    def remove_fixtures(self):
        self.db.execute("delete from public.conversations where id=%s", (self.thread,))
        self.db.execute("delete from public.listings where id=%s", (self.listing,))
        self.db.execute("delete from auth.users where id in (%s,%s)", (self.seller, self.buyer))

    def execute_result(self, connection, command, parameters):
        try:
            connection.execute(command, parameters).fetchone()
            return "ok"
        except self.psycopg.Error as error:
            return error.sqlstate

    def race_sale_with_write(self, command, parameters):
        with self.psycopg.connect(DATABASE_URL, autocommit=True) as other:
            other.execute("set statement_timeout='10s'")
            other.execute("set role authenticated")
            other.execute("select set_config('request.jwt.claim.sub',%s,false)", (self.buyer,))
            other.execute("select set_config('request.jwt.claim.role','authenticated',false)")
            with ThreadPoolExecutor(max_workers=1) as pool:
                with self.db.transaction():
                    self.db.execute("""update public.listings set status='SOLD',buyer_id=%s
                        where id=%s""", (self.buyer, self.listing))
                    future = pool.submit(self.execute_result, other, command, parameters)
                    self.wait_until_blocked(other.info.backend_pid)
                self.assertEqual(future.result(timeout=15), "BM006")
        self.assertEqual(self.db.execute("select body from public.messages where id=%s",
                                         (self.message,)).fetchone()[0], "Original")
        self.assertEqual(self.db.execute("select count(*) from public.messages where conversation_id=%s",
                                         (self.thread,)).fetchone()[0], 1)

    def wait_until_blocked(self, pid):
        deadline = time.monotonic() + 5
        while time.monotonic() < deadline:
            blocked = self.db.execute("select %s=any(pg_blocking_pids(%s))",
                                      (self.db.info.backend_pid, pid)).fetchone()[0]
            if blocked:
                return
            time.sleep(0.01)
        self.fail("Write did not wait for the in-progress sale")

    def test_direct_insert_waiting_for_sale_cannot_send_using_stale_state(self):
        self.race_sale_with_write(
            "insert into public.messages(conversation_id,body) values (%s,'Must fail') returning id",
            (self.thread,))

    def test_edit_rpc_waiting_for_sale_cannot_change_message_body(self):
        self.race_sale_with_write(
            "select * from public.edit_message(%s,%s,'Must fail',null)",
            (self.thread, self.message))
