-- Older clients omitted buyer_id from restore PATCHes when its value was null.
-- Repair only listings already restored to AVAILABLE, leaving every sold listing untouched.
-- The existing create_listing_review_request trigger cancels actionable requests
-- for the old buyer when buyer_id is cleared; terminal requests remain unchanged.
begin;
set local lock_timeout = '5s';
set local statement_timeout = '30s';

update public.listings
set buyer_id = null
where status = 'AVAILABLE'
  and is_sold = false
  and buyer_id is not null;

commit;
