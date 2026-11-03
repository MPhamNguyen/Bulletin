SET local check_function_bodies = off;
CREATE SCHEMA "private";
CREATE TABLE "public"."conversation_participants" (
  "conversation_id" uuid                     NOT NULL,
  "user_id"         uuid                     NOT NULL,
  "joined_at"       timestamp with time zone NOT NULL DEFAULT now(),
  "last_read_at"    timestamp with time zone,
  "deleted_at"      timestamp with time zone,
  CONSTRAINT "conversation_participants_pkey" PRIMARY KEY (conversation_id, user_id)
);
ALTER TABLE "public"."conversation_participants"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."conversations" (
  "id"         uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "listing_id" uuid,
  "created_at" timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at" timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "conversations_pkey" PRIMARY KEY (id)
);
ALTER TABLE "public"."conversations"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."listing_bookmarks" (
  "user_id"    uuid                     NOT NULL,
  "listing_id" uuid                     NOT NULL,
  "created_at" timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "listing_bookmarks_pkey" PRIMARY KEY (user_id, listing_id)
);
ALTER TABLE "public"."listing_bookmarks"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."listings" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "name"        text,
  "category"    text                     NOT NULL DEFAULT 'OTHER'::text,
  "condition"   text                     NOT NULL DEFAULT 'GOOD'::text,
  "price"       real,
  "description" text,
  "created_at"  timestamp with time zone NOT NULL DEFAULT now(),
  "is_sold"     boolean                  DEFAULT false,
  CONSTRAINT "listings_category_allowed"
    CHECK ((category = ANY (ARRAY['TEXTBOOKS'::text, 'ELECTRONICS'::text, 'FURNITURE'::text, 'CLOTHING'::text, 'HOUSING'::text, 'OTHER'::text]))),
  CONSTRAINT "listings_condition_allowed" CHECK ((condition = ANY (ARRAY['NEW'::text, 'LIKE_NEW'::text, 'GOOD'::text, 'FAIR'::text, 'POOR'::text]))),
  CONSTRAINT "listings_pkey" PRIMARY KEY (id),
  "user_id"     uuid                     DEFAULT auth.uid()
);
ALTER TABLE "public"."listings"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."message_reports" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "message_id"  uuid                     NOT NULL,
  "reason"      text                     NOT NULL,
  "created_at"  timestamp with time zone NOT NULL DEFAULT now(),
  "status"      text                     NOT NULL DEFAULT 'pending'::text,
  CONSTRAINT "message_reports_pkey" PRIMARY KEY (id),
  CONSTRAINT "valid_report_status" CHECK ((status = ANY (ARRAY['pending'::text, 'reviewed'::text, 'dismissed'::text, 'action_taken'::text]))),
  "reporter_id" uuid                     NOT NULL DEFAULT auth.uid()
);
ALTER TABLE "public"."message_reports"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."messages" (
  "id"              uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "conversation_id" uuid                     NOT NULL,
  "body"            text                     NOT NULL,
  "created_at"      timestamp with time zone NOT NULL DEFAULT now(),
  "edited_at"       timestamp with time zone,
  "deleted_at"      timestamp with time zone,
  CONSTRAINT "message_body_length" CHECK (((char_length(body) >= 1) AND (char_length(body) <= 1000))),
  CONSTRAINT "messages_pkey" PRIMARY KEY (id),
  "sender_id"       uuid                     NOT NULL DEFAULT auth.uid()
);
ALTER TABLE "public"."messages"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."profiles" (
  "id"          uuid                     NOT NULL,
  "email"       text                     NOT NULL,
  "full_name"   text                     NOT NULL DEFAULT 'Student'::text,
  "university"  text                     NOT NULL DEFAULT 'CSU Long Beach'::text,
  "bio"         text                     DEFAULT ''::text,
  "is_verified" boolean                  DEFAULT false,
  "created_at"  timestamp with time zone DEFAULT now(),
  "major"       text,
  "deleted_at"  timestamp with time zone,
  "avatar_url"  text,
  CONSTRAINT "profiles_email_key" UNIQUE (email),
  CONSTRAINT "profiles_pkey" PRIMARY KEY (id)
);
ALTER TABLE "public"."profiles"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."reviews" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "reviewer_id" uuid                     NOT NULL,
  "reviewee_id" uuid                     NOT NULL,
  "score"       integer                  NOT NULL,
  "comment"     text                     NOT NULL,
  "created_at"  timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "reviews_comment_check" CHECK ((length(TRIM(BOTH FROM comment)) > 0)),
  CONSTRAINT "reviews_pkey" PRIMARY KEY (id),
  CONSTRAINT "reviews_score_check" CHECK (((score >= 1) AND (score <= 5))),
  CONSTRAINT "unique_reviewer_reviewee" UNIQUE (reviewer_id, reviewee_id)
);
ALTER TABLE "public"."reviews"
  ENABLE ROW LEVEL SECURITY;
