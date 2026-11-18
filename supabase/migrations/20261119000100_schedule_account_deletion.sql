-- Incremental migration: requires the deployed moderation + message-retention migrations.
-- Does not enable Cron or perform any deletion when installed.
begin;
do $$
begin
  if to_regprocedure('private.lock_conversation_lifecycle()') is null
     or to_regprocedure('private.account_deletion_report_has_target(public.reports)') is null then
    raise exception 'Install the moderation/message-retention migrations first';
  end if;
end $$;

create table private.account_purge_jobs (
  user_id uuid primary key, -- Intentionally no FK: survives Auth deletion and HTTP retries.
  requested_at timestamptz not null,
  started_at timestamptz not null default clock_timestamp(),
  completed_at timestamptz,
  listing_ids uuid[] not null,
  lease_token uuid not null,
  lease_until timestamptz not null,
  retry_after timestamptz not null default now(),
  attempts integer not null default 1,
  last_error text
);
create table private.account_purge_objects (
  user_id uuid references private.account_purge_jobs on delete cascade,
  bucket_id text not null,
  object_name text not null,
  primary key (user_id, bucket_id, object_name)
);
alter table private.account_purge_jobs enable row level security;
alter table private.account_purge_objects enable row level security;
revoke all on private.account_purge_jobs, private.account_purge_objects
  from public, anon, authenticated, service_role;
create index profiles_pending_purge on public.profiles(deleted_at, id)
  where deleted_at is not null;

create function private.account_purge_held(p_user uuid) returns boolean
language sql stable security definer set search_path = '' set row_security = off as $$
  select exists (select 1 from public.reports r
    where r.status in ('open', 'in_review') and
      (private.account_deletion_report_subject(r) = p_user
       or not private.account_deletion_report_has_target(r)));
$$;

create function private.account_purge_finalizing(p_user uuid) returns boolean
language sql stable security definer set search_path = '' set row_security = off as $$
  select exists (select 1 from private.account_purge_jobs j
    where j.user_id = p_user and j.completed_at is null);
$$;

-- Read metadata only. Actual file removal always goes through the Storage API.
create function private.account_purge_inventory(p_user uuid, p_listings uuid[])
returns table(bucket_id text, object_name text)
language sql stable security definer set search_path = '' set row_security = off as $$
  select o.bucket_id, o.name from storage.objects o
  where coalesce(to_jsonb(o)->>'owner_id', to_jsonb(o)->>'owner') = p_user::text
    or (o.bucket_id = 'pfp' and split_part(o.name, '/', 1) = p_user::text)
    or (o.bucket_id = 'listing_imgs' and (
      split_part(o.name, '/', 1) = any(array(select x::text from unnest(p_listings) x))
      or exists (select 1 from public.listing_images i
                 where i.listing_id = any(p_listings) and i.object_path = o.name)));
$$;

create function private.account_purge_profile_guard() returns trigger
language plpgsql security definer set search_path = '' set row_security = off as $$
begin
  perform private.lock_conversation_lifecycle();
  if tg_op = 'UPDATE' and private.account_purge_finalizing(old.id) then
    raise exception 'Account cleanup has started; restoration and profile changes are closed'
      using errcode = '23514';
  end if;
  if tg_op = 'INSERT' then
    if new.deleted_at is not null then new.deleted_at := clock_timestamp(); end if;
  elsif old.deleted_at is null and new.deleted_at is not null then
    new.deleted_at := clock_timestamp(); -- Never trust the device clock.
  elsif old.deleted_at is not null and new.deleted_at is not null then
    new.deleted_at := old.deleted_at;
  end if;
  return new;
end $$;
create trigger bulletin_purge_profile_guard before insert or update on public.profiles
  for each row execute function private.account_purge_profile_guard();

create function private.account_purge_content_guard() returns trigger
language plpgsql security definer set search_path = '' set row_security = off as $$
declare v_owner uuid; v_previous uuid;
begin
  perform private.lock_conversation_lifecycle();
  if tg_table_name = 'listings' then
    if tg_op <> 'DELETE' then v_owner := new.user_id; end if;
    if tg_op <> 'INSERT' then v_previous := old.user_id; end if;
  else
    if tg_op <> 'DELETE' then
      select user_id into v_owner from public.listings where id = new.listing_id;
    end if;
    if tg_op <> 'INSERT' then
      select user_id into v_previous from public.listings where id = old.listing_id;
    end if;
  end if;
  if (private.account_purge_finalizing(v_owner) and exists(select 1 from auth.users where id=v_owner))
    or (private.account_purge_finalizing(v_previous) and exists(select 1 from auth.users where id=v_previous)) then
    raise exception 'Account cleanup has started; listing changes are closed' using errcode='23514';
  end if;
  if tg_op = 'DELETE' then return old; end if;
  return new;
