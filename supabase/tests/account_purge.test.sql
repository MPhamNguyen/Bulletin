-- DISPOSABLE database only: requires deployed baseline + new worker migration.
-- Entire test rolls back; never use production as a test database.
begin;
do $$
declare u uuid := gen_random_uuid(); reviewer uuid := gen_random_uuid(); j jsonb; f jsonb; n integer;
begin
  insert into auth.users(id,email) values (u,u||'@test.example.edu'),(reviewer,reviewer||'@test.example.edu');
  update public.profiles set deleted_at='2000-01-01' where id=u;
  if (select deleted_at < now()-interval '1 minute' from public.profiles where id=u) then
    raise exception 'Device backdating was accepted';
  end if;
  -- Simulate elapsed time ONLY in a disposable fixture, bypassing timestamp trigger.
  alter table public.profiles disable trigger bulletin_purge_profile_guard;
  update public.profiles set deleted_at=now()-interval '31 days' where id=u;
  alter table public.profiles enable trigger bulletin_purge_profile_guard;
  insert into public.reports(reporter_id,reported_user_id,type,reason)
    values(reviewer,u,'user','Spam');
  if not private.account_purge_held(u) or private.account_purge_held(reviewer) then
    raise exception 'Moderation hold confused reporter and subject';
  end if;
  if public.account_purge_claim() is not null then raise exception 'Claim ignored active report'; end if;
  update public.reports set status='dismissed' where reported_user_id=u;
  insert into storage.objects(bucket_id,name,owner_id) values('pfp',u||'/avatar',u::text);
  perform public.account_purge_preview();
  if exists(select 1 from private.account_purge_jobs where user_id=u) then raise exception 'Preview mutated state'; end if;
  j := public.account_purge_claim();
  if j->>'user_id' <> u::text then raise exception 'Run this test in a database with no other eligible accounts'; end if;
  if private.account_purge_storage_writable('pfp',u||'/new-avatar',u::text) then
    raise exception 'Finalizing uploads were allowed';
  end if;
  -- Exercise restrictive Storage policy as the real client role, not just owner bypass.
  execute 'grant usage on schema storage to authenticated';
  execute 'grant insert on storage.objects to authenticated';
  execute 'create policy purge_test_allow_insert on storage.objects for insert to authenticated with check (true)';
  perform set_config('request.jwt.claim.sub',u::text,true);
  perform set_config('request.jwt.claim.role','authenticated',true);
  execute 'set local role authenticated';
  begin
    insert into storage.objects(bucket_id,name,owner_id) values('pfp',u||'/blocked',u::text);
    raise exception 'Storage RLS allowed a finalizing upload';
  exception when insufficient_privilege then null; end;
  execute 'reset role';
  begin
    update public.profiles set deleted_at=null where id=u;
    raise exception 'Restore unexpectedly succeeded';
  exception when check_violation then null; end;
  begin
    update public.reports set status='open' where reported_user_id=u;
    raise exception 'Reopen unexpectedly succeeded';
  exception when check_violation then null; end;
  f := public.account_purge_step(u,(j->>'token')::uuid,'files');
  if jsonb_array_length(f) <> 1 then raise exception 'Missing Storage inventory'; end if;
  begin
    delete from auth.users where id=u;
    raise exception 'Auth deleted before Storage';
  exception when check_violation then null; end;
  -- Metadata-only fixture simulation; production MUST use the Storage API instead.
  delete from storage.objects where owner_id=u::text;
  f := public.account_purge_step(u,(j->>'token')::uuid,'files');
  if jsonb_array_length(f) <> 0 then raise exception 'Inventory did not advance'; end if;
  insert into storage.objects(bucket_id,name,owner_id) values('pfp',u||'/late',u::text);
  begin
    delete from auth.users where id=u;
    raise exception 'Late upload was not detected';
  exception when check_violation then null; end;
  delete from storage.objects where owner_id=u::text;
  delete from auth.users where id=u;
  perform public.account_purge_step(u,(j->>'token')::uuid,'complete');
  if has_function_privilege('authenticated','public.account_purge_claim()','execute')
    or has_function_privilege('anon','public.account_purge_preview()','execute') then
    raise exception 'Worker RPC exposed to client';
  end if;
end $$;
rollback;
