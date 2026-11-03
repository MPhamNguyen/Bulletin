-- Run only against a DISPOSABLE database with the complete Bulletin schema and identity migration.
-- psql -v ON_ERROR_STOP=1 -d <disposable_db> -f supabase/tests/profile_identity_integrity.test.sql
begin;
create function pg_temp.expect_error(stmt text, fragment text) returns void language plpgsql as $$
begin
  begin
    execute stmt;
  exception when others then
    if sqlerrm not like '%' || fragment || '%' then
      raise exception 'expected "%" but got "%" for: %', fragment, sqlerrm, stmt;
    end if;
    return;
  end;
  raise exception 'statement unexpectedly succeeded: %', stmt;
end $$;
insert into auth.users(id, email, email_confirmed_at, raw_user_meta_data) values
  ('00000000-0000-0000-0000-0000000000c1', 'identity-owner@example.edu', now(), '{"full_name":"Owner","university":"CSULB"}'),
  ('00000000-0000-0000-0000-0000000000c2', 'identity-other@example.edu', now(), '{}');
-- The Auth trigger already creates a profile. Remove it so this tests actual repair, not only a no-op.
delete from public.profiles where id = '00000000-0000-0000-0000-0000000000c1';
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000c1', true);
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000c1","role":"authenticated"}', true);
select public.ensure_my_profile();
select public.ensure_my_profile();
do $$ begin
  assert (select count(*) from public.profiles where id = '00000000-0000-0000-0000-0000000000c1') = 1;
  assert (select email from public.profiles where id = '00000000-0000-0000-0000-0000000000c1') = 'identity-owner@example.edu';
  assert (select is_verified from public.profiles where id = '00000000-0000-0000-0000-0000000000c1');
  assert (select university from public.profiles where id = '00000000-0000-0000-0000-0000000000c1') = 'CSULB';
end $$;
update public.profiles set bio = 'hello' where id = '00000000-0000-0000-0000-0000000000c1';
do $$ begin
  assert (select bio from public.profiles where id = '00000000-0000-0000-0000-0000000000c1') = 'hello';
end $$;
select pg_temp.expect_error(
  $$update public.profiles set email = 'someone-else@example.edu' where id = '00000000-0000-0000-0000-0000000000c1'$$,
  'managed by authentication');
select pg_temp.expect_error(
  $$update public.profiles set id = '00000000-0000-0000-0000-0000000000c2' where id = '00000000-0000-0000-0000-0000000000c1'$$,
  'cannot be changed');
select pg_temp.expect_error(
  $$insert into public.profiles(id, email, full_name) values ('00000000-0000-0000-0000-0000000000c2', 'forged@example.edu', 'Forged')$$,
  'must match the authenticated account');
reset role;
do $$ begin
  assert not has_table_privilege('anon', 'public.profiles', 'INSERT');
  assert not has_table_privilege('anon', 'public.profiles', 'UPDATE');
  assert not has_table_privilege('anon', 'public.profiles', 'DELETE');
  assert not has_table_privilege('anon', 'public.profiles', 'TRUNCATE');
  assert not has_table_privilege('anon', 'public.profiles', 'REFERENCES');
  assert not has_table_privilege('anon', 'public.profiles', 'TRIGGER');
  assert not has_table_privilege('authenticated', 'public.profiles', 'TRUNCATE');
  assert not has_table_privilege('authenticated', 'public.profiles', 'REFERENCES');
  assert not has_table_privilege('authenticated', 'public.profiles', 'TRIGGER');
end $$;
update auth.users set email = 'identity-renamed@example.edu' where id = '00000000-0000-0000-0000-0000000000c1';
do $$ begin
  assert (select email from public.profiles where id = '00000000-0000-0000-0000-0000000000c1') = 'identity-renamed@example.edu';
end $$;
delete from public.profiles where id = '00000000-0000-0000-0000-0000000000c2';
update auth.users set banned_until = now() + interval '1 day' where id = '00000000-0000-0000-0000-0000000000c2';
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000c2', true);
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000c2","role":"authenticated"}', true);
select pg_temp.expect_error($$select public.ensure_my_profile()$$, 'account is not active');
reset role;
do $$ begin
  assert not exists (select 1 from public.profiles where id = '00000000-0000-0000-0000-0000000000c2');
end $$;
set local role anon;
select set_config('request.jwt.claims', '{}', true);
select set_config('request.jwt.claim.sub', '', true);
select pg_temp.expect_error($$select public.ensure_my_profile()$$, 'permission denied');
reset role;
rollback;
\echo profile identity integrity SQL tests passed
