alter table public.listings
    add column if not exists updated_at timestamptz;

update public.listings
set updated_at = created_at
where updated_at is null;

alter table public.listings
    alter column updated_at set default now(),
    alter column updated_at set not null;

create or replace function public.set_listing_updated_at()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'INSERT' then
        new.updated_at := new.created_at;
    else
        new.updated_at := now();
    end if;
    return new;
end;
$$;

drop trigger if exists set_listing_updated_at on public.listings;

create trigger set_listing_updated_at
    before insert or update on public.listings
    for each row
    execute function public.set_listing_updated_at();
