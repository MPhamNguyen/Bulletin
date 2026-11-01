-- Listing attributes: server validation, filter columns and RLS.
-- Run against a DISPOSABLE database that has the app schema plus the attributes migration:
--   psql -v ON_ERROR_STOP=1 -d <disposable_db> -f supabase/tests/listing_attributes.test.sql
-- It creates its own fixture rows inside one transaction and rolls everything back. Never run against production.
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

insert into auth.users(id,email) values
  ('00000000-0000-0000-0000-0000000000a1','attributes-owner@example.edu'),
  ('00000000-0000-0000-0000-0000000000b2','attributes-other@example.edu');
insert into public.profiles(id, email, full_name) values
  ('00000000-0000-0000-0000-0000000000a1', 'attributes-owner@example.edu', 'Owner'),
  ('00000000-0000-0000-0000-0000000000b2', 'attributes-other@example.edu', 'Other')
on conflict (id) do update set full_name = excluded.full_name;

-- Valid attributes are stored and typed values survive.
insert into public.listings(id, name, user_id, category, attributes) values
  ('11111111-1111-1111-1111-111111111111', 'Framework laptop', '00000000-0000-0000-0000-0000000000a1', 'ELECTRONICS',
   '{"brand":"Framework","device_type":"laptop","ram_gb":16,"storage_gb":512}');
do $$ begin
  assert (select jsonb_typeof(attributes->'ram_gb') from public.listings where id = '11111111-1111-1111-1111-111111111111') = 'number';
  assert (select attr_brand_key from public.listings where id = '11111111-1111-1111-1111-111111111111') = 'framework';
  assert (select attr_ram_gb from public.listings where id = '11111111-1111-1111-1111-111111111111') = 16;
  assert (select attr_device_type from public.listings where id = '11111111-1111-1111-1111-111111111111') = 'laptop';
end $$;

-- Legacy rows default to {} and have NULL filter columns, so filters exclude them but unfiltered reads include them.
insert into public.listings(id, name, user_id) values
  ('22222222-2222-2222-2222-222222222222', 'Legacy', '00000000-0000-0000-0000-0000000000a1');
do $$ begin
  assert (select attributes from public.listings where id = '22222222-2222-2222-2222-222222222222') = '{}'::jsonb;
  assert (select attr_ram_gb from public.listings where id = '22222222-2222-2222-2222-222222222222') is null;
end $$;

-- Match key normalization mirrors the Kotlin implementations.
do $$ begin
  assert private.listing_text_match_key('  Levi''s ') = 'levis';
  assert private.listing_text_match_key('H&M') = 'h and m';
  assert private.listing_text_match_key('Under-Armour') = 'under armour';
  assert private.listing_text_match_key('Dell') <> private.listing_text_match_key('Dell Inc');
  assert private.listing_text_match_key('Pen Tel') <> private.listing_text_match_key('Pentel');
end $$;

-- Invalid writes are rejected server side.
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x1','00000000-0000-0000-0000-0000000000a1','ELECTRONICS','{"ram_gb":"16"}')$q$, 'invalid value');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x2','00000000-0000-0000-0000-0000000000a1','ELECTRONICS','{"device_type":"monitor","ram_gb":16}')$q$, 'RAM requires');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x3','00000000-0000-0000-0000-0000000000a1','ELECTRONICS','{"device_type":"laptop","ram_gb":0}')$q$, 'invalid value');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x4','00000000-0000-0000-0000-0000000000a1','ELECTRONICS','{"mystery":"x"}')$q$, 'unknown listing attribute');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x5','00000000-0000-0000-0000-0000000000a1','CLOTHING','{"ram_gb":8}')$q$, 'does not apply to category');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x6','00000000-0000-0000-0000-0000000000a1','TEXTBOOKS','{"isbn":"9780306406158"}')$q$, 'invalid value');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x7','00000000-0000-0000-0000-0000000000a1','OTHER','{"a":{"nested":1}}')$q$, 'string, number or boolean');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x8','00000000-0000-0000-0000-0000000000a1','OTHER','[1]')$q$, 'must be a JSON object');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x9','00000000-0000-0000-0000-0000000000a1','HOUSING','{"monthly_rent_usd":1200.505}')$q$, 'invalid value');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x10','00000000-0000-0000-0000-0000000000a1','HOUSING','{"available_date":"2026-02-30"}')$q$, 'invalid value');
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values ('x11','00000000-0000-0000-0000-0000000000a1','OTHER',(select jsonb_object_agg('k'||g, 'v') from generate_series(1,17) g))$q$, 'at most 16');

