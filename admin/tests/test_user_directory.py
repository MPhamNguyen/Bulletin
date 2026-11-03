import json
import unittest
from unittest.mock import patch

from fakes import ADMIN, TARGET, LISTING, FakeSupabase
from portal.backend import PortalError
from portal.user_directory import PUBLIC_PROFILE_COLUMNS, profile_avatar, search_profiles
from portal.web import ACCESS_COOKIE, create_app


class UserDirectoryTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeSupabase()
        self.backend.profiles[TARGET].update(university="Campus University", major="Computing",
                                             bio="Hello campus", is_verified=True)
        self.profile_rows = [self.backend.profiles[TARGET]]
        self.listing_rows = [dict(self.backend.listing, listing_images=[{
            "object_path": LISTING + "/photo one.jpg", "position": 0,
        }])]
        self.queries = []
        self.backend.rows = self.rows
        self.app = create_app({"TESTING": True, "SECRET_KEY": "test-only-secret-" * 4}, self.backend)
        self.client = self.app.test_client()
        self.client.set_cookie(ACCESS_COOKIE, "verified-token")
        self.path = "/directory/" + TARGET

    def rows(self, table, **query):
        self.queries.append((table, query))
        return self.profile_rows if table == "profiles" else self.listing_rows

    def test_home_links_to_all_user_search(self):
        html = self.client.get("/").get_data(as_text=True)
        self.assertIn('href="/directory"', html)
        self.assertIn("Search All Users", html)

    def test_search_results_link_to_public_profile(self):
        response = self.client.get("/directory?q=Target")
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        self.assertIn('href="' + self.path + '"', html)
        self.assertIn("Campus University", html)
        table, query = self.queries[-1]
        self.assertEqual("profiles", table)
        self.assertEqual(PUBLIC_PROFILE_COLUMNS, query["select"])
        self.assertEqual('(full_name.ilike."%Target%",email.ilike."%Target%")', query["or"])
        self.assertNotIn("is_admin", query)
        self.assertNotIn("deleted_at", query)

    def test_user_id_search_includes_exact_id_match(self):
        self.client.get("/directory?q=" + TARGET)
        self.assertIn("id.eq." + TARGET, self.queries[-1][1]["or"])

    def test_search_quotes_postgrest_filter_characters(self):
        term = 'name),id.not.is.null,"quoted"'
        search_profiles(self.backend, term, 1)
        query = self.queries[-1][1]
        quoted = json.dumps("%" + term + "%")
        self.assertEqual("(full_name.ilike." + quoted + ",email.ilike." + quoted + ")", query["or"])

    def test_empty_search_browses_all_profiles_with_pagination(self):
        self.profile_rows *= 26
        response = self.client.get("/directory?page=2")
        html = response.get_data(as_text=True)
        self.assertIn("Previous users", html)
        self.assertIn("Next users", html)
        query = self.queries[-1][1]
        self.assertEqual(25, query["offset"])
        self.assertEqual(26, query["limit"])
        self.assertNotIn("or", query)

    def test_profile_displays_public_information_and_owned_listing_photos(self):
        response = self.client.get(self.path)
        self.assertEqual(200, response.status_code)
        html = response.get_data(as_text=True)
        for text in ("Campus University", "Computing", "Hello campus", "Verified", "Book",
                     "Listing content", LISTING + "/photo%20one.jpg"):
            self.assertIn(text, html)
        self.assertNotIn("target@example.test", html)
        self.assertNotIn("admin@example.test", html)
        table, query = self.queries[-1]
        self.assertEqual("listings", table)
        self.assertEqual("eq." + TARGET, query["user_id"])
        self.assertNotIn("is_sold", query)
        self.assertIn("listing_images(object_path,position)", query["select"])

    def test_sold_listings_and_pagination_are_supported(self):
        self.listing_rows[0]["is_sold"] = True
        self.listing_rows *= 26
        response = self.client.get(self.path + "?page=2")
        html = response.get_data(as_text=True)
        self.assertIn("Sold", html)
        self.assertIn("Previous listings", html)
        self.assertIn("Next listings", html)
        self.assertEqual(25, self.queries[-1][1]["offset"])

    def test_missing_profile_and_empty_results(self):
        self.profile_rows = []
        self.assertIn("No users found", self.client.get("/directory").get_data(as_text=True))
        self.listing_rows = []
        self.assertIn("This user has no listings", self.client.get(self.path).get_data(as_text=True))
        del self.backend.profiles[TARGET]
        self.assertEqual(404, self.client.get(self.path).status_code)

    def test_invalid_images_do_not_hide_listing_details(self):
        self.listing_rows[0]["listing_images"][0]["object_path"] = "another-user/photo.jpg"
        html = self.client.get(self.path).get_data(as_text=True)
        self.assertIn("Listing photos could not be loaded", html)
        self.assertIn("Listing content", html)
        self.assertNotIn("another-user/photo.jpg", html)

    def test_profile_and_listing_text_are_escaped(self):
        self.backend.profiles[TARGET]["bio"] = "<script>bad</script>"
        self.listing_rows[0]["description"] = "<script>bad</script>"
        html = self.client.get(self.path).get_data(as_text=True)
        self.assertEqual(2, html.count("&lt;script&gt;bad&lt;/script&gt;"))
        self.assertNotIn("<script>bad", html)

    def test_avatar_only_uses_the_configured_public_profile_bucket(self):
        good = self.backend.url + "/storage/v1/object/public/pfp/" + TARGET + "/avatar"
        self.assertEqual(good, profile_avatar(self.backend, {"avatar_url": good}))
        for value in (None, "javascript:alert(1)", "https://other.example/photo.png",
                      self.backend.url + "/storage/v1/object/public/listing_imgs/photo.png"):
            self.assertIsNone(profile_avatar(self.backend, {"avatar_url": value}))

    def test_invalid_page_and_search_are_rejected(self):
        for path in ("/directory?page=0", self.path + "?page=-1", "/directory?page=abc",
                     "/directory?q=" + "a" * 321, "/directory?q=%00"):
            self.assertEqual(400, self.client.get(path).status_code)
        self.assertFalse(self.queries)

    def test_directory_is_admin_only(self):
        self.backend.profiles[ADMIN]["is_admin"] = False
        for path in ("/directory", self.path):
            self.assertEqual(403, self.client.get(path).status_code)
        self.backend.profiles[ADMIN]["is_admin"] = True
        self.backend.admin_account["banned_until"] = "9999-01-01T00:00:00Z"
        self.assertEqual(403, self.client.get("/directory").status_code)
        self.client.delete_cookie(ACCESS_COOKIE)
        self.assertEqual("/login", self.client.get(self.path).location)
        self.assertFalse(self.queries)

    def test_backend_failure_does_not_look_like_empty_results(self):
        with patch.object(self.backend, "rows", side_effect=PortalError("Unavailable", 502)):
            self.assertEqual(502, self.client.get("/directory").status_code)
            response = self.client.get(self.path)
        html = response.get_data(as_text=True)
        self.assertEqual(200, response.status_code)
        self.assertIn("Listings could not be loaded", html)
        self.assertIn("Campus University", html)
        self.assertNotIn("This user has no listings", html)


if __name__ == "__main__":
    unittest.main()
