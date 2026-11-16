-- Review-request transitions: every (from, to) pair must match the domain table in ReviewRequestTransitions.
-- Run only against a disposable database containing the Bulletin schema and migrations.
-- Assumes listing_review_requests accepts rows with just listing_id, buyer_id, seller_id, listing_title, status.
begin;

do $$
declare
  statuses text[] := array['PENDING', 'ACCEPTED', 'DECLINED', 'SUBMITTED', 'CANCELLED'];
  legal text[] := array[
    'PENDING>ACCEPTED', 'PENDING>DECLINED', 'PENDING>SUBMITTED', 'PENDING>CANCELLED',
    'ACCEPTED>SUBMITTED', 'ACCEPTED>CANCELLED'
  ];
  from_status text;
  to_status text;
  allowed boolean;
begin
  foreach from_status in array statuses loop
    foreach to_status in array statuses loop
      continue when from_status = to_status;
      -- Seed without foreign-key checks, then restore triggers before the update under test.
      set local session_replication_role = replica;
      delete from public.listing_review_requests
        where listing_id = '00000000-0000-0000-0000-0000000000a1';
      execute format(
        'insert into public.listing_review_requests(listing_id, buyer_id, seller_id, listing_title, status) '
        || 'values (%L, %L, %L, %L, %L)',
        '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000b1',
        '00000000-0000-0000-0000-0000000000c1', 'Textbook', from_status
      );
      set local session_replication_role = origin;

      allowed := (from_status || '>' || to_status) = any (legal);
      begin
        execute format(
          'update public.listing_review_requests set status = %L where listing_id = %L',
          to_status, '00000000-0000-0000-0000-0000000000a1'
        );
        assert allowed, format('%s -> %s must be rejected', from_status, to_status);
      exception when check_violation then
        assert not allowed, format('%s -> %s must be allowed', from_status, to_status);
      end;
    end loop;
  end loop;
end $$;

rollback;
