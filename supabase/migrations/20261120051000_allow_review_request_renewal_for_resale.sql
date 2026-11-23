-- A resale to the same buyer reissues the cancelled request from the trusted
-- listing trigger. Buyer-driven updates still cannot leave a terminal state.
begin;

create or replace function public.enforce_listing_review_request_transition()
returns trigger
language plpgsql
set search_path to public, pg_temp
as $function$
begin
  if new.status::text is not distinct from old.status::text then
    return new;
  end if;

  -- Only a nested listing-sale trigger may reissue a cancelled request. The
  -- listing must already describe this exact sale; request identities cannot change.
  if old.status::text = 'CANCELLED' and new.status::text = 'PENDING'
     and pg_trigger_depth() > 1
     and new.listing_id is not distinct from old.listing_id
     and new.buyer_id is not distinct from old.buyer_id
     and new.seller_id is not distinct from old.seller_id
     and exists (
       select 1 from public.listings l
       where l.id = new.listing_id
         and l.user_id = new.seller_id
         and l.buyer_id = new.buyer_id
         and l.status = 'SOLD'
         and l.is_sold = true
     ) then
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
$function$;

commit;
