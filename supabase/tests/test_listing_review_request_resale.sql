-- Opt-in database regression: run with `supabase db query --remote=<ref> --file <this file> --yes`.
-- All rows and cloned functions are temporary. Persistent tables/functions are only read.
-- The real deployed function bodies are cloned with their table references redirected
-- to fixtures, so this checks the migration rather than a second implementation.
begin;
set local statement_timeout = '30s';

create temporary table resale_listings (
  id text primary key, user_id text not null, name text not null,
  status text not null, is_sold boolean not null, buyer_id text
) on commit drop;
create temporary table resale_requests (
  listing_id text not null, buyer_id text not null, seller_id text not null,
  listing_title text not null, status text not null default 'PENDING',
  primary key (listing_id, buyer_id)
) on commit drop;

do $clone$
begin
  execute replace(replace(replace(
    pg_get_functiondef('public.create_listing_review_request()'::regprocedure),
    'public.create_listing_review_request()', 'pg_temp.resale_create_request()'),
    'public.listing_review_requests', 'pg_temp.resale_requests'),
    'listing_review_requests.status', 'resale_requests.status');
  execute replace(replace(
    pg_get_functiondef('public.enforce_listing_review_request_transition()'::regprocedure),
    'public.enforce_listing_review_request_transition()', 'pg_temp.resale_request_transition()'),
    'public.listings', 'pg_temp.resale_listings');
end;
$clone$;

create trigger resale_request_guard before update of status on resale_requests
for each row execute function pg_temp.resale_request_transition();
-- Production currently has two guards. Exercise that setup as well.
create trigger resale_request_guard_duplicate before update of status on resale_requests
for each row execute function pg_temp.resale_request_transition();
create trigger resale_listing_sale after insert or update of is_sold, buyer_id on resale_listings
for each row execute function pg_temp.resale_create_request();

-- A nested update unrelated to a valid sale must not bypass the guard.
create temporary table resale_probe (buyer_id text) on commit drop;
create function pg_temp.resale_probe_renewal() returns trigger language plpgsql as $probe$
begin
  update pg_temp.resale_requests set status = 'PENDING' where buyer_id = new.buyer_id;
  return new;
end;
$probe$;
create trigger resale_probe_update after insert on resale_probe
for each row execute function pg_temp.resale_probe_renewal();

do $test$
declare
  terminal_status text;
  rejected boolean;
begin
  insert into resale_listings values ('listing', 'seller', 'Desk', 'SOLD', true, 'buyer');
  assert (select status = 'PENDING' from resale_requests where buyer_id = 'buyer'), 'Initial sale creates request';
  update resale_requests set status = 'ACCEPTED' where buyer_id = 'buyer';
  update resale_listings set status = 'AVAILABLE', is_sold = false, buyer_id = null;
  assert (select status = 'CANCELLED' from resale_requests where buyer_id = 'buyer'), 'Restore cancels request';

  -- This resale reproduces the reported 23514 before the migration.
  update resale_listings set status = 'SOLD', is_sold = true, buyer_id = 'buyer';
  assert (select status = 'PENDING' from resale_requests where buyer_id = 'buyer'), 'Resale renews cancelled request';
  assert (select count(*) = 1 from resale_requests), 'Renewal does not duplicate the request';

  -- Even with the listing sold to this buyer, a direct reset must remain illegal.
  update resale_requests set status = 'CANCELLED' where buyer_id = 'buyer';
  rejected := false;
  begin
    update resale_requests set status = 'PENDING' where buyer_id = 'buyer';
  exception when check_violation then rejected := true;
  end;
  assert rejected, 'Direct CANCELLED to PENDING must be rejected';

  update resale_listings set user_id = 'another-seller';
  rejected := false;
  begin
    insert into resale_probe values ('buyer');
  exception when check_violation then rejected := true;
  end;
  assert rejected, 'Nested renewal must match the current listing seller';
  update resale_listings set user_id = 'seller';

  -- Trigger nesting alone cannot renew a request for a listing that is available.
  update resale_listings set status = 'AVAILABLE', is_sold = false, buyer_id = null;
  rejected := false;
  begin
    insert into resale_probe values ('buyer');
  exception when check_violation then rejected := true;
  end;
  assert rejected, 'Nested renewal must match a current sold listing';

  -- A different buyer receives their own request; submitted/declined requests are preserved.
  foreach terminal_status in array array['SUBMITTED', 'DECLINED'] loop
    update resale_listings set status = 'AVAILABLE', is_sold = false, buyer_id = null;
    update resale_listings set status = 'SOLD', is_sold = true, buyer_id = terminal_status;
    update resale_requests set status = terminal_status where buyer_id = terminal_status;
    update resale_listings set status = 'AVAILABLE', is_sold = false, buyer_id = null;
    update resale_listings set status = 'SOLD', is_sold = true, buyer_id = terminal_status;
    assert (select status = terminal_status from resale_requests where buyer_id = terminal_status),
      'Resale must preserve submitted and declined requests';
    rejected := false;
    begin
      insert into resale_probe values (terminal_status);
    exception when check_violation then rejected := true;
    end;
    assert rejected, 'Even nested updates cannot reopen submitted or declined requests';
    rejected := false;
    begin
      insert into resale_probe values ('buyer');
    exception when check_violation then rejected := true;
    end;
    assert rejected, 'Nested renewal must match the current listing buyer';
  end loop;

  -- Swapping buyers on a sold listing cancels only the previous active request.
  update resale_listings set buyer_id = 'other-buyer';
  update resale_listings set buyer_id = 'last-buyer';
  assert (select status = 'CANCELLED' from resale_requests where buyer_id = 'other-buyer'), 'Old buyer cancelled';
  assert (select status = 'PENDING' from resale_requests where buyer_id = 'last-buyer'), 'New buyer notified';
end;
$test$;

rollback;