CREATE TABLE "public"."user_reports" (
  "id"               uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "reporter_id"      uuid                     DEFAULT gen_random_uuid(),
  "reported_user_id" uuid                     DEFAULT gen_random_uuid(),
  "reason"           text,
  "description"      text,
  "status"           text                     NOT NULL DEFAULT 'pending'::text,
  "created_at"       timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "user_reports_pkey" PRIMARY KEY (id)
);
ALTER TABLE "public"."user_reports"
  ENABLE ROW LEVEL SECURITY;
CREATE OR REPLACE FUNCTION private.can_access_message (
  target_message_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.messages message
        join public.conversation_participants participant
          on participant.conversation_id = message.conversation_id
        where message.id = target_message_id
          and participant.user_id = (select auth.uid())
    );
$function$;
CREATE OR REPLACE FUNCTION private.is_conversation_participant (
  target_conversation_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.conversation_participants cp
        where cp.conversation_id = target_conversation_id
          and cp.user_id = (select auth.uid())
    );
$function$;
CREATE OR REPLACE FUNCTION private.shares_conversation_with (
  target_user_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.conversation_participants current_participant
        join public.conversation_participants other_participant
          on other_participant.conversation_id =
             current_participant.conversation_id
        where current_participant.user_id = (select auth.uid())
          and other_participant.user_id = target_user_id
    );
$function$;
CREATE OR REPLACE FUNCTION public.enforce_edu_email_signup (
  event jsonb
)
  RETURNS jsonb
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
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
$function$;
REVOKE ALL ON FUNCTION "public"."enforce_edu_email_signup"(jsonb) FROM PUBLIC, "anon", "authenticated";
CREATE OR REPLACE FUNCTION public.enforce_signup_rules()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
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
$function$;
CREATE OR REPLACE FUNCTION public.get_or_create_listing_conversation (
  p_requester_id uuid,
  p_seller_id    uuid,
  p_listing_id   uuid
)
  RETURNS TABLE (
    conversation_id uuid
  )
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
declare
    existing_id uuid;
begin
    if auth.uid() is null or auth.uid() <> p_requester_id then
        raise exception 'authenticated requester mismatch' using errcode = '42501';
    end if;
    if p_requester_id = p_seller_id or p_listing_id is null then
        raise exception 'invalid listing conversation' using errcode = '22023';
    end if;

    -- Serialize the lookup/insert for this exact listing and unordered user pair.
    perform pg_advisory_xact_lock(
        hashtextextended(
            p_listing_id::text || ':' || least(p_requester_id::text, p_seller_id::text) || ':' ||
            greatest(p_requester_id::text, p_seller_id::text),
            0
        )
    );

    select c.id into existing_id
    from public.conversations c
    where c.listing_id = p_listing_id
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_requester_id)
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_seller_id)
    limit 1;

    if existing_id is null then
        insert into public.conversations (listing_id) values (p_listing_id) returning id into existing_id;
        insert into public.conversation_participants (conversation_id, user_id, joined_at)
        values (existing_id, p_requester_id, now()), (existing_id, p_seller_id, now());
    end if;

    return query select existing_id;
exception when unique_violation then
    select c.id into existing_id from public.conversations c
    where c.listing_id = p_listing_id
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_requester_id)
      and exists (select 1 from public.conversation_participants cp where cp.conversation_id = c.id and cp.user_id = p_seller_id)
    limit 1;
    return query select existing_id;
