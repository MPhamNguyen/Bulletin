"""Server-only Supabase REST adapter. Never expose backend errors or keys."""

import json
import logging
import re
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen


class PortalError(Exception):
    def __init__(self, message, status=400):
        super().__init__(message)
        self.status = status


def database_error_details(error):
    """Extract only a validated error code and a known constraint name."""
    try:
        body = json.loads(error.read(65536))
        if not isinstance(body, dict):
            return None, False
        code = body.get("code")
        if not isinstance(code, str) or not re.fullmatch(r"[A-Z0-9]{5}|PGRST[0-9]{3}", code):
            return None, False
        status_constraint = code == "23514" and '"reports_status_check"' in str(body.get("message", ""))
        return code, status_constraint
    except (ValueError, OSError):
        return None, False
    finally:
        error.close()


def reject_backend_request(error, method, path, access_token, public, missing_ok):
    code, status_constraint = database_error_details(error)
    if missing_ok and error.code == 404:
        return None
    logging.getLogger(__name__).warning(
        "Supabase request rejected: method=%s path=%s http_status=%s code=%s",
        method, path, error.code, code or "unknown",
    )
    if error.code == 429:
        raise PortalError("Too many requests. Please wait and try again.", 429) from None
    if access_token and error.code in (401, 403):
        raise PortalError("Your session expired. Please sign in again.", 401) from None
    if public and error.code in (400, 401, 403, 422):
        raise PortalError("Unable to sign in with these credentials.", 401) from None
    if method == "PATCH" and path == "/rest/v1/reports" and status_constraint:
        raise PortalError("This status is not allowed by the database's reports_status_check rule (23514).", 422) from None
    if method == "DELETE" and path == "/rest/v1/listings" and code == "23503":
        raise PortalError("Related data prevents this listing from being deleted. The listing and reports were retained.", 409) from None
    details = "HTTP " + str(error.code) + (", code " + code if code else "")
    raise PortalError("Supabase rejected the request (" + details + "). No success was confirmed.", 502) from None


class Supabase:
    def __init__(self, url, anon_key, service_key):
        self.url = url.rstrip("/")
        self.anon_key = anon_key
        self.service_key = service_key

    def configured(self):
        parsed = urlsplit(self.url)
        return bool(
            parsed.scheme == "https" and parsed.netloc and not parsed.username
            and not parsed.query and not parsed.fragment
            and self.anon_key and self.service_key
        )

    def request(self, method, path, *, query=None, data=None, access_token=None,
                public=False, missing_ok=False):
        key = self.anon_key if public or access_token else self.service_key
        headers = {"apikey": key, "Accept": "application/json"}
        if access_token or not public:
            headers["Authorization"] = "Bearer " + (access_token or self.service_key)
        if method in ("PATCH", "DELETE") and path.startswith("/rest/v1/"):
            headers["Prefer"] = "return=representation"
        body = None
        if data is not None:
            headers["Content-Type"] = "application/json"
            body = json.dumps(data).encode("utf-8")
        url = self.url + path + ("?" + urlencode(query) if query else "")
        request = Request(url, data=body, headers=headers, method=method)
        try:
            with urlopen(request, timeout=8) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except HTTPError as error:
            return reject_backend_request(error, method, path, access_token, public, missing_ok)
        except (URLError, TimeoutError, OSError, ValueError):
            raise PortalError(
                "Unable to confirm the request. Refresh before retrying an action.", 502
            ) from None

    def rows(self, table, **query):
        result = self.request("GET", "/rest/v1/" + table, query=query)
        if not isinstance(result, list) or any(not isinstance(row, dict) for row in result):
            raise PortalError("Supabase returned an unexpected response. Please try again.", 502)
        return result

    def one(self, table, row_id, columns):
        rows = self.rows(table, id="eq." + row_id, select=columns, limit=1)
        return rows[0] if rows else None

    def sign_in(self, email, password):
        return self.request(
            "POST", "/auth/v1/token", query={"grant_type": "password"},
            data={"email": email, "password": password}, public=True,
        )

    def current_user(self, token):
        return self.request("GET", "/auth/v1/user", access_token=token)

    def auth_user(self, user_id):
        return self.request("GET", "/auth/v1/admin/users/" + user_id, missing_ok=True)

    def set_ban(self, user_id, duration):
        return self.request(
            "PUT", "/auth/v1/admin/users/" + user_id, data={"ban_duration": duration}
        )

    def sign_out(self, token):
        self.request(
            "POST", "/auth/v1/logout", query={"scope": "local"}, access_token=token
        )
