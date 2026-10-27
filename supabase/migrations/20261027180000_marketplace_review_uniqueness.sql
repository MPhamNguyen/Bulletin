-- Marketplace reviews are unique per listing. Profile reviews, which have no
-- listing_id, remain unique per reviewer and reviewee.
ALTER TABLE public.reviews
    DROP CONSTRAINT IF EXISTS unique_reviewer_reviewee;

ALTER TABLE public.reviews
    DROP CONSTRAINT IF EXISTS reviews_comment_check;

ALTER TABLE public.reviews
    ADD CONSTRAINT reviews_comment_check
    CHECK (listing_id IS NOT NULL OR length(trim(comment)) > 0);

CREATE UNIQUE INDEX IF NOT EXISTS unique_reviewer_reviewee_profile
    ON public.reviews (reviewer_id, reviewee_id)
    WHERE listing_id IS NULL;
