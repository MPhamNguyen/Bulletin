import unittest

from portal.backend import PortalError
from portal.listing_review_state import listing_delete_filters, listing_snapshot


class ListingReviewStateTests(unittest.TestCase):
    def listing(self):
        return dict(id="listing", user_id="seller", name="Book", description="Description",
                    price=12.5, is_sold=False, edited_at=None)

    def test_text_filters_quote_reserved_characters_and_preserve_newlines(self):
        listing = self.listing()
        listing["description"] = 'A "book", with (notes) and \\ marks\nNext line'
        filters = listing_delete_filters(listing)
        self.assertEqual('eq."A \\"book\\", with (notes) and \\\\ marks\nNext line"', filters["description"])
        self.assertEqual("eq.12.5", filters["price"])
        self.assertEqual("eq.false", filters["is_sold"])

    def test_null_is_supported_but_absent_evidence_is_rejected(self):
        listing = self.listing()
        self.assertEqual("is.null", listing_delete_filters(listing)["edited_at"])
        listing.pop("edited_at")
        with self.assertRaises(PortalError) as caught:
            listing_delete_filters(listing)
        self.assertEqual(502, caught.exception.status)

    def test_snapshot_tracks_reviewed_content_and_edit_time(self):
        listing = self.listing()
        before = listing_snapshot(listing)
        listing["description"] = "Changed directly"
        self.assertNotEqual(before, listing_snapshot(listing))
        self.assertNotIn("updated_at", before)
