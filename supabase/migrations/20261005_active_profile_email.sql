-- An email may be reused after its previous profile is soft-deleted, but only
-- one non-deleted profile may own an email at a time.
DO $$
DECLARE
    email_constraint text;
BEGIN
    SELECT constraint_name
    INTO email_constraint
    FROM information_schema.table_constraints
    WHERE table_schema = 'public'
      AND table_name = 'profiles'
      AND constraint_type = 'UNIQUE'
      AND constraint_name = 'profiles_email_key';

    IF email_constraint IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE public.profiles DROP CONSTRAINT %I',
            email_constraint
        );
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_active_email
    ON public.profiles (LOWER(email))
    WHERE deleted_at IS NULL;
