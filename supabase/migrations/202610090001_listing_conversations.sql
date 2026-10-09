-- Listing-scoped conversations are unique per listing and unordered pair of users.
-- The RPC is the database-side idempotency boundary for rapid repeated sends and races.
alter table public.conversations
    add column if not exists listing_id uuid;

drop function if exists public.get_or_create_listing_conversation(uuid, uuid, text);
drop function if exists public.get_or_create_listing_conversation(uuid, uuid, uuid);

create or replace function public.get_or_create_listing_conversation(
    p_requester_id uuid,
    p_seller_id uuid,
    p_listing_id uuid
)
returns table(conversation_id uuid)
language plpgsql
security definer
set search_path = public
as $$
declare
    existing_id uuid;
begin
    if auth.uid() is null or auth.uid() <> p_requester_id then
        raise exception 'authenticated requester mismatch' using errcode = '42501';
    end if;
    if p_requester_id = p_seller_id or p_listing_id is null then
        raise exception 'invalid listing conversation' using errcode = '22023';
    end if;

    -- Serialize the lookup/insert for this exact listing and unordered user pair.
    perform pg_advisory_xact_lock(
        hashtextextended(
            p_listing_id::text || ':' || least(p_requester_id::text, p_seller_id::text) || ':' ||
            greatest(p_requester_id::text, p_seller_id::text),
            0
        )
    );

    select c.id into existing_id
    from public.conversations c
    where c.listing_id = p_listing_id
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_requester_id)
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_seller_id)
    limit 1;

    if existing_id is null then
        insert into public.conversations (listing_id) values (p_listing_id) returning id into existing_id;
        insert into public.conversation_participants (conversation_id, user_id, joined_at)
        values (existing_id, p_requester_id, now()), (existing_id, p_seller_id, now());
    end if;

    return query select existing_id;
exception when unique_violation then
    select c.id into existing_id from public.conversations c
    where c.listing_id = p_listing_id
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_requester_id)
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_seller_id)
    limit 1;
    return query select existing_id;
end;
$$;

revoke all on function public.get_or_create_listing_conversation(uuid, uuid, uuid) from public;
grant execute on function public.get_or_create_listing_conversation(uuid, uuid, uuid) to authenticated;
