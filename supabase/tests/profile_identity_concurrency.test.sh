#!/usr/bin/env bash
# DISPOSABLE database only: commits fixture rows, then deletes them on exit.
# DATABASE_URL=<disposable superuser URL> supabase/tests/profile_identity_concurrency.test.sh
set -euo pipefail
: "${DATABASE_URL:?set DATABASE_URL to a disposable database}"
psql_() { PGOPTIONS='-c statement_timeout=15000' psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -X -q -At "$@"; }
UID_A='00000000-0000-0000-0000-0000000000d1'
cleanup() { psql_ -c "delete from auth.users where id = '$UID_A'" >/dev/null || true; }
background_pid=''
finish() {
  if [ -n "$background_pid" ]; then
    kill "$background_pid" 2>/dev/null || true
    wait "$background_pid" 2>/dev/null || true
  fi
  cleanup
}
trap finish EXIT
reset_fixture() {
  cleanup
  psql_ -c "insert into auth.users(id, email, email_confirmed_at, raw_user_meta_data)
            values ('$UID_A', 'race-old@example.edu', now(), '{\"full_name\":\"Racer\"}')"
  psql_ -c "delete from public.profiles where id = '$UID_A'"
}
create_as_owner() { # mode, supplied email, transaction hold duration
  local create_sql='select public.ensure_my_profile();'
  if [ "$1" = 'direct' ]; then
    create_sql="insert into public.profiles(id,email,full_name,is_verified) values ('$UID_A','$2','Racer',false);"
  fi
  psql_ <<SQL >/dev/null
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"$UID_A","role":"authenticated"}', true);
select set_config('request.jwt.claim.sub', '$UID_A', true);
$create_sql
reset role;
select pg_sleep($3);
commit;
SQL
}
change_auth_email() {
  psql_ <<SQL >/dev/null
begin;
update auth.users set email = 'race-new@example.edu' where id = '$UID_A';
select pg_sleep($1);
commit;
SQL
}
assert_profile_email() {
  local got
  got=$(psql_ -c "select email from public.profiles where id = '$UID_A'")
  [ "$got" = 'race-new@example.edu' ] || { echo "FAIL ($1): unexpected profile email" >&2; exit 1; }
  echo "ok: $1"
}
# Observe the writer after its operation, while it holds the transaction open. Avoid assuming
# that a background psql connection has reached its lock after a fixed startup delay.
wait_for_hold() {
  local attempt
  for attempt in {1..100}; do
    if [ "$(psql_ -c "select count(*) from pg_stat_activity where application_name = '$1' and wait_event = 'PgSleep'")" = '1' ]; then
      return
    fi
    sleep 0.05
  done
  echo "FAIL: writer did not reach transaction hold" >&2
  exit 1
}
for mode in rpc direct; do
  reset_fixture
  PGAPPNAME=profile_identity_auth_writer change_auth_email 3 & background_pid=$!
  wait_for_hold profile_identity_auth_writer
  create_as_owner "$mode" race-new@example.edu 0
  wait "$background_pid"
  background_pid=''
  assert_profile_email "$mode: auth change commits before profile creation"

  reset_fixture
  PGAPPNAME=profile_identity_profile_writer create_as_owner "$mode" race-old@example.edu 3 & background_pid=$!
  wait_for_hold profile_identity_profile_writer
  change_auth_email 0
  wait "$background_pid"
  background_pid=''
  assert_profile_email "$mode: profile creation commits before auth change"
done
