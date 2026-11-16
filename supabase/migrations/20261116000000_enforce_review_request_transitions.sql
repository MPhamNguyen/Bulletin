-- Enforce the review-request status transition table in the database.
--
-- Source of truth: ReviewRequestTransitions (shared/.../marketplace/domain/model). Keep this trigger aligned:
--   PENDING  -> ACCEPTED | DECLINED | SUBMITTED | CANCELLED   (PENDING -> SUBMITTED is intentional)
--   ACCEPTED -> SUBMITTED | CANCELLED
--   DECLINED, SUBMITTED, CANCELLED are terminal.
-- Updates that leave status unchanged (other columns) are allowed. Repeating a buyer status is handled by the
-- client as an idempotent no-op and never reaches this trigger because its conditional UPDATE matches no row.
create or replace function public.enforce_listing_review_request_transition()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
  if new.status::text is not distinct from old.status::text then
    return new;
  end if;

  if not (
    (old.status::text = 'PENDING' and new.status::text in ('ACCEPTED', 'DECLINED', 'SUBMITTED', 'CANCELLED'))
    or (old.status::text = 'ACCEPTED' and new.status::text in ('SUBMITTED', 'CANCELLED'))
  ) then
    raise exception 'Illegal review request transition from % to %', old.status, new.status
      using errcode = '23514';
  end if;

  return new;
end;
$$;

drop trigger if exists enforce_listing_review_request_transition on public.listing_review_requests;
create trigger enforce_listing_review_request_transition
  before update of status on public.listing_review_requests
  for each row execute function public.enforce_listing_review_request_transition();
