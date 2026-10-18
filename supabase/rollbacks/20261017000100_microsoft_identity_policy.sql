-- Disable Azure sign-in before rollback. Baseline schema is retained.
BEGIN;
DROP TRIGGER IF EXISTS tr_enforce_microsoft_identity ON auth.identities;
DROP TRIGGER IF EXISTS tr_enforce_auth_email_change ON auth.users;
DROP FUNCTION IF EXISTS public.enforce_microsoft_identity();
DROP FUNCTION IF EXISTS public.enforce_auth_email_change();

CREATE OR REPLACE FUNCTION "public"."enforce_signup_rules"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
BEGIN
    IF NOT public.is_valid_edu_email(NEW.email) THEN
        RAISE EXCEPTION 'Bulletin requires a valid .edu university email.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.profiles p
        WHERE public.normalize_email(p.email) = public.normalize_email(NEW.email)
          AND p.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'An account with this email address already exists.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION "public"."enforce_edu_email_signup"("event" "jsonb") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO ''
    AS $_$
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
$_$;

CREATE OR REPLACE FUNCTION "public"."handle_new_user"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public', 'pg_temp'
    AS $$
begin
  insert into public.profiles (id, email, full_name, university, is_verified)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'full_name', 'Student'),
    coalesce(new.raw_user_meta_data->>'university', 'CSU Long Beach'),
    new.email_confirmed_at is not null
  )
  on conflict (id) do update set
    is_verified = (new.email_confirmed_at is not null);
  return new;
end;
$$;
COMMIT;