end $$;
create trigger bulletin_purge_listing_guard before insert or update or delete on public.listings
  for each row execute function private.account_purge_content_guard();
create trigger bulletin_purge_image_guard before insert or update or delete on public.listing_images
  for each row execute function private.account_purge_content_guard();
create trigger bulletin_purge_image_write_gate before insert or update or delete on public.listing_images
  for each statement execute function private.account_deletion_write_gate();

create function private.account_purge_report_guard() returns trigger
language plpgsql security definer set search_path = '' set row_security = off as $$
begin
  perform private.lock_conversation_lifecycle();
  if new.status in ('open','in_review')
    and private.account_purge_finalizing(private.account_deletion_report_subject(new)) then
    raise exception 'Account cleanup has started; this target cannot accept active reports'
      using errcode='23514';
  end if;
  return new;
end $$;
create trigger bulletin_purge_report_guard before insert or update on public.reports
  for each row execute function private.account_purge_report_guard();

-- Restrictive policies compose with existing Storage ownership policies.
-- VOLATILE + shared lock serializes client metadata writes with job creation.
create function private.account_purge_storage_writable(p_bucket text, p_name text, p_owner text)
returns boolean language plpgsql volatile security definer set search_path = '' set row_security = off as $$
begin
  perform private.lock_conversation_lifecycle();
  return not private.account_purge_finalizing(auth.uid()) and not exists (
    select 1 from private.account_purge_jobs j where j.completed_at is null and (
      j.user_id::text = p_owner
      or (p_bucket = 'pfp' and split_part(p_name,'/',1) = j.user_id::text)
      or (p_bucket = 'listing_imgs' and split_part(p_name,'/',1) = any(
        array(select x::text from unnest(j.listing_ids) x)))));
end $$;
create policy bulletin_purge_storage_insert on storage.objects as restrictive
  for insert to authenticated with check
  (private.account_purge_storage_writable(bucket_id,name,coalesce(to_jsonb(objects)->>'owner_id',to_jsonb(objects)->>'owner')));
create policy bulletin_purge_storage_update on storage.objects as restrictive
  for update to authenticated using
  (private.account_purge_storage_writable(bucket_id,name,coalesce(to_jsonb(objects)->>'owner_id',to_jsonb(objects)->>'owner')))
  with check (private.account_purge_storage_writable(bucket_id,name,coalesce(to_jsonb(objects)->>'owner_id',to_jsonb(objects)->>'owner')));
create policy bulletin_purge_storage_delete on storage.objects as restrictive
  for delete to authenticated using
  (private.account_purge_storage_writable(bucket_id,name,coalesce(to_jsonb(objects)->>'owner_id',to_jsonb(objects)->>'owner')));

-- A late trusted-service upload must also prevent Auth deletion.
create function private.account_purge_auth_guard() returns trigger
language plpgsql security definer set search_path = '' set row_security = off as $$
declare j private.account_purge_jobs;
begin
  perform private.lock_conversation_lifecycle();
  select * into j from private.account_purge_jobs where user_id=old.id and completed_at is null;
  if found and (exists(select 1 from private.account_purge_objects where user_id=old.id)
    or exists(select 1 from private.account_purge_inventory(old.id,j.listing_ids))) then
    raise exception 'Storage cleanup is incomplete' using errcode='23514';
  end if;
  return old;
end $$;
create trigger bulletin_purge_auth_guard before delete on auth.users
  for each row execute function private.account_purge_auth_guard();

create function public.account_purge_preview() returns jsonb
language sql stable security definer set search_path = '' set row_security = off as $$
  select jsonb_build_object(
    'eligible', count(*) filter(where not private.account_purge_held(p.id)),
    'held', count(*) filter(where private.account_purge_held(p.id)),
    'finalizing', (select count(*) from private.account_purge_jobs where completed_at is null))
  from public.profiles p where deleted_at <= now() - interval '30 days'
    and not private.account_purge_finalizing(p.id);
