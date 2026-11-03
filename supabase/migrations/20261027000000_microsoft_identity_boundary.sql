-- Requires the existing Bulletin schema (profiles, normalize_email, is_valid_edu_email,
-- tr_enforce_signup_rules, on_auth_user_created). No schema snapshot is included.
-- Any tenant is intentional; email is contact data, not a tenant/membership identity.
-- Auth identity writes occur in the OAuth callback transaction, before tokens are issued.
-- No client metadata is used to decide the provider or verified ownership.
DO $$
BEGIN
    IF to_regprocedure('public.is_valid_edu_email(text)') IS NULL
       OR to_regprocedure('public.normalize_email(text)') IS NULL
       OR NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'auth.users'::regclass
           AND tgname = 'tr_enforce_signup_rules' AND tgenabled <> 'D')
       OR NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'auth.users'::regclass
           AND tgname = 'on_auth_user_created' AND tgenabled <> 'D') THEN
        RAISE EXCEPTION 'Reconcile the Bulletin schema and signup/profile triggers before applying SSO policy.';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.enforce_signup_rules() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF NEW.raw_app_meta_data->>'provider' IS DISTINCT FROM 'azure'
       AND NOT public.is_valid_edu_email(NEW.email) THEN
        RAISE EXCEPTION 'Bulletin requires a valid .edu university email.';
    END IF;
    IF NEW.email IS NULL OR btrim(NEW.email) = '' THEN
        RAISE EXCEPTION 'bulletin_missing_email';
    END IF;
    IF EXISTS (SELECT 1 FROM public.profiles p
        WHERE public.normalize_email(p.email) = public.normalize_email(NEW.email)) THEN
        RAISE EXCEPTION 'bulletin_account_conflict';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.enforce_edu_email_signup(event jsonb) RETURNS jsonb
LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
    -- app_metadata is set by Auth; user_metadata is deliberately ignored.
    IF event->'user'->'app_metadata'->>'provider' IS DISTINCT FROM 'azure'
       AND NOT public.is_valid_edu_email(event->'user'->>'email') THEN
        RETURN jsonb_build_object('error', jsonb_build_object('http_code', 403,
            'message', 'Bulletin requires a valid .edu university email.'));
    END IF;
    RETURN '{}'::jsonb;
END;
$$;

CREATE OR REPLACE FUNCTION public.enforce_microsoft_identity() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
    account_email text;
    password_present boolean;
    domain_verified jsonb;
BEGIN
    -- Serialize provider checks on the owning account, including concurrent linking.
    PERFORM 1 FROM auth.users WHERE id = NEW.user_id FOR UPDATE;
    IF NEW.provider <> 'azure' THEN
        IF EXISTS (SELECT 1 FROM auth.identities i WHERE i.user_id = NEW.user_id AND i.provider = 'azure') THEN
            RAISE EXCEPTION 'bulletin_account_conflict';
        END IF;
        RETURN NEW;
    END IF;
    SELECT email, coalesce(encrypted_password, '') <> '' INTO account_email, password_present
    FROM auth.users WHERE id = NEW.user_id;
    domain_verified := NEW.identity_data->'custom_claims'->'xms_edov';
    IF account_email IS NULL OR nullif(btrim(NEW.identity_data->>'email'), '') IS NULL THEN
        RAISE EXCEPTION 'bulletin_missing_email';
    END IF;
    -- GoTrue assumes verified when xms_edov is absent. Do not accept that fallback.
    -- Only the provider-derived auth.identities data is evidence; never user_metadata.
    IF domain_verified IS NULL OR domain_verified NOT IN ('true'::jsonb, '"true"'::jsonb, '"1"'::jsonb)
       OR NEW.identity_data->'email_verified' IS DISTINCT FROM 'true'::jsonb
       OR lower(btrim(NEW.identity_data->>'email')) <> lower(btrim(account_email)) THEN
        RAISE EXCEPTION 'bulletin_unverified_email';
    END IF;
    IF password_present OR EXISTS (SELECT 1 FROM auth.identities i
        WHERE i.user_id = NEW.user_id AND (i.provider <> 'azure' OR (TG_OP = 'INSERT' AND i.id <> NEW.id))) THEN
        RAISE EXCEPTION 'bulletin_account_conflict';
    END IF;
    IF EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = NEW.user_id AND p.deleted_at IS NOT NULL) THEN
        RAISE EXCEPTION 'bulletin_account_conflict';
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION public.enforce_microsoft_identity() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS tr_enforce_microsoft_identity ON auth.identities;
CREATE TRIGGER tr_enforce_microsoft_identity BEFORE INSERT OR UPDATE ON auth.identities
FOR EACH ROW EXECUTE FUNCTION public.enforce_microsoft_identity();

CREATE OR REPLACE FUNCTION public.enforce_auth_email_change() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF NEW.email IS NOT DISTINCT FROM OLD.email THEN RETURN NEW; END IF;
    IF NEW.raw_app_meta_data->>'provider' = 'azure' THEN
        IF NOT EXISTS (SELECT 1 FROM auth.identities i WHERE i.user_id = NEW.id AND i.provider = 'azure'
            AND lower(i.identity_data->>'email') = lower(NEW.email)
            AND i.identity_data->'email_verified' = 'true'::jsonb
            AND i.identity_data->'custom_claims'->'xms_edov' IN ('true'::jsonb, '"true"'::jsonb, '"1"'::jsonb)) THEN
            RAISE EXCEPTION 'bulletin_unverified_email';
        END IF;
    ELSIF NOT public.is_valid_edu_email(NEW.email) THEN
        RAISE EXCEPTION 'Bulletin requires a valid .edu university email.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.profiles p WHERE p.id <> NEW.id
        AND public.normalize_email(p.email) = public.normalize_email(NEW.email)) THEN
        RAISE EXCEPTION 'bulletin_account_conflict';
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION public.enforce_auth_email_change() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS tr_enforce_auth_email_change ON auth.users;
CREATE TRIGGER tr_enforce_auth_email_change BEFORE UPDATE OF email ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.enforce_auth_email_change();

CREATE OR REPLACE FUNCTION public.handle_new_user() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, university, is_verified)
    VALUES (NEW.id, NEW.email,
        coalesce(nullif(btrim(NEW.raw_user_meta_data->>'full_name'), ''),
            nullif(btrim(NEW.raw_user_meta_data->>'name'), ''), 'Student'),
        CASE WHEN NEW.raw_app_meta_data->>'provider' = 'azure' THEN ''
             ELSE coalesce(NEW.raw_user_meta_data->>'university', 'CSU Long Beach') END,
        NEW.email_confirmed_at IS NOT NULL)
    ON CONFLICT (id) DO UPDATE SET is_verified = (NEW.email_confirmed_at IS NOT NULL);
    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.enforce_signup_rules() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.enforce_edu_email_signup(jsonb) FROM PUBLIC, anon, authenticated;
GRANT USAGE ON SCHEMA public TO supabase_auth_admin;
GRANT EXECUTE ON FUNCTION public.enforce_edu_email_signup(jsonb) TO supabase_auth_admin;
;
