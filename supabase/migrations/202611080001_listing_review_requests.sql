create table if not exists public.listing_review_requests (
    listing_id uuid not null references public.listings(id) on delete cascade,
    buyer_id uuid not null references public.profiles(id) on delete cascade,
    seller_id uuid not null references public.profiles(id) on delete cascade,
    listing_title text not null,
    status text not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'DECLINED', 'SUBMITTED', 'CANCELLED')),
    created_at timestamptz not null default now(),
    primary key (listing_id, buyer_id),
    check (buyer_id <> seller_id)
);

alter table public.listing_review_requests enable row level security;
grant select on public.listing_review_requests to authenticated;
grant insert on public.listing_review_requests to authenticated;
grant update (status) on public.listing_review_requests to authenticated;

create policy "review request participants can read"
    on public.listing_review_requests for select to authenticated
    using (auth.uid() = buyer_id or auth.uid() = seller_id);
create policy "seller creates request for sold listing buyer"
    on public.listing_review_requests for insert to authenticated
    with check (
        auth.uid() = seller_id
        and exists (
            select 1 from public.listings l
            where l.id = listing_id and l.user_id = seller_id
              and l.buyer_id = buyer_id and l.is_sold = true
        )
    );
create policy "buyer changes own request status"
    on public.listing_review_requests for update to authenticated
    using (auth.uid() = buyer_id)
    with check (
        auth.uid() = buyer_id
        and status in ('ACCEPTED', 'DECLINED', 'SUBMITTED')
    );
create policy "seller cancels request after restoring listing"
    on public.listing_review_requests for update to authenticated
    using (auth.uid() = seller_id)
    with check (
        auth.uid() = seller_id
        and status = 'CANCELLED'
        and exists (
            select 1 from public.listings l
            where l.id = listing_review_requests.listing_id
              and l.user_id = seller_id
              and l.is_sold = false
              and l.buyer_id is null
        )
    );

create or replace function public.create_listing_review_request()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    should_create boolean;
begin
    if tg_op = 'INSERT' then
        should_create := true;
    else
        should_create := not old.is_sold or old.buyer_id is distinct from new.buyer_id;
        if old.buyer_id is not null and (old.is_sold and not new.is_sold or old.buyer_id is distinct from new.buyer_id) then
            update public.listing_review_requests
            set status = 'CANCELLED'
            where listing_id = old.id
              and buyer_id = old.buyer_id
              and status in ('PENDING', 'ACCEPTED');
        end if;
    end if;
    if new.is_sold and new.buyer_id is not null and should_create then
        insert into public.listing_review_requests(listing_id, buyer_id, seller_id, listing_title)
        values (new.id, new.buyer_id, new.user_id, new.name)
        on conflict (listing_id, buyer_id) do update
            set status = 'PENDING', seller_id = excluded.seller_id, listing_title = excluded.listing_title
            where listing_review_requests.status = 'CANCELLED';
    end if;
    return new;
end;
$$;

insert into public.listing_review_requests(listing_id, buyer_id, seller_id, listing_title)
select id, buyer_id, user_id, name
from public.listings
where is_sold = true and buyer_id is not null and buyer_id <> user_id
on conflict (listing_id, buyer_id) do nothing;

create or replace function public.enforce_listing_review_request_transition()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    if (old.status = 'PENDING' and new.status not in ('ACCEPTED', 'DECLINED', 'SUBMITTED', 'CANCELLED'))
       or (old.status = 'ACCEPTED' and new.status not in ('SUBMITTED', 'CANCELLED'))
       or (old.status = 'CANCELLED' and new.status <> 'PENDING')
       or old.status in ('DECLINED', 'SUBMITTED') then
        raise exception 'invalid review request status transition';
    end if;
    return new;
end;
$$;
create trigger listing_review_request_status_transition
    before update of status on public.listing_review_requests
    for each row execute function public.enforce_listing_review_request_transition();

drop trigger if exists listing_review_request_after_sold on public.listings;
create trigger listing_review_request_after_sold
    after insert or update of is_sold, buyer_id on public.listings
    for each row execute function public.create_listing_review_request();
