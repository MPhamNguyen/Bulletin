-- Sold-listing buyer attribution and one review per sold listing.
-- The explicit constraint names are also used by PostgREST relationship selects.
alter table public.listings
    add column if not exists buyer_id uuid null;

alter table public.listings
    drop constraint if exists listings_user_id_fkey,
    add constraint listings_user_id_fkey
        foreign key (user_id) references public.profiles(id) on delete cascade;

alter table public.listings
    drop constraint if exists listings_buyer_id_fkey,
    add constraint listings_buyer_id_fkey
        foreign key (buyer_id) references public.profiles(id) on delete set null;

alter table public.reviews
    add column if not exists listing_id uuid null;

alter table public.reviews
    drop constraint if exists reviews_listing_id_fkey,
    add constraint reviews_listing_id_fkey
        foreign key (listing_id) references public.listings(id) on delete cascade;

create unique index if not exists reviews_one_per_listing_idx
    on public.reviews (listing_id)
    where listing_id is not null;

alter table public.reviews enable row level security;

drop policy if exists reviews_insert_buyer_only on public.reviews;
create policy reviews_insert_buyer_only on public.reviews
    for insert to authenticated
    with check (
        reviewer_id = auth.uid()
        and exists (
            select 1
            from public.listings
            where listings.id = reviews.listing_id
              and listings.buyer_id = auth.uid()
              and listings.is_sold = true
              and listings.user_id = reviews.reviewee_id
        )
    );
