-- Enforce Bulletin's university-email signup policy at the authentication boundary.
-- Configure this function as the Authentication > Hooks > Before User Created hook.
create or replace function public.enforce_edu_email_signup(event jsonb)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
    signup_email text := lower(trim(event->'user'->>'email'));
begin
    if signup_email is null
        or signup_email !~ '^[^@[:space:]]+@[a-z0-9-]+([.][a-z0-9-]+)*[.]edu$'
    then
        return jsonb_build_object(
            'error', jsonb_build_object(
                'http_code', 403,
                'message', 'Bulletin requires a valid .edu university email.'
            )
        );
    end if;

    return '{}'::jsonb;
end;
$$;

grant usage on schema public to supabase_auth_admin;
grant execute on function public.enforce_edu_email_signup(jsonb) to supabase_auth_admin;
revoke execute on function public.enforce_edu_email_signup(jsonb) from authenticated, anon, public;