-- Wrong scalar types must return false rather than SQL NULL bypassing the guard.
select pg_temp.expect_error($q$insert into public.listings(name,user_id,category,attributes) values
 ('wrong choice','00000000-0000-0000-0000-0000000000a1','ELECTRONICS','{"device_type":true}')$q$, 'invalid value');
select pg_temp.expect_error($q$update public.listings set attributes = attributes || '{"device_type":"monitor"}'
 where id = '11111111-1111-1111-1111-111111111111'$q$, 'requires');

-- Valid textbook, housing and furniture values (ISBN text with leading zero preserved, exact decimals).
insert into public.listings(id, name, user_id, category, attributes) values
  ('33333333-3333-3333-3333-333333333333','Book','00000000-0000-0000-0000-0000000000a1','TEXTBOOKS','{"isbn":"0306406152","course_code":"CECS 328","edition":3}'),
  ('44444444-4444-4444-4444-444444444444','Room','00000000-0000-0000-0000-0000000000a1','HOUSING','{"monthly_rent_usd":1250.50,"available_date":"2026-09-01","housing_type":"room"}');
do $$ begin
  assert (select attributes->>'isbn' from public.listings where id='33333333-3333-3333-3333-333333333333') = '0306406152';
  assert (select (attributes->>'monthly_rent_usd')::numeric from public.listings where id='44444444-4444-4444-4444-444444444444') = 1250.50;
end $$;

-- A category-only update (older client) leaves attributes untouched and is not rejected.
update public.listings set category = 'OTHER' where id = '11111111-1111-1111-1111-111111111111';
do $$ begin
  assert (select attributes->>'brand' from public.listings where id = '11111111-1111-1111-1111-111111111111') = 'Framework';
end $$;
update public.listings set category = 'ELECTRONICS' where id = '11111111-1111-1111-1111-111111111111';

-- Unknown values already stored are carried through unchanged but cannot be introduced.
alter table public.listings disable trigger listing_attributes_validate;
update public.listings set attributes = attributes || '{"future_key":{"nested":[1,2]}}' where id = '11111111-1111-1111-1111-111111111111';
alter table public.listings enable trigger listing_attributes_validate;
update public.listings set attributes = attributes || '{"ram_gb":32}' where id = '11111111-1111-1111-1111-111111111111';
do $$ begin
  assert (select attributes->'future_key' from public.listings where id = '11111111-1111-1111-1111-111111111111') = '{"nested":[1,2]}'::jsonb;
end $$;
select pg_temp.expect_error($q$update public.listings set attributes = attributes || '{"another_new":"x"}' where id = '11111111-1111-1111-1111-111111111111'$q$, 'unknown listing attribute');

-- RLS: owner may update attributes; non-owners cannot update and anon cannot read.
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000a1', true);
update public.listings set attributes = attributes || '{"storage_gb":1024}' where id = '11111111-1111-1111-1111-111111111111';
do $$ begin
  assert (select attr_storage_gb from public.listings where id = '11111111-1111-1111-1111-111111111111') = 1024;
end $$;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000b2', true);
do $$
declare n integer;
begin
  update public.listings set attributes = '{"brand":"Hijack"}' where id = '11111111-1111-1111-1111-111111111111';
  get diagnostics n = row_count;
  assert n = 0, 'a non-owner must not update attributes';
  assert (select count(*) from public.listings) >= 4, 'other users can still read listings';
end $$;
reset role;
set local role anon;
do $$ begin
  assert (select count(*) from public.listings) = 0, 'anon must not read listings';
end $$;
reset role;

rollback;
\echo listing attribute SQL tests passed
