create table public.listing_images (
    id uuid primary key default gen_random_uuid(),
    listing_id uuid not null references public.listings(id) on delete cascade,
    object_path text not null unique,
    position integer not null,
    created_at timestamptz not null default now(),
    constraint listing_images_position_range check (position between 0 and 4),
    constraint listing_images_path_matches_listing check (
        split_part(object_path, '/', 1) = listing_id::text
        and split_part(object_path, '/', 2) <> ''
        and strpos(split_part(object_path, '/', 2), '/') = 0
    ),
    constraint listing_images_unique_position unique (listing_id, position)
);

alter table public.listing_images enable row level security;
grant select, insert, update, delete on public.listing_images to authenticated;

create policy "Authenticated users can read listing images"
    on public.listing_images
    for select
    to authenticated
    using (true);

create policy "Listing owners can add listing images"
    on public.listing_images
    for insert
    to authenticated
    with check (
        exists (
            select 1
            from public.listings
            where listings.id = listing_images.listing_id
              and listings.user_id = (select auth.uid())
        )
    );

create policy "Listing owners can reorder listing images"
    on public.listing_images
    for update
    to authenticated
    using (
        exists (
            select 1
            from public.listings
            where listings.id = listing_images.listing_id
              and listings.user_id = (select auth.uid())
        )
    )
    with check (
        exists (
            select 1
            from public.listings
            where listings.id = listing_images.listing_id
              and listings.user_id = (select auth.uid())
        )
    );

create policy "Listing owners can remove listing images"
    on public.listing_images
    for delete
    to authenticated
    using (
        exists (
            select 1
            from public.listings
            where listings.id = listing_images.listing_id
              and listings.user_id = (select auth.uid())
        )
    );

update storage.buckets
set public = true,
    file_size_limit = 5242880,
    allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']
where id = 'listing_imgs';

create policy "Listing owners can upload listing images"
    on storage.objects
    for insert
    to authenticated
    with check (
        bucket_id = 'listing_imgs'
        and exists (
            select 1
            from public.listings
            where listings.id::text = (storage.foldername(storage.objects.name))[1]
              and listings.user_id = (select auth.uid())
        )
    );

create policy "Listing owners can delete listing images"
    on storage.objects
    for delete
    to authenticated
    using (
        bucket_id = 'listing_imgs'
        and exists (
            select 1
            from public.listings
            where listings.id::text = (storage.foldername(storage.objects.name))[1]
              and listings.user_id = (select auth.uid())
        )
    );
