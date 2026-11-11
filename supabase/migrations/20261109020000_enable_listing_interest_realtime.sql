begin;

do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'listings'
    ) then
        alter publication supabase_realtime add table public.listings;
    end if;
end;
$$;

commit;
