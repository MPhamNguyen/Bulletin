drop policy "Listing owners can upload listing images" on storage.objects;
drop policy "Listing owners can delete listing images" on storage.objects;

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
