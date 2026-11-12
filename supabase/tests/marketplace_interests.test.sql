-- Marketplace interests: category validation, replacement and account isolation.
-- Run only against a disposable database containing the Bulletin schema and migration.
begin;

insert into auth.users(id, email) values
  ('00000000-0000-0000-0000-0000000000c3', 'interests-owner@example.edu'),
  ('00000000-0000-0000-0000-0000000000d4', 'interests-other@example.edu');

set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000c3', true);
select public.replace_marketplace_interests('[
  {"category":"TEXTBOOKS","attribute_filters":{"edition":{"type":"integer_range","min":2,"max":2}}},
  {"category":"ELECTRONICS","condition":"GOOD","minimum_price_cents":10000,"attribute_filters":{}},
  {"category":"TEXTBOOKS","attribute_filters":{"edition":{"type":"integer_range","min":2,"max":2}}}
]'::jsonb);
do $$ begin
  assert (select count(*) from public.marketplace_interests) = 2,
    'replacement must store distinct categories';
end $$;

select public.replace_marketplace_interests('[{"category":"FURNITURE","attribute_filters":{}}]'::jsonb);
do $$ begin
  assert (select array_agg(category order by category) from public.marketplace_interests) = array['FURNITURE'],
    'replacement must remove categories that are no longer selected';
end $$;

select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000d4', true);
do $$ begin
  assert (select count(*) from public.marketplace_interests) = 0,
    'another user must not read the owners interests';
end $$;
select public.replace_marketplace_interests('[{"category":"CLOTHING","attribute_filters":{}}]'::jsonb);
do $$ begin
  assert (select array_agg(category) from public.marketplace_interests) = array['CLOTHING'],
    'replacement must affect only the authenticated user';
end $$;

reset role;
set local role anon;
do $$ begin
  assert (select count(*) from public.marketplace_interests) = 0,
    'anonymous users must not read interests';
  begin
    perform public.replace_marketplace_interests('[{"category":"HOUSING","attribute_filters":{}}]'::jsonb);
    raise exception 'anonymous function execution unexpectedly succeeded';
  exception when insufficient_privilege then
    null;
  end;
end $$;

reset role;
rollback;
\echo marketplace interest SQL tests passed
