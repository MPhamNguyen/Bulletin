begin;

create table if not exists public.marketplace_interests (
    user_id uuid not null references auth.users(id) on delete cascade,
    category text not null constraint marketplace_interests_category_check
        check (category in ('TEXTBOOKS', 'ELECTRONICS', 'FURNITURE', 'CLOTHING', 'HOUSING', 'OTHER')),
    created_at timestamptz not null default now(),
    primary key (user_id, category)
);

alter table public.marketplace_interests enable row level security;

revoke all on table public.marketplace_interests from anon, authenticated;
grant select, insert, delete on table public.marketplace_interests to authenticated;

drop policy if exists "Users can read their marketplace interests" on public.marketplace_interests;
create policy "Users can read their marketplace interests"
on public.marketplace_interests for select to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = user_id);

drop policy if exists "Users can add their marketplace interests" on public.marketplace_interests;
create policy "Users can add their marketplace interests"
on public.marketplace_interests for insert to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = user_id);

drop policy if exists "Users can remove their marketplace interests" on public.marketplace_interests;
create policy "Users can remove their marketplace interests"
on public.marketplace_interests for delete to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = user_id);

create or replace function public.replace_marketplace_interests(p_categories text[])
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    if exists (
        select 1
        from unnest(coalesce(p_categories, '{}'::text[])) as selected(category)
        where selected.category not in (
            'TEXTBOOKS', 'ELECTRONICS', 'FURNITURE', 'CLOTHING', 'HOUSING', 'OTHER'
        )
    ) then
        raise exception 'Invalid marketplace category';
    end if;

    delete from public.marketplace_interests where user_id = auth.uid();

    insert into public.marketplace_interests (user_id, category)
    select auth.uid(), selected.category
    from (
        select distinct unnest(coalesce(p_categories, '{}'::text[])) as category
    ) as selected;
end;
$$;

revoke execute on function public.replace_marketplace_interests(text[]) from public;
revoke execute on function public.replace_marketplace_interests(text[]) from anon;
grant execute on function public.replace_marketplace_interests(text[]) to authenticated;

commit;