end;
$function$;
CREATE OR REPLACE FUNCTION public.handle_new_user()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public', 'pg_temp'
  AS $function$
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
$function$;
CREATE OR REPLACE FUNCTION public.is_valid_edu_email (
  raw text
)
  RETURNS boolean
  LANGUAGE sql
  IMMUTABLE
  PARALLEL SAFE
  SET search_path TO ''
  AS $function$ SELECT raw IS NOT NULL AND length(raw) <= 254
   AND lower(btrim(raw)) ~ '^[^@[:space:]]+@([a-z0-9]([a-z0-9-]*[a-z0-9])?[.])+edu$'; $function$;
CREATE OR REPLACE FUNCTION public.normalize_email (
  raw text
)
  RETURNS text
  LANGUAGE sql
  IMMUTABLE
  PARALLEL SAFE
  STRICT
  SET search_path TO ''
  AS $function$ SELECT regexp_replace(lower(btrim(raw)), '\+[^@]*@', '@'); $function$;
ALTER TABLE "public"."conversation_participants"
  ADD CONSTRAINT "conversation_participants_conversation_id_fkey" FOREIGN KEY (conversation_id) REFERENCES public.conversations(id) ON DELETE CASCADE;
ALTER TABLE "public"."listing_bookmarks"
  ADD CONSTRAINT "listing_bookmarks_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE "public"."conversations"
  ADD CONSTRAINT "conversations_listing_id_fkey" FOREIGN KEY (listing_id) REFERENCES public.listings(id) ON DELETE SET NULL;
ALTER TABLE "public"."listing_bookmarks"
  ADD CONSTRAINT "listing_bookmarks_listing_id_fkey" FOREIGN KEY (listing_id) REFERENCES public.listings(id) ON DELETE CASCADE;
ALTER TABLE "public"."messages"
  ADD CONSTRAINT "messages_conversation_id_fkey" FOREIGN KEY (conversation_id) REFERENCES public.conversations(id) ON DELETE CASCADE;
ALTER TABLE "public"."message_reports"
  ADD CONSTRAINT "message_reports_message_id_fkey" FOREIGN KEY (message_id) REFERENCES public.messages(id) ON DELETE CASCADE;
ALTER TABLE "public"."profiles"
  ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE "public"."conversation_participants"
  ADD CONSTRAINT "conversation_participants_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE "public"."reviews"
  ADD CONSTRAINT "reviews_reviewee_id_fkey" FOREIGN KEY (reviewee_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE "public"."reviews"
  ADD CONSTRAINT "reviews_reviewer_id_fkey" FOREIGN KEY (reviewer_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE "public"."user_reports"
  ADD CONSTRAINT "user_reports_reported_user_id_fkey" FOREIGN KEY (reported_user_id) REFERENCES public.profiles(id);
ALTER TABLE "public"."user_reports"
  ADD CONSTRAINT "user_reports_reporter_id_fkey" FOREIGN KEY (reporter_id) REFERENCES public.profiles(id);
CREATE UNIQUE INDEX idx_profiles_active_email ON public.profiles USING btree (lower(email))
  WHERE (deleted_at IS NULL);
CREATE INDEX listing_bookmarks_user_created_idx ON public.listing_bookmarks USING btree (user_id, created_at DESC);
CREATE INDEX listings_category_idx ON public.listings USING btree (category);
CREATE INDEX listings_condition_idx ON public.listings USING btree (condition);
CREATE UNIQUE INDEX user_reports_reporter_reported_user_unique ON public.user_reports USING btree (reporter_id, reported_user_id)
  WHERE (status IS DISTINCT FROM 'closed'::text);
CREATE TRIGGER on_auth_user_created
  AFTER INSERT OR UPDATE OF email_confirmed_at ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();
CREATE TRIGGER tr_enforce_signup_rules
  BEFORE INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_signup_rules();
CREATE POLICY "Participants can view conversation members" ON "public"."conversation_participants"
  FOR SELECT
  TO "authenticated"
  USING (private.is_conversation_participant(conversation_id));
CREATE POLICY "Participants can view conversations" ON "public"."conversations"
  FOR SELECT
  TO "authenticated"
  USING (private.is_conversation_participant(id));
CREATE POLICY "Users can bookmark listings" ON "public"."listing_bookmarks"
  FOR INSERT
  TO "authenticated"
  WITH CHECK ((auth.uid() = user_id));
CREATE POLICY "Users can read their bookmarks" ON "public"."listing_bookmarks"
  FOR SELECT
  TO "authenticated"
  USING ((auth.uid() = user_id));
CREATE POLICY "Users can remove their bookmarks" ON "public"."listing_bookmarks"
  FOR DELETE
  TO "authenticated"
  USING ((auth.uid() = user_id));
CREATE POLICY "Authenticated users can view listings" ON "public"."listings"
  FOR SELECT
  TO "authenticated"
  USING (true);
CREATE POLICY "Participants can view message reports" ON "public"."message_reports"
  FOR SELECT
  TO "authenticated"
  USING (private.can_access_message(message_id));
CREATE POLICY "Participants can view messages" ON "public"."messages"
  FOR SELECT
  TO "authenticated"
  USING (private.is_conversation_participant(conversation_id));
CREATE POLICY "Authenticated students can view profiles" ON "public"."profiles"
  FOR SELECT
  TO "authenticated"
  USING (((is_verified = true) OR (auth.uid() = id)));
CREATE POLICY "Conversation participants can view each other" ON "public"."profiles"
  FOR SELECT
  TO "authenticated"
  USING (private.shares_conversation_with(id));
CREATE POLICY "Users can insert own profile" ON "public"."profiles"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((auth.uid() = id) AND (is_verified = false)));
CREATE POLICY "Users can update own profile" ON "public"."profiles"
  FOR UPDATE
  TO "authenticated"
  USING ((auth.uid() = id))
  WITH CHECK (((auth.uid() = id) AND (is_verified = ( SELECT p.is_verified
   FROM public.profiles p
  WHERE (p.id = auth.uid())))));
CREATE POLICY "Authenticated students can read reviews" ON "public"."reviews"
  FOR SELECT
  TO "authenticated"
  USING (true);
CREATE POLICY "Students can submit reviews for other peers" ON "public"."reviews"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((auth.uid() = reviewer_id) AND (reviewer_id <> reviewee_id) AND (EXISTS ( SELECT 1
   FROM public.profiles p
  WHERE ((p.id = auth.uid()) AND (p.is_verified = true))))));