$$;

create function public.account_purge_claim() returns jsonb
language plpgsql security definer set search_path = '' set row_security = off as $$
declare j private.account_purge_jobs; v_user uuid;
begin
  perform private.lock_conversation_lifecycle();
  select * into j from private.account_purge_jobs
    where completed_at is null and lease_until < now() and retry_after <= now()
    order by started_at limit 1 for update;
  if found then
    update private.account_purge_jobs set lease_token=gen_random_uuid(),
      lease_until=now()+interval '15 minutes',attempts=attempts+1,last_error=null
      where user_id=j.user_id returning * into j;
  else
    select p.id into v_user from public.profiles p
      where p.deleted_at <= now()-interval '30 days'
        and not exists(select 1 from private.account_purge_jobs x where x.user_id=p.id)
        and not private.account_purge_held(p.id)
      order by p.deleted_at,p.id limit 1;
    if v_user is null then return null; end if;
    insert into private.account_purge_jobs(user_id,requested_at,listing_ids,lease_token,lease_until)
      select p.id,p.deleted_at,array(select id from public.listings where user_id=p.id),
        gen_random_uuid(),now()+interval '15 minutes' from public.profiles p where p.id=v_user
      returning * into j;
  end if;
  insert into private.account_purge_objects
    select j.user_id,i.* from private.account_purge_inventory(j.user_id,j.listing_ids) i
    on conflict do nothing;
  return jsonb_build_object('user_id',j.user_id,'token',j.lease_token);
end $$;

-- Each step is bounded. A successful Storage response alone never acknowledges files:
-- refresh checks metadata, catches partial removals, and permits safe retries.
create function public.account_purge_step(p_user uuid,p_token uuid,p_action text)
returns jsonb language plpgsql security definer set search_path = '' set row_security = off as $$
declare j private.account_purge_jobs; result jsonb;
begin
  perform private.lock_conversation_lifecycle();
  select * into j from private.account_purge_jobs where user_id=p_user for update;
  if not found or j.completed_at is not null or j.lease_token <> p_token or j.lease_until <= now() then
    raise exception 'Invalid or expired cleanup lease' using errcode='42501';
  end if;
  if p_action = 'retry' then
    update private.account_purge_jobs set lease_until=now(),retry_after=now()+interval '1 hour',
      last_error='Worker failed; inspect Edge Function status and backend constraints'
      where user_id=p_user;
    return '{}'::jsonb;
  elsif p_action = 'complete' then
    if exists(select 1 from auth.users where id=p_user) then
      raise exception 'Auth account still exists';
    end if;
    delete from private.account_purge_objects where user_id=p_user;
    update private.account_purge_jobs set completed_at=now(),last_error=null where user_id=p_user;
    return '{}'::jsonb;
  elsif p_action <> 'files' then
    raise exception 'Invalid cleanup action';
  end if;
  -- Drop acknowledged/missing objects, then inventory again for late uploads.
  delete from private.account_purge_objects x where x.user_id=p_user and not exists (
    select 1 from storage.objects o where o.bucket_id=x.bucket_id and o.name=x.object_name);
  insert into private.account_purge_objects
    select p_user,i.* from private.account_purge_inventory(p_user,j.listing_ids) i on conflict do nothing;
  select coalesce(jsonb_agg(to_jsonb(x)),'[]') into result from (
    select bucket_id,object_name from private.account_purge_objects where user_id=p_user
    order by bucket_id,object_name limit 100) x;
  return result;
end $$;

-- Default EXECUTE is PUBLIC, including functions in private schemas: revoke explicitly.
revoke all on function private.account_purge_held(uuid),private.account_purge_finalizing(uuid),
  private.account_purge_inventory(uuid,uuid[]),private.account_purge_profile_guard(),
  private.account_purge_content_guard(),private.account_purge_report_guard(),
  private.account_purge_auth_guard(),private.account_purge_storage_writable(text,text,text)
  from public,anon,authenticated,service_role;
grant execute on function private.account_purge_storage_writable(text,text,text) to authenticated;
revoke all on function public.account_purge_preview(),public.account_purge_claim(),
  public.account_purge_step(uuid,uuid,text) from public,anon,authenticated;
grant execute on function public.account_purge_preview(),public.account_purge_claim(),
  public.account_purge_step(uuid,uuid,text) to service_role;
commit;
