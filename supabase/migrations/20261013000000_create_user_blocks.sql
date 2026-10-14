-- Directed blocks are stored against Supabase Auth identities. The restrictive
-- listings policy below makes the privacy rule hold even if a client is modified.
create table if not exists public.user_blocks (
    blocker_id uuid not null references auth.users(id) on delete cascade,
    blocked_user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (blocker_id, blocked_user_id),
    check (blocker_id <> blocked_user_id)
);

create index if not exists user_blocks_blocked_user_idx
    on public.user_blocks (blocked_user_id);

alter table public.user_blocks enable row level security;

create policy "Users can create their own blocks"
    on public.user_blocks for insert
    with check (auth.uid() = blocker_id);

create policy "Users can read their own block relationships"
    on public.user_blocks for select
    using (auth.uid() = blocker_id or auth.uid() = blocked_user_id);

create policy "Blocked users cannot read each other's listings"
    on public.listings as restrictive for select
    using (
        not exists (
            select 1
            from public.user_blocks
            where (user_blocks.blocker_id = auth.uid() and user_blocks.blocked_user_id = listings.user_id)
               or (user_blocks.blocked_user_id = auth.uid() and user_blocks.blocker_id = listings.user_id)
        )
    );
