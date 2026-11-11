begin;

alter table public.marketplace_interests
    add column if not exists condition text,
    add column if not exists minimum_price_cents bigint,
    add column if not exists maximum_price_cents bigint,
    add column if not exists attribute_filters jsonb not null default '{}'::jsonb;

alter table public.marketplace_interests
    drop constraint if exists marketplace_interests_condition_check,
    add constraint marketplace_interests_condition_check
        check (condition is null or condition in ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR')),
    drop constraint if exists marketplace_interests_price_check,
    add constraint marketplace_interests_price_check
        check (
            (minimum_price_cents is null or minimum_price_cents >= 0) and
            (maximum_price_cents is null or maximum_price_cents >= 0) and
            (minimum_price_cents is null or maximum_price_cents is null or
                minimum_price_cents <= maximum_price_cents)
        ),
    drop constraint if exists marketplace_interests_attribute_filters_check,
    add constraint marketplace_interests_attribute_filters_check
        check (
            jsonb_typeof(attribute_filters) = 'object' and
            octet_length(attribute_filters::text) <= 16384
        ),
    drop constraint if exists marketplace_interests_housing_condition_check,
    add constraint marketplace_interests_housing_condition_check
        check (category <> 'HOUSING' or condition is null);

drop function if exists public.replace_marketplace_interests(text[]);

create or replace function public.replace_marketplace_interests(p_interests jsonb)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;
    if jsonb_typeof(coalesce(p_interests, '[]'::jsonb)) <> 'array' then
        raise exception 'Marketplace interests must be a JSON array';
    end if;
    if exists (
        select 1
        from jsonb_array_elements(coalesce(p_interests, '[]'::jsonb)) as item
        where jsonb_typeof(item) <> 'object'
            or item->>'category' not in (
                'TEXTBOOKS', 'ELECTRONICS', 'FURNITURE', 'CLOTHING', 'HOUSING', 'OTHER'
            )
            or coalesce(jsonb_typeof(item->'attribute_filters'), 'object') <> 'object'
    ) then
        raise exception 'Invalid marketplace interest';
    end if;

    delete from public.marketplace_interests where user_id = auth.uid();

    insert into public.marketplace_interests (
        user_id,
        category,
        condition,
        minimum_price_cents,
        maximum_price_cents,
        attribute_filters
    )
    select distinct on (item->>'category')
        auth.uid(),
        item->>'category',
        nullif(item->>'condition', ''),
        (item->>'minimum_price_cents')::bigint,
        (item->>'maximum_price_cents')::bigint,
        coalesce(item->'attribute_filters', '{}'::jsonb)
    from jsonb_array_elements(coalesce(p_interests, '[]'::jsonb)) as item
    order by item->>'category';
end;
$$;

revoke execute on function public.replace_marketplace_interests(jsonb) from public;
revoke execute on function public.replace_marketplace_interests(jsonb) from anon;
grant execute on function public.replace_marketplace_interests(jsonb) to authenticated;

commit;
