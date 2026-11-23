-- Opt-in: disposable database with the deployed baseline and sold-conversation migration.
-- Run with psql -v ON_ERROR_STOP=1; all fixtures and role changes roll back.
begin;
set local statement_timeout = '30s';

do $$
declare
    seller uuid := gen_random_uuid();
    buyer uuid := gen_random_uuid();
    listing uuid := gen_random_uuid();
    thread uuid := gen_random_uuid();
    new_thread uuid := gen_random_uuid();
    outgoing uuid := gen_random_uuid();
    actor uuid;
begin
    insert into auth.users(id, email)
    values (seller, seller || '@test.example.edu'), (buyer, buyer || '@test.example.edu');
    insert into public.listings(id, user_id, name, description, category, condition, price, status)
    values (listing, seller, 'Test textbook', 'Fixture', 'TEXTBOOKS', 'NEW', 10, 'AVAILABLE');
    insert into public.conversations(id, listing_id, listing_is_sold) values (thread, listing, true);
    if (select listing_is_sold from public.conversations where id = thread) then
        raise exception 'Conversation accepted a forged sold snapshot';
    end if;
    insert into public.conversation_participants(conversation_id, user_id)
    values (thread, seller), (thread, buyer);

    perform set_config('request.jwt.claim.sub', buyer::text, true);
    perform set_config('request.jwt.claim.role', 'authenticated', true);
    execute 'set local role authenticated';
    insert into public.messages(id, conversation_id, body) values (outgoing, thread, 'Before sale');
    execute 'reset role';

    update public.listings set status = 'SOLD', buyer_id = buyer where id = listing;
    if not (select listing_is_sold from public.conversations where id = thread) then
        raise exception 'Sale did not update existing conversations';
    end if;
    update public.conversations set listing_is_sold = false where id = thread;
    if not (select listing_is_sold from public.conversations where id = thread) then
        raise exception 'Conversation accepted a forged unlocked snapshot';
    end if;
    insert into public.conversations(id, listing_id) values (new_thread, listing);
    if not (select listing_is_sold from public.conversations where id = new_thread) then
        raise exception 'New conversation did not inherit sold state';
    end if;

    foreach actor in array array[buyer, seller] loop
        perform set_config('request.jwt.claim.sub', actor::text, true);
        execute 'set local role authenticated';
        if (select count(*) from public.messages where conversation_id = thread) <> 1 then
            raise exception 'Sale removed access to message history';
        end if;
        begin
            insert into public.messages(conversation_id, body) values (thread, 'Must fail');
            raise exception 'Direct insert was allowed after sale';
        exception when sqlstate 'BM006' then null;
        end;
        perform public.mark_conversation_read(thread);
        execute 'reset role';
    end loop;

    perform set_config('request.jwt.claim.sub', buyer::text, true);
    execute 'set local role authenticated';
    begin
        perform public.edit_message(thread, outgoing, 'Must fail', null);
        raise exception 'Body edit was allowed after sale';
    exception when sqlstate 'BM006' then null;
    end;
    execute 'reset role';
    if (select body from public.messages where id = outgoing) <> 'Before sale' then
        raise exception 'Rejected edit changed the message';
    end if;

    update public.listings set status = 'AVAILABLE', buyer_id = null where id = listing;
    if exists(select 1 from public.conversations where listing_id = listing and listing_is_sold) then
        raise exception 'Restoration did not unlock all conversations';
    end if;
    perform set_config('request.jwt.claim.sub', buyer::text, true);
    execute 'set local role authenticated';
    insert into public.messages(conversation_id, body) values (thread, 'After restoration');
    perform public.edit_message(thread, outgoing, 'Edited after restoration', null);
    execute 'reset role';
end;
$$;

rollback;