CREATE POLICY "Authenticated users can submit user reports" ON "public"."user_reports"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((reporter_id = ( SELECT auth.uid() AS uid)) AND (reported_user_id <> ( SELECT auth.uid() AS uid))));
CREATE POLICY "Users insert their own profile photo" ON "storage"."objects"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((bucket_id = 'pfp'::text) AND (name = (( SELECT (auth.uid())::text AS uid) || '/avatar'::text))));
CREATE POLICY "Users update their own profile photo" ON "storage"."objects"
  FOR UPDATE
  TO "authenticated"
  USING (((bucket_id = 'pfp'::text) AND (name = (( SELECT (auth.uid())::text AS uid) || '/avatar'::text))))
  WITH CHECK (((bucket_id = 'pfp'::text) AND (name = (( SELECT (auth.uid())::text AS uid) || '/avatar'::text))));
CREATE POLICY "pfp users can select own avatar" ON "storage"."objects"
  FOR SELECT
  TO "authenticated"
  USING (((bucket_id = 'pfp'::text) AND ((storage.foldername(name))[1] = ( SELECT (auth.uid())::text AS uid))));
ALTER PUBLICATION "supabase_realtime" ADD TABLE "public"."messages";
REVOKE ALL ON FUNCTION "private"."can_access_message"(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "private"."can_access_message"(uuid) TO "authenticated";
REVOKE ALL ON FUNCTION "private"."is_conversation_participant"(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "private"."is_conversation_participant"(uuid) TO "authenticated";
REVOKE ALL ON FUNCTION "private"."shares_conversation_with"(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "private"."shares_conversation_with"(uuid) TO "authenticated";
REVOKE ALL ON FUNCTION "public"."enforce_edu_email_signup"(jsonb) FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."enforce_edu_email_signup"(jsonb) TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."enforce_edu_email_signup"(jsonb) TO "service_role", "supabase_auth_admin";
GRANT EXECUTE ON FUNCTION "public"."enforce_signup_rules"() TO PUBLIC, "anon", "authenticated";
REVOKE ALL ON FUNCTION "public"."enforce_signup_rules"() FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."enforce_signup_rules"() TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."enforce_signup_rules"() TO "service_role";
REVOKE ALL ON FUNCTION "public"."get_or_create_listing_conversation"(uuid, uuid, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "public"."get_or_create_listing_conversation"(uuid, uuid, uuid) TO "anon", "authenticated";
REVOKE ALL ON FUNCTION "public"."get_or_create_listing_conversation"(uuid, uuid, uuid) FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."get_or_create_listing_conversation"(uuid, uuid, uuid) TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."get_or_create_listing_conversation"(uuid, uuid, uuid) TO "service_role";
GRANT EXECUTE ON FUNCTION "public"."handle_new_user"() TO PUBLIC, "anon", "authenticated";
REVOKE ALL ON FUNCTION "public"."handle_new_user"() FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."handle_new_user"() TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."handle_new_user"() TO "service_role";
GRANT EXECUTE ON FUNCTION "public"."is_valid_edu_email"(text) TO PUBLIC, "anon", "authenticated";
REVOKE ALL ON FUNCTION "public"."is_valid_edu_email"(text) FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."is_valid_edu_email"(text) TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."is_valid_edu_email"(text) TO "service_role";
GRANT EXECUTE ON FUNCTION "public"."normalize_email"(text) TO PUBLIC, "anon", "authenticated";
REVOKE ALL ON FUNCTION "public"."normalize_email"(text) FROM "postgres";
GRANT EXECUTE ON FUNCTION "public"."normalize_email"(text) TO "postgres";
GRANT EXECUTE ON FUNCTION "public"."normalize_email"(text) TO "service_role";
GRANT USAGE ON SCHEMA "private" TO "authenticated";
REVOKE ALL ON SCHEMA "public" FROM "supabase_auth_admin";
GRANT USAGE ON SCHEMA "public" TO "supabase_auth_admin";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversation_participants" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."conversation_participants" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversation_participants" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversation_participants" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversations" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."conversations" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversations" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."conversations" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listing_bookmarks" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."listing_bookmarks" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listing_bookmarks" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listing_bookmarks" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listings" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."listings" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listings" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."listings" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."message_reports" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."message_reports" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."message_reports" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."message_reports" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."messages" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."messages" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."messages" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."messages" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."profiles" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reviews" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."reviews" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reviews" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reviews" TO "service_role";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."user_reports" TO "anon", "authenticated";
REVOKE ALL ON TABLE "public"."user_reports" FROM "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."user_reports" TO "postgres";
GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."user_reports" TO "service_role";
ALTER TABLE "public"."listings"
  ADD CONSTRAINT "listings_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
CREATE POLICY "Users can create their own listings" ON "public"."listings"
  FOR INSERT
  TO "authenticated"
  WITH CHECK ((( SELECT auth.uid() AS uid) = user_id));
CREATE POLICY "Users can delete their own listings" ON "public"."listings"
  FOR DELETE
  TO "authenticated"
  USING ((( SELECT auth.uid() AS uid) = user_id));
CREATE POLICY "Users can update their own listings" ON "public"."listings"
  FOR UPDATE
  TO "authenticated"
  USING ((( SELECT auth.uid() AS uid) = user_id))
  WITH CHECK ((( SELECT auth.uid() AS uid) = user_id));
ALTER TABLE "public"."message_reports"
  ADD CONSTRAINT "message_reports_reporter_id_fkey" FOREIGN KEY (reporter_id) REFERENCES auth.users(id) ON DELETE CASCADE;
CREATE POLICY "Participants can report messages" ON "public"."message_reports"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((reporter_id = ( SELECT auth.uid() AS uid)) AND private.can_access_message(message_id)));
ALTER TABLE "public"."messages"
  ADD CONSTRAINT "messages_sender_id_fkey" FOREIGN KEY (sender_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
CREATE POLICY "Participants can send messages" ON "public"."messages"
  FOR INSERT
  TO "authenticated"
  WITH CHECK (((sender_id = ( SELECT auth.uid() AS uid)) AND private.is_conversation_participant(conversation_id)));
CREATE UNIQUE INDEX idx_profiles_active_email_norm ON public.profiles USING btree (public.normalize_email(email))
  WHERE (deleted_at IS NULL);
