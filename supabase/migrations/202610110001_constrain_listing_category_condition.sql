begin;

update public.listings
set category = coalesce(nullif(upper(trim(category)), ''), 'OTHER'),
    condition = coalesce(
        nullif(replace(upper(trim(condition)), ' ', '_'), ''),
        'GOOD'
    );

update public.listings
set category = case
        when category in ('TEXTBOOKS', 'ELECTRONICS', 'FURNITURE', 'CLOTHING', 'HOUSING', 'OTHER') then category
        else 'OTHER'
    end,
    condition = case
        when condition in ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR') then condition
        else 'GOOD'
    end;

alter table public.listings
    drop constraint if exists listings_category_allowed,
    drop constraint if exists listings_condition_allowed;

alter table public.listings
    alter column category set default 'OTHER',
    alter column category set not null,
    alter column condition set default 'GOOD',
    alter column condition set not null,
    add constraint listings_category_allowed
        check (category in ('TEXTBOOKS', 'ELECTRONICS', 'FURNITURE', 'CLOTHING', 'HOUSING', 'OTHER')),
    add constraint listings_condition_allowed
        check (condition in ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR'));

create index if not exists listings_category_idx on public.listings (category);
create index if not exists listings_condition_idx on public.listings (condition);

commit;
