from copy import deepcopy

from portal.backend import PortalError

ADMIN = "00000000-0000-0000-0000-000000000001"
TARGET = "00000000-0000-0000-0000-000000000002"
REPORTER = "00000000-0000-0000-0000-000000000003"
REPORT = "00000000-0000-0000-0000-000000000004"
MESSAGE = "00000000-0000-0000-0000-000000000005"
LISTING = "00000000-0000-0000-0000-000000000006"


class FakeSupabase:
    def __init__(self):
        self.available = True
        self.profiles = {
            ADMIN: dict(id=ADMIN, is_admin=True, full_name="Admin", email="admin@example.test", deleted_at=None),
            TARGET: dict(id=TARGET, is_admin=False, full_name="Target", email="target@example.test", deleted_at=None),
            REPORTER: dict(id=REPORTER, is_admin=False, full_name="Reporter", email="reporter@example.test", deleted_at=None),
        }
        self.report = dict(
            id=REPORT, type="user", status="open", reporter_id=REPORTER,
            reported_user_id=TARGET, listing_id=None, message_id=None, reason="scam_or_fraud",
            description="Please review", created_at="2026-10-01T00:00:00+00:00",
            modified_at=None,
        )
        self.message = dict(id=MESSAGE, sender_id=TARGET, body="Message content",
                            created_at="2026-10-01T00:00:00Z", conversation_id=LISTING)
        self.listing = dict(id=LISTING, user_id=TARGET, name="Book", description="Listing content",
                            price=12, is_sold=False, updated_at="2026-10-01T00:00:00Z")
        self.auth_account = dict(id=TARGET, banned_until=None)
        self.admin_account = dict(id=ADMIN, banned_until=None)
        self.patches = []
        self.events = []
        self.event_failure = None
        self.event_failure_on_call = None
        self.event_calls = 0
        self.history_failure = None
        self.ban_failure = None
        self.bans = []
        self.queries = []
        self.page_rows = None
        self.race = False
        self.failure = None
        self.logged_out = []
        self.initialize_listing_storage()

    def initialize_listing_storage(self):
        self.url = "https://project.supabase.co"
        self.images = []
        self.deletes = []
        self.storage_deletes = []
        self.storage_failures = 0
        self.delete_conflict = False
        self.delete_blocked = False
        self.listing_reports = {LISTING: [REPORT], "unrelated-listing": ["unrelated-report"]}

    def configured(self):
        return self.available

    def sign_in(self, email, password):
        if email != "admin@example.test" or password != "correct":
            raise PortalError("Unable to sign in with these credentials.", 401)
        return dict(access_token="verified-token", expires_in=3600)

    def current_user(self, token):
        if self.failure:
            raise self.failure
        if token != "verified-token":
            raise PortalError("Your session expired.", 401)
        return dict(id=ADMIN)

    def one(self, table, row_id, columns):
        if table == "profiles":
            return deepcopy(self.profiles.get(row_id))
        if table == "reports":
            return deepcopy(self.report) if self.report and row_id == self.report["id"] else None
        if table == "messages":
            return deepcopy(self.message)
        if table == "listings":
            return deepcopy(self.listing)
        raise AssertionError(table)

    def rows(self, table, **query):
        self.queries.append((table, query))
        if table == "listing_images":
            return deepcopy(self.images)
        if table == "moderation_events":
            if self.history_failure:
                raise self.history_failure
            rows = [event for event in self.events if event["report_id"] == query.get("report_id", "").removeprefix("eq.")]
            return deepcopy(rows)
        if table == "profiles":
            profile_ids = query.get("id", "").removeprefix("in.(").removesuffix(")").split(",")
            return deepcopy([self.profiles[user_id] for user_id in profile_ids if user_id in self.profiles])
        return deepcopy(self.page_rows if self.page_rows is not None else [self.report])

    def request(self, method, path, **kwargs):
        if method == "POST" and path == "/rest/v1/moderation_events":
            self.event_calls += 1
            if self.event_failure:
                raise self.event_failure
            if self.event_failure_on_call == self.event_calls:
                raise PortalError("Audit storage unavailable", 502)
            event = dict(id="event-" + str(len(self.events) + 1), **kwargs["data"],
                         created_at="2026-10-31T12:00:00Z")
            self.events.append(event)
            return [deepcopy(event)]
        if method == "DELETE":
            return self.delete_listing_data(path, kwargs)
        assert method == "PATCH" and path == "/rest/v1/reports"
        # Match the deployed reports.status CHECK constraint, independently of the UI.
        if kwargs["data"]["status"] not in ("open", "in_review", "resolved", "dismissed"):
            raise PortalError("Report status rejected by the database constraint.", 422)
        self.patches.append(deepcopy(kwargs))
        if self.race:
            return []
        self.report.update(kwargs["data"])
        return [deepcopy(self.report)]

    def delete_listing_data(self, path, kwargs):
        if path == "/storage/v1/object/listing_imgs":
            self.storage_deletes.append(deepcopy(kwargs["data"]))
            if self.storage_failures:
                self.storage_failures -= 1
                raise PortalError("Storage deletion unavailable", 502)
            return []
        assert path == "/rest/v1/listings"
        self.deletes.append(deepcopy(kwargs["query"]))
        if self.delete_blocked:
            raise PortalError("Related data prevents deletion.", 409)
        if self.delete_conflict:
            return []
        self.listing = None
        self.report = None
        self.images = []
        self.listing_reports.pop(LISTING, None)
        return [{"id": LISTING}]

    def auth_user(self, user_id):
        return deepcopy(self.admin_account if user_id == ADMIN else self.auth_account)

    def set_ban(self, user_id, duration):
        if self.ban_failure:
            raise self.ban_failure
        if self.failure:
            raise self.failure
        self.bans.append((user_id, duration))
        return dict(id=user_id, banned_until="2026-11-01T00:00:00Z")

    def sign_out(self, token):
        self.logged_out.append(token)
