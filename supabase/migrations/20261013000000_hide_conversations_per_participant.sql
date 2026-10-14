-- BULLETIN-93: inbox visibility belongs to a participant, never to the shared conversation.
alter table public.conversation_participants
    add column if not exists deleted_at timestamptz;

create index if not exists conversation_participants_visible_inbox_idx
    on public.conversation_participants (user_id, conversation_id)
    where deleted_at is null;

-- The client has no table UPDATE permission for this action. These functions change only
-- the authenticated user's membership row and return false for unknown/nonmember IDs.
create or replace function public.hide_conversation(p_conversation_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    changed_count integer;
begin
    if auth.uid() is null then
        return false;
    end if;
    update public.conversation_participants
       set deleted_at = coalesce(deleted_at, now())
     where conversation_id = p_conversation_id
       and user_id = auth.uid();
    get diagnostics changed_count = row_count;
    return changed_count = 1;
end;
$$;

create or replace function public.restore_conversation(p_conversation_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    changed_count integer;
begin
    if auth.uid() is null then
        return false;
    end if;
    update public.conversation_participants
       set deleted_at = null
     where conversation_id = p_conversation_id
       and user_id = auth.uid();
    get diagnostics changed_count = row_count;
    return changed_count = 1;
end;
$$;

revoke all on function public.hide_conversation(uuid) from public, anon;
revoke all on function public.restore_conversation(uuid) from public, anon;
grant execute on function public.hide_conversation(uuid) to authenticated;
grant execute on function public.restore_conversation(uuid) to authenticated;
