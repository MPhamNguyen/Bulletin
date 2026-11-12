begin;

drop policy if exists "interest owners receive listing broadcasts" on realtime.messages;
create policy "interest owners receive listing broadcasts"
on realtime.messages
for select
to authenticated
using (
    realtime.messages.extension = 'broadcast'
    and realtime.topic() = 'marketplace-interests:' || (select auth.uid())::text
);

create or replace function public.broadcast_marketplace_interest_listing()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    recipient record;
begin
    if new.is_sold then
        return new;
    end if;

    for recipient in
        select distinct interest.user_id
        from public.marketplace_interests as interest
        where interest.user_id::text <> new.user_id::text
          and interest.category = new.category
          and (interest.condition is null or interest.condition = new.condition)
          and (
              interest.minimum_price_cents is null
              or interest.minimum_price_cents <= round(new.price * 100)::bigint
          )
          and (
              interest.maximum_price_cents is null
              or interest.maximum_price_cents >= round(new.price * 100)::bigint
          )
    loop
        perform realtime.send(
            jsonb_build_object('listing_id', new.id),
            'listing-inserted',
            'marketplace-interests:' || recipient.user_id::text,
            true
        );
    end loop;

    return new;
end;
$$;

revoke execute on function public.broadcast_marketplace_interest_listing() from public;

drop trigger if exists broadcast_marketplace_interest_listing_insert on public.listings;
create trigger broadcast_marketplace_interest_listing_insert
after insert on public.listings
for each row
execute function public.broadcast_marketplace_interest_listing();

commit;
