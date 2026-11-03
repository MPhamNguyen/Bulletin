-- Auth owns profile identity. Reconcile missing rows only for active accounts, serialized with Auth updates.
begin;

create or replace function public.bulletin_protect_profile_identity()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  auth_email text;
begin
  if coalesce(current_setting('role', true), '') not in ('authenticated', 'anon') then
    return new;
  end if;
  if tg_op = 'UPDATE' then
    if new.id is distinct from old.id then
      raise exception 'profile id cannot be changed' using errcode = '42501';
    end if;
    if new.email is distinct from old.email then
      raise exception 'profile email is managed by authentication' using errcode = '42501';
    end if;
    return new;
  end if;
  -- Direct client inserts must serialize with Auth email changes, just like RPC reconciliation.
  select u.email into auth_email from auth.users u where u.id = new.id for no key update;
  if auth_email is null or lower(btrim(new.email)) is distinct from lower(btrim(auth_email)) then
    raise exception 'profile email must match the authenticated account' using errcode = '42501';
  end if;
  return new;
end $$;
revoke all on function public.bulletin_protect_profile_identity() from public, anon, authenticated;
drop trigger if exists bulletin_protect_profile_identity on public.profiles;
create trigger bulletin_protect_profile_identity before insert or update on public.profiles
  for each row execute function public.bulletin_protect_profile_identity();

create or replace function public.sync_profile_email_from_auth()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.email is not null then
    update public.profiles set email = new.email where id = new.id and email is distinct from new.email;
  end if;
  return null;
end $$;
revoke all on function public.sync_profile_email_from_auth() from public, anon, authenticated;
drop trigger if exists sync_profile_email_from_auth on auth.users;
create trigger sync_profile_email_from_auth after update of email on auth.users
  for each row when (old.email is distinct from new.email) execute function public.sync_profile_email_from_auth();

create or replace function public.ensure_my_profile()
returns void language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  u auth.users%rowtype;
begin
  if uid is null then
    raise exception 'not authenticated' using errcode = '28000';
  end if;
  -- Conflicts with concurrent Auth updates; permits foreign-key checks on the owning row.
  select * into u from auth.users where id = uid for no key update;
  if not found or u.email is null or u.deleted_at is not null then
    raise exception 'authenticated account not found' using errcode = '28000';
  end if;
  -- SECURITY DEFINER bypasses RLS: repeat the hosted active-account ban check explicitly.
  if u.banned_until is not null and u.banned_until > now() then
    raise exception 'account is not active' using errcode = '42501';
  end if;
  insert into public.profiles (id, email, full_name, university, is_verified)
  values (
    u.id, u.email,
    coalesce(nullif(btrim(u.raw_user_meta_data ->> 'full_name'), ''),
             nullif(btrim(u.raw_user_meta_data ->> 'name'), ''), 'Student'),
    case when u.raw_app_meta_data ->> 'provider' = 'azure' then ''
         else coalesce(btrim(u.raw_user_meta_data ->> 'university'), '') end,
    u.email_confirmed_at is not null
  ) on conflict (id) do nothing;
end $$;
revoke all on function public.ensure_my_profile() from public, anon;
grant execute on function public.ensure_my_profile() to authenticated;

revoke truncate, references, trigger on public.profiles from anon, authenticated;
revoke insert, update, delete on public.profiles from anon;
commit;
