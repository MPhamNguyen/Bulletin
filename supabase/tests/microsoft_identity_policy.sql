-- Local/staging only. All fixtures roll back; no real users or OAuth calls.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgtap WITH SCHEMA extensions;
SET search_path = public, extensions;
SELECT no_plan();

CREATE FUNCTION pg_temp.signup(address text, provider text, verified boolean DEFAULT true)
RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE account_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO auth.users (id, email, raw_app_meta_data, raw_user_meta_data, email_confirmed_at)
    VALUES (account_id, address, jsonb_build_object('provider', provider),
        '{"full_name":"Test Student","university":"Untrusted campus"}', now());
    INSERT INTO auth.identities (provider_id, user_id, provider, identity_data)
    VALUES (account_id::text, account_id, provider,
        jsonb_build_object('sub', account_id::text, 'email', address, 'email_verified', verified));
    RETURN account_id;
END;
$$;

SELECT throws_ok($$SELECT pg_temp.signup('not-a-student@example.com', 'email')$$,
    'P0001', 'Bulletin requires a valid .edu university email.', 'Password signup still requires .edu');
SELECT is(public.enforce_edu_email_signup(
    '{"user":{"email":"personal@outlook.com","app_metadata":{"provider":"azure"}}}'),
    '{}'::jsonb, 'Auth hook admits a Microsoft personal email');
SELECT ok(public.enforce_edu_email_signup(
    '{"user":{"email":"personal@outlook.com","app_metadata":{"provider":"email"},"user_metadata":{"provider":"azure"}}}') ? 'error',
    'Editable metadata cannot bypass password eligibility');
SELECT throws_ok($$SELECT pg_temp.signup('unverified@outlook.com', 'azure', false)$$,
    'P0001', 'bulletin_unverified_email', 'Unverified Microsoft identity is rejected');
SELECT is((SELECT count(*) FROM auth.users WHERE email = 'unverified@outlook.com'), 0::bigint,
    'Rejected signup leaves no auth user');
SELECT is((SELECT count(*) FROM public.profiles WHERE email = 'unverified@outlook.com'), 0::bigint,
    'Rejected signup leaves no orphan profile');
SELECT throws_ok($$SELECT pg_temp.signup(NULL, 'azure')$$,
    'P0001', 'bulletin_missing_email', 'Missing Microsoft email is rejected');

CREATE TEMP TABLE actors (kind text PRIMARY KEY, id uuid);
INSERT INTO actors VALUES ('password', pg_temp.signup('password@school.edu','email')),
    ('microsoft', pg_temp.signup('personal@outlook.com','azure')),
    ('other', pg_temp.signup('other@school.edu','email'));
SELECT is((SELECT university FROM profiles WHERE id = (SELECT id FROM actors WHERE kind='microsoft')), '',
    'Microsoft profile does not infer a campus from editable claims');
SELECT is((SELECT count(*) FROM profiles WHERE id = (SELECT id FROM actors WHERE kind='microsoft')), 1::bigint,
    'First Microsoft signup creates exactly one profile');
SELECT throws_ok($$SELECT pg_temp.signup('PASSWORD+alias@school.edu','azure')$$,
    'P0001', 'bulletin_account_conflict', 'Normalized email collision is rejected');
SELECT throws_ok($$INSERT INTO auth.identities(provider_id,user_id,provider,identity_data)
    SELECT 'new-ms-identity',id,'azure','{"email":"password@school.edu","email_verified":true}'
    FROM actors WHERE kind='password'$$,
    'P0001', 'bulletin_account_conflict', 'Microsoft cannot silently link to a password account');
SELECT throws_ok($$INSERT INTO auth.identities(provider_id,user_id,provider,identity_data)
    SELECT 'new-email-identity',id,'email','{"email":"personal@outlook.com","email_verified":true}'
    FROM actors WHERE kind='microsoft'$$,
    'P0001', 'bulletin_account_conflict', 'Linking another method to Microsoft is unavailable');
SELECT throws_ok($$UPDATE auth.users SET email='changed@example.com'
    WHERE id=(SELECT id FROM actors WHERE kind='password')$$,
    'P0001', 'Bulletin requires a valid .edu university email.', 'Password email change keeps eligibility');
SELECT throws_ok($$UPDATE auth.users SET email='changed@outlook.com'
    WHERE id=(SELECT id FROM actors WHERE kind='microsoft')$$,
    'P0001', 'bulletin_unverified_email', 'Microsoft email change requires matching verified identity');
UPDATE profiles SET deleted_at=now() WHERE id=(SELECT id FROM actors WHERE kind='microsoft');
SELECT throws_ok($$UPDATE auth.identities SET identity_data=identity_data
    WHERE user_id=(SELECT id FROM actors WHERE kind='microsoft')$$,
    'P0001', 'bulletin_account_conflict', 'SSO cannot reactivate a soft-deleted profile');
SELECT throws_ok($$SELECT pg_temp.signup('personal+alias@outlook.com','azure')$$,
    'P0001', 'bulletin_account_conflict', 'Soft-deleted profile email aliases stay reserved');
UPDATE profiles SET deleted_at=NULL WHERE id=(SELECT id FROM actors WHERE kind='microsoft');

-- Same RLS checks with synthetic password and Microsoft JWT claims.
INSERT INTO conversations(id) VALUES ('00000000-0000-0000-0000-000000000001');
INSERT INTO conversation_participants(conversation_id,user_id)
SELECT '00000000-0000-0000-0000-000000000001',id FROM actors WHERE kind='other';
INSERT INTO messages(conversation_id,sender_id,body)
SELECT '00000000-0000-0000-0000-000000000001',id,'Synthetic test message' FROM actors WHERE kind='other';
GRANT SELECT ON actors TO authenticated;
SELECT set_config('request.jwt.claims', jsonb_build_object('sub',(SELECT id FROM actors WHERE kind='password'),
    'role','authenticated','aal','aal1')::text, true);
SET LOCAL ROLE authenticated;
SELECT is((SELECT count(*) FROM messages),0::bigint,'Password session cannot read another conversation');
SELECT throws_ok($$INSERT INTO messages(conversation_id,sender_id,body)
    SELECT '00000000-0000-0000-0000-000000000001',id,'Unauthorized' FROM actors WHERE kind='password'$$,
    '42501', 'new row violates row-level security policy for table "messages"',
    'Password session cannot write another conversation');
RESET ROLE;
SELECT set_config('request.jwt.claims', jsonb_build_object('sub',(SELECT id FROM actors WHERE kind='microsoft'),
    'role','authenticated','aal','aal1')::text, true);
SET LOCAL ROLE authenticated;
SELECT is((SELECT count(*) FROM messages),0::bigint,'Microsoft session cannot read another conversation');
SELECT throws_ok($$INSERT INTO messages(conversation_id,sender_id,body)
    SELECT '00000000-0000-0000-0000-000000000001',id,'Unauthorized' FROM actors WHERE kind='microsoft'$$,
    '42501', 'new row violates row-level security policy for table "messages"',
    'Microsoft session cannot write another conversation');
RESET ROLE;
SELECT * FROM finish();
ROLLBACK;
