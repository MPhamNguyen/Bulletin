-- Run AFTER migration, function deployment, secret setup and a successful preview.
-- Enable pg_cron (Cron) and pg_net in the Dashboard first.
-- Vault must contain account_deletion_project_url and account_deletion_cron_secret.
-- Re-running replaces this job. Default remains preview until explicitly enabled below.
do $$
begin
  if not exists(select 1 from vault.decrypted_secrets where name='account_deletion_project_url')
    or not exists(select 1 from vault.decrypted_secrets where name='account_deletion_cron_secret') then
    raise exception 'Create the two account deletion Vault secrets first';
  end if;
  perform cron.unschedule(jobid) from cron.job where jobname='bulletin-account-purge';
end $$;

select cron.schedule('bulletin-account-purge', '0 * * * *', $cron$
  select net.http_post(
    url := (select decrypted_secret from vault.decrypted_secrets
            where name='account_deletion_project_url') || '/functions/v1/purge-deleted-accounts',
    headers := jsonb_build_object('Content-Type','application/json',
      'x-deletion-secret',(select decrypted_secret from vault.decrypted_secrets
                          where name='account_deletion_cron_secret')),
    body := '{"dry_run":true}'::jsonb,
    timeout_milliseconds := 110000
  );
$cron$);

-- ACTIVATE: after preview review, uncomment and run ONLY this statement.
-- update cron.job set command=replace(command,'"dry_run":true','"dry_run":false')
-- where jobname='bulletin-account-purge';

-- PAUSE (in-flight cleanup may already have deleted files; do not unfreeze jobs):
-- select cron.unschedule('bulletin-account-purge');
