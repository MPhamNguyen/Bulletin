-- Schema-only export from Bulletin on 2026-10-17; contains no user rows.



SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


CREATE SCHEMA IF NOT EXISTS "private";


ALTER SCHEMA "private" OWNER TO "postgres";


CREATE SCHEMA IF NOT EXISTS "public";


ALTER SCHEMA "public" OWNER TO "pg_database_owner";


COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE TYPE "public"."report_type" AS ENUM (
    'user',
    'message',
    'listing'
);


ALTER TYPE "public"."report_type" OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "private"."can_access_message"("target_message_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
    select exists (
        select 1
        from public.messages message
        join public.conversation_participants participant
          on participant.conversation_id = message.conversation_id
        where message.id = target_message_id
          and participant.user_id = (select auth.uid())
    );
$$;


ALTER FUNCTION "private"."can_access_message"("target_message_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "private"."is_conversation_participant"("target_conversation_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
    select exists (
        select 1
        from public.conversation_participants cp
        where cp.conversation_id = target_conversation_id
          and cp.user_id = (select auth.uid())
    );
$$;


ALTER FUNCTION "private"."is_conversation_participant"("target_conversation_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "private"."notify_inbox_on_message"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
DECLARE
    participant record;
BEGIN
    FOR participant IN
        SELECT cp.user_id
        FROM public.conversation_participants AS cp
        WHERE cp.conversation_id = NEW.conversation_id
    LOOP
        BEGIN
            PERFORM realtime.send(
                jsonb_build_object('conversation_id', NEW.conversation_id::text),
                'conversation-changed',
                'inbox:' || participant.user_id::text,
                true
            );
        EXCEPTION WHEN OTHERS THEN
            -- A missed invalidation is recovered by the next inbox refresh.
            RAISE WARNING 'Inbox broadcast failed';
        END;
    END LOOP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION "private"."notify_inbox_on_message"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "private"."shares_conversation_with"("target_user_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
    select exists (
        select 1
        from public.conversation_participants current_participant
        join public.conversation_participants other_participant
          on other_participant.conversation_id =
             current_participant.conversation_id
        where current_participant.user_id = (select auth.uid())
          and other_participant.user_id = target_user_id
    );
$$;


ALTER FUNCTION "private"."shares_conversation_with"("target_user_id" "uuid") OWNER TO "postgres";


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


ALTER FUNCTION "public"."enforce_edu_email_signup"("event" "jsonb") OWNER TO "postgres";


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


ALTER FUNCTION "public"."enforce_signup_rules"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") RETURNS TABLE("conversation_id" "uuid")
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
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
$$;


ALTER FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") OWNER TO "postgres";


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


ALTER FUNCTION "public"."handle_new_user"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."hide_conversation"("p_conversation_id" "uuid") RETURNS boolean
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
declare
    changed_count integer;
begin
    if auth.uid() is null then
        return false;
    end if;
    update public.conversation_participants
       set deleted_at = coalesce(deleted_at, now())
     where conversation_id = p_conversation_id
       and user_id = auth.uid();
    get diagnostics changed_count = row_count;
    return changed_count = 1;
end;
$$;


ALTER FUNCTION "public"."hide_conversation"("p_conversation_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_valid_edu_email"("raw" "text") RETURNS boolean
    LANGUAGE "sql" IMMUTABLE PARALLEL SAFE
    SET "search_path" TO ''
    AS $_$ SELECT raw IS NOT NULL AND length(raw) <= 254
   AND lower(btrim(raw)) ~ '^[^@[:space:]]+@([a-z0-9]([a-z0-9-]*[a-z0-9])?[.])+edu$'; $_$;


ALTER FUNCTION "public"."is_valid_edu_email"("raw" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."normalize_email"("raw" "text") RETURNS "text"
    LANGUAGE "sql" IMMUTABLE STRICT PARALLEL SAFE
    SET "search_path" TO ''
    AS $$ SELECT regexp_replace(lower(btrim(raw)), '\+[^@]*@', '@'); $$;


ALTER FUNCTION "public"."normalize_email"("raw" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."restore_conversation"("p_conversation_id" "uuid") RETURNS boolean
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
begin
    if auth.uid() is null then
        return false;
    end if;
    if not exists (
        select 1
          from public.conversation_participants
         where conversation_id = p_conversation_id
           and user_id = auth.uid()
    ) then
        return false;
    end if;
    update public.conversation_participants
       set deleted_at = null
     where conversation_id = p_conversation_id
       and user_id = auth.uid();
    return true;
end;
$$;


ALTER FUNCTION "public"."restore_conversation"("p_conversation_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."restore_conversation_participants_on_message"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO ''
    AS $$
begin
    update public.conversation_participants
       set deleted_at = null
     where conversation_id = new.conversation_id;
    return new;
end;
$$;


ALTER FUNCTION "public"."restore_conversation_participants_on_message"() OWNER TO "postgres";

SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."conversation_participants" (
    "conversation_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "joined_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_read_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);


ALTER TABLE "public"."conversation_participants" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."conversations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "listing_id" "uuid",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."conversations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."listing_bookmarks" (
    "user_id" "uuid" NOT NULL,
    "listing_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."listing_bookmarks" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."listings" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" "text",
    "user_id" "uuid" DEFAULT "auth"."uid"(),
    "category" "text" DEFAULT 'OTHER'::"text" NOT NULL,
    "condition" "text" DEFAULT 'GOOD'::"text" NOT NULL,
    "price" real,
    "description" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "is_sold" boolean DEFAULT false,
    "buyer_id" "uuid",
    CONSTRAINT "listings_category_allowed" CHECK (("category" = ANY (ARRAY['TEXTBOOKS'::"text", 'ELECTRONICS'::"text", 'FURNITURE'::"text", 'CLOTHING'::"text", 'HOUSING'::"text", 'OTHER'::"text"]))),
    CONSTRAINT "listings_condition_allowed" CHECK (("condition" = ANY (ARRAY['NEW'::"text", 'LIKE_NEW'::"text", 'GOOD'::"text", 'FAIR'::"text", 'POOR'::"text"])))
);


ALTER TABLE "public"."listings" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."message_reports" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "message_id" "uuid" NOT NULL,
    "reporter_id" "uuid" DEFAULT "auth"."uid"() NOT NULL,
    "reason" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "status" "text" DEFAULT 'pending'::"text" NOT NULL,
    CONSTRAINT "valid_report_status" CHECK (("status" = ANY (ARRAY['pending'::"text", 'reviewed'::"text", 'dismissed'::"text", 'action_taken'::"text"])))
);


ALTER TABLE "public"."message_reports" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."messages" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "conversation_id" "uuid" NOT NULL,
    "sender_id" "uuid" DEFAULT "auth"."uid"() NOT NULL,
    "body" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "edited_at" timestamp with time zone,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "message_body_length" CHECK ((("char_length"("body") >= 1) AND ("char_length"("body") <= 1000)))
);


ALTER TABLE "public"."messages" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."profiles" (
    "id" "uuid" NOT NULL,
    "email" "text" NOT NULL,
    "full_name" "text" DEFAULT 'Student'::"text" NOT NULL,
    "university" "text" DEFAULT 'CSU Long Beach'::"text" NOT NULL,
    "bio" "text" DEFAULT ''::"text",
    "is_verified" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "major" "text",
    "deleted_at" timestamp with time zone,
    "avatar_url" "text"
);


ALTER TABLE "public"."profiles" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."reports" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "reporter_id" "uuid" NOT NULL,
    "reported_user_id" "uuid",
    "type" "public"."report_type" NOT NULL,
    "message_id" "uuid",
    "listing_id" "uuid",
    "reason" "text" NOT NULL,
    "description" "text",
    "status" "text" DEFAULT 'open'::"text" NOT NULL,
    "modified_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "reports_description_length" CHECK (("char_length"("description") <= 1000)),
    CONSTRAINT "reports_no_self_user_report" CHECK ((("type" <> 'user'::"public"."report_type") OR ("reported_user_id" <> "reporter_id"))),
    CONSTRAINT "reports_nonblank_reason" CHECK (("length"("btrim"("reason")) > 0)),
    CONSTRAINT "reports_valid_status" CHECK (("status" = ANY (ARRAY['open'::"text", 'in_review'::"text", 'resolved'::"text", 'dismissed'::"text"]))),
    CONSTRAINT "reports_valid_target" CHECK (((("type" = 'user'::"public"."report_type") AND ("reported_user_id" IS NOT NULL) AND ("message_id" IS NULL) AND ("listing_id" IS NULL)) OR (("type" = 'message'::"public"."report_type") AND ("message_id" IS NOT NULL) AND ("reported_user_id" IS NULL) AND ("listing_id" IS NULL)) OR (("type" = 'listing'::"public"."report_type") AND ("listing_id" IS NOT NULL) AND ("reported_user_id" IS NULL) AND ("message_id" IS NULL))))
);


ALTER TABLE "public"."reports" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."reviews" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "reviewer_id" "uuid" NOT NULL,
    "reviewee_id" "uuid" NOT NULL,
    "score" integer NOT NULL,
    "comment" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "listing_id" "uuid",
    CONSTRAINT "reviews_comment_check" CHECK (("length"(TRIM(BOTH FROM "comment")) > 0)),
    CONSTRAINT "reviews_score_check" CHECK ((("score" >= 1) AND ("score" <= 5)))
);


ALTER TABLE "public"."reviews" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."user_blocks" (
    "blocker_id" "uuid" NOT NULL,
    "blocked_user_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "user_blocks_check" CHECK (("blocker_id" <> "blocked_user_id"))
);


ALTER TABLE "public"."user_blocks" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."user_reports" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "reporter_id" "uuid" DEFAULT "gen_random_uuid"(),
    "reported_user_id" "uuid" DEFAULT "gen_random_uuid"(),
    "reason" "text",
    "description" "text",
    "status" "text" DEFAULT 'pending'::"text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."user_reports" OWNER TO "postgres";


ALTER TABLE ONLY "public"."conversation_participants"
    ADD CONSTRAINT "conversation_participants_pkey" PRIMARY KEY ("conversation_id", "user_id");



ALTER TABLE ONLY "public"."conversations"
    ADD CONSTRAINT "conversations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."listing_bookmarks"
    ADD CONSTRAINT "listing_bookmarks_pkey" PRIMARY KEY ("user_id", "listing_id");



ALTER TABLE ONLY "public"."listings"
    ADD CONSTRAINT "listings_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."message_reports"
    ADD CONSTRAINT "message_reports_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."messages"
    ADD CONSTRAINT "messages_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_email_key" UNIQUE ("email");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."reports"
    ADD CONSTRAINT "reports_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "reviews_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "unique_listing_review" UNIQUE ("listing_id");



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "unique_reviewer_reviewee" UNIQUE ("reviewer_id", "reviewee_id");



ALTER TABLE ONLY "public"."user_blocks"
    ADD CONSTRAINT "user_blocks_pkey" PRIMARY KEY ("blocker_id", "blocked_user_id");



ALTER TABLE ONLY "public"."user_reports"
    ADD CONSTRAINT "user_reports_pkey" PRIMARY KEY ("id");



CREATE INDEX "conversation_participants_visible_inbox_idx" ON "public"."conversation_participants" USING "btree" ("user_id", "conversation_id") WHERE ("deleted_at" IS NULL);



CREATE UNIQUE INDEX "idx_profiles_active_email" ON "public"."profiles" USING "btree" ("lower"("email")) WHERE ("deleted_at" IS NULL);



CREATE UNIQUE INDEX "idx_profiles_active_email_norm" ON "public"."profiles" USING "btree" ("public"."normalize_email"("email")) WHERE ("deleted_at" IS NULL);



CREATE INDEX "listing_bookmarks_user_created_idx" ON "public"."listing_bookmarks" USING "btree" ("user_id", "created_at" DESC);



CREATE INDEX "listings_category_idx" ON "public"."listings" USING "btree" ("category");



CREATE INDEX "listings_condition_idx" ON "public"."listings" USING "btree" ("condition");



CREATE UNIQUE INDEX "reports_active_message_unique" ON "public"."reports" USING "btree" ("reporter_id", "message_id") WHERE (("type" = 'message'::"public"."report_type") AND ("status" = ANY (ARRAY['open'::"text", 'in_review'::"text"])));



CREATE UNIQUE INDEX "reports_active_user_unique" ON "public"."reports" USING "btree" ("reporter_id", "reported_user_id") WHERE (("type" = 'user'::"public"."report_type") AND ("status" = ANY (ARRAY['open'::"text", 'in_review'::"text"])));



CREATE INDEX "reports_message_idx" ON "public"."reports" USING "btree" ("message_id") WHERE ("message_id" IS NOT NULL);



CREATE UNIQUE INDEX "reports_one_open_listing_report_per_reporter" ON "public"."reports" USING "btree" ("reporter_id", "listing_id") WHERE (("type" = 'listing'::"public"."report_type") AND (("status" IS NULL) OR ("lower"("status") <> ALL (ARRAY['closed'::"text", 'resolved'::"text", 'dismissed'::"text"]))));



CREATE INDEX "reports_queue_idx" ON "public"."reports" USING "btree" ("status", "created_at");



CREATE INDEX "reports_reporter_idx" ON "public"."reports" USING "btree" ("reporter_id");



CREATE UNIQUE INDEX "reviews_one_per_listing_idx" ON "public"."reviews" USING "btree" ("listing_id") WHERE ("listing_id" IS NOT NULL);



CREATE INDEX "user_blocks_blocked_user_idx" ON "public"."user_blocks" USING "btree" ("blocked_user_id");



CREATE UNIQUE INDEX "user_reports_reporter_reported_user_unique" ON "public"."user_reports" USING "btree" ("reporter_id", "reported_user_id") WHERE ("status" IS DISTINCT FROM 'closed'::"text");



CREATE OR REPLACE TRIGGER "messages_notify_inbox" AFTER INSERT ON "public"."messages" FOR EACH ROW EXECUTE FUNCTION "private"."notify_inbox_on_message"();



CREATE OR REPLACE TRIGGER "restore_conversation_participants_on_message" AFTER INSERT ON "public"."messages" FOR EACH ROW EXECUTE FUNCTION "public"."restore_conversation_participants_on_message"();



ALTER TABLE ONLY "public"."conversation_participants"
    ADD CONSTRAINT "conversation_participants_conversation_id_fkey" FOREIGN KEY ("conversation_id") REFERENCES "public"."conversations"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."conversation_participants"
    ADD CONSTRAINT "conversation_participants_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."profiles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."conversations"
    ADD CONSTRAINT "conversations_listing_id_fkey" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."listing_bookmarks"
    ADD CONSTRAINT "listing_bookmarks_listing_id_fkey" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."listing_bookmarks"
    ADD CONSTRAINT "listing_bookmarks_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."listings"
    ADD CONSTRAINT "listings_buyer_id_fkey" FOREIGN KEY ("buyer_id") REFERENCES "public"."profiles"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."listings"
    ADD CONSTRAINT "listings_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."profiles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."message_reports"
    ADD CONSTRAINT "message_reports_message_id_fkey" FOREIGN KEY ("message_id") REFERENCES "public"."messages"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."message_reports"
    ADD CONSTRAINT "message_reports_reporter_id_fkey" FOREIGN KEY ("reporter_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."messages"
    ADD CONSTRAINT "messages_conversation_id_fkey" FOREIGN KEY ("conversation_id") REFERENCES "public"."conversations"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."messages"
    ADD CONSTRAINT "messages_sender_id_fkey" FOREIGN KEY ("sender_id") REFERENCES "public"."profiles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY ("id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reports"
    ADD CONSTRAINT "reports_listing_id_fkey" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reports"
    ADD CONSTRAINT "reports_message_id_fkey" FOREIGN KEY ("message_id") REFERENCES "public"."messages"("id");



ALTER TABLE ONLY "public"."reports"
    ADD CONSTRAINT "reports_reported_user_id_fkey" FOREIGN KEY ("reported_user_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."reports"
    ADD CONSTRAINT "reports_reporter_id_fkey" FOREIGN KEY ("reporter_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "reviews_listing_id_fkey" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "reviews_reviewee_id_fkey" FOREIGN KEY ("reviewee_id") REFERENCES "public"."profiles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reviews"
    ADD CONSTRAINT "reviews_reviewer_id_fkey" FOREIGN KEY ("reviewer_id") REFERENCES "public"."profiles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."user_blocks"
    ADD CONSTRAINT "user_blocks_blocked_user_id_fkey" FOREIGN KEY ("blocked_user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."user_blocks"
    ADD CONSTRAINT "user_blocks_blocker_id_fkey" FOREIGN KEY ("blocker_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."user_reports"
    ADD CONSTRAINT "user_reports_reported_user_id_fkey" FOREIGN KEY ("reported_user_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."user_reports"
    ADD CONSTRAINT "user_reports_reporter_id_fkey" FOREIGN KEY ("reporter_id") REFERENCES "public"."profiles"("id");



CREATE POLICY "Authenticated students can read reviews" ON "public"."reviews" FOR SELECT TO "authenticated" USING (true);



CREATE POLICY "Authenticated students can view profiles" ON "public"."profiles" FOR SELECT TO "authenticated" USING ((("is_verified" = true) OR ("auth"."uid"() = "id")));



CREATE POLICY "Authenticated users can report listings" ON "public"."reports" FOR INSERT TO "authenticated" WITH CHECK ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("type" = 'listing'::"public"."report_type") AND ("listing_id" IS NOT NULL)));



CREATE POLICY "Authenticated users can submit user reports" ON "public"."user_reports" FOR INSERT TO "authenticated" WITH CHECK ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("reported_user_id" <> ( SELECT "auth"."uid"() AS "uid"))));



CREATE POLICY "Authenticated users can view listings" ON "public"."listings" FOR SELECT TO "authenticated" USING (true);



CREATE POLICY "Blocked users cannot read each other's listings" ON "public"."listings" AS RESTRICTIVE FOR SELECT USING ((NOT (EXISTS ( SELECT 1
   FROM "public"."user_blocks"
  WHERE ((("user_blocks"."blocker_id" = "auth"."uid"()) AND ("user_blocks"."blocked_user_id" = "listings"."user_id")) OR (("user_blocks"."blocked_user_id" = "auth"."uid"()) AND ("user_blocks"."blocker_id" = "listings"."user_id")))))));



CREATE POLICY "Conversation participants can view each other" ON "public"."profiles" FOR SELECT TO "authenticated" USING ("private"."shares_conversation_with"("id"));



CREATE POLICY "Participants can report messages" ON "public"."message_reports" FOR INSERT TO "authenticated" WITH CHECK ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND "private"."can_access_message"("message_id")));



CREATE POLICY "Participants can send messages" ON "public"."messages" FOR INSERT TO "authenticated" WITH CHECK ((("sender_id" = ( SELECT "auth"."uid"() AS "uid")) AND "private"."is_conversation_participant"("conversation_id")));



CREATE POLICY "Participants can view conversation members" ON "public"."conversation_participants" FOR SELECT TO "authenticated" USING ("private"."is_conversation_participant"("conversation_id"));



CREATE POLICY "Participants can view conversations" ON "public"."conversations" FOR SELECT TO "authenticated" USING ("private"."is_conversation_participant"("id"));



CREATE POLICY "Participants can view message reports" ON "public"."message_reports" FOR SELECT TO "authenticated" USING ("private"."can_access_message"("message_id"));



CREATE POLICY "Participants can view messages" ON "public"."messages" FOR SELECT TO "authenticated" USING ("private"."is_conversation_participant"("conversation_id"));



CREATE POLICY "Students can submit reviews for other peers" ON "public"."reviews" FOR INSERT TO "authenticated" WITH CHECK ((("auth"."uid"() = "reviewer_id") AND ("reviewer_id" <> "reviewee_id") AND (EXISTS ( SELECT 1
   FROM "public"."profiles" "p"
  WHERE (("p"."id" = "auth"."uid"()) AND ("p"."is_verified" = true))))));



CREATE POLICY "Users can bookmark listings" ON "public"."listing_bookmarks" FOR INSERT TO "authenticated" WITH CHECK (("auth"."uid"() = "user_id"));



CREATE POLICY "Users can create their own blocks" ON "public"."user_blocks" FOR INSERT WITH CHECK (("auth"."uid"() = "blocker_id"));



CREATE POLICY "Users can create their own listings" ON "public"."listings" FOR INSERT TO "authenticated" WITH CHECK ((( SELECT "auth"."uid"() AS "uid") = "user_id"));



CREATE POLICY "Users can delete their own listings" ON "public"."listings" FOR DELETE TO "authenticated" USING ((( SELECT "auth"."uid"() AS "uid") = "user_id"));



CREATE POLICY "Users can insert own profile" ON "public"."profiles" FOR INSERT TO "authenticated" WITH CHECK ((("auth"."uid"() = "id") AND ("is_verified" = false)));



CREATE POLICY "Users can read their bookmarks" ON "public"."listing_bookmarks" FOR SELECT TO "authenticated" USING (("auth"."uid"() = "user_id"));



CREATE POLICY "Users can read their own block relationships" ON "public"."user_blocks" FOR SELECT USING ((("auth"."uid"() = "blocker_id") OR ("auth"."uid"() = "blocked_user_id")));



CREATE POLICY "Users can read their own listing reports" ON "public"."reports" FOR SELECT TO "authenticated" USING ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("type" = 'listing'::"public"."report_type")));



CREATE POLICY "Users can remove their bookmarks" ON "public"."listing_bookmarks" FOR DELETE TO "authenticated" USING (("auth"."uid"() = "user_id"));



CREATE POLICY "Users can remove their own blocks" ON "public"."user_blocks" FOR DELETE USING (("auth"."uid"() = "blocker_id"));



CREATE POLICY "Users can update own profile" ON "public"."profiles" FOR UPDATE TO "authenticated" USING (("auth"."uid"() = "id")) WITH CHECK ((("auth"."uid"() = "id") AND ("is_verified" = ( SELECT "p"."is_verified"
   FROM "public"."profiles" "p"
  WHERE ("p"."id" = "auth"."uid"())))));



CREATE POLICY "Users can update their own listings" ON "public"."listings" FOR UPDATE TO "authenticated" USING ((( SELECT "auth"."uid"() AS "uid") = "user_id")) WITH CHECK ((( SELECT "auth"."uid"() AS "uid") = "user_id"));



ALTER TABLE "public"."conversation_participants" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."conversations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."listing_bookmarks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."listings" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."message_reports" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."messages" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."reports" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "reports_insert_message" ON "public"."reports" FOR INSERT TO "authenticated" WITH CHECK ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("type" = 'message'::"public"."report_type") AND ("message_id" IS NOT NULL) AND ("reported_user_id" IS NULL) AND ("listing_id" IS NULL) AND (EXISTS ( SELECT 1
   FROM ("public"."messages" "m"
     JOIN "public"."conversation_participants" "cp" ON (("cp"."conversation_id" = "m"."conversation_id")))
  WHERE (("m"."id" = "reports"."message_id") AND ("cp"."user_id" = ( SELECT "auth"."uid"() AS "uid")))))));



CREATE POLICY "reports_insert_user" ON "public"."reports" FOR INSERT TO "authenticated" WITH CHECK ((("reporter_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("type" = 'user'::"public"."report_type") AND ("reported_user_id" IS NOT NULL) AND ("reported_user_id" <> ( SELECT "auth"."uid"() AS "uid")) AND ("message_id" IS NULL) AND ("listing_id" IS NULL)));



CREATE POLICY "reports_select_own" ON "public"."reports" FOR SELECT TO "authenticated" USING (("reporter_id" = ( SELECT "auth"."uid"() AS "uid")));



ALTER TABLE "public"."reviews" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "reviews_insert_buyer_only" ON "public"."reviews" FOR INSERT TO "authenticated" WITH CHECK ((("reviewer_id" = "auth"."uid"()) AND (EXISTS ( SELECT 1
   FROM "public"."listings"
  WHERE (("listings"."id" = "reviews"."listing_id") AND ("listings"."buyer_id" = "auth"."uid"()) AND ("listings"."is_sold" = true) AND ("listings"."user_id" = "reviews"."reviewee_id"))))));



ALTER TABLE "public"."user_blocks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."user_reports" ENABLE ROW LEVEL SECURITY;


GRANT USAGE ON SCHEMA "private" TO "authenticated";



GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";
GRANT USAGE ON SCHEMA "public" TO "supabase_auth_admin";



REVOKE ALL ON FUNCTION "private"."can_access_message"("target_message_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "private"."can_access_message"("target_message_id" "uuid") TO "authenticated";



REVOKE ALL ON FUNCTION "private"."is_conversation_participant"("target_conversation_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "private"."is_conversation_participant"("target_conversation_id" "uuid") TO "authenticated";



REVOKE ALL ON FUNCTION "private"."notify_inbox_on_message"() FROM PUBLIC;



REVOKE ALL ON FUNCTION "private"."shares_conversation_with"("target_user_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "private"."shares_conversation_with"("target_user_id" "uuid") TO "authenticated";



REVOKE ALL ON FUNCTION "public"."enforce_edu_email_signup"("event" "jsonb") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."enforce_edu_email_signup"("event" "jsonb") TO "service_role";
GRANT ALL ON FUNCTION "public"."enforce_edu_email_signup"("event" "jsonb") TO "supabase_auth_admin";



GRANT ALL ON FUNCTION "public"."enforce_signup_rules"() TO "anon";
GRANT ALL ON FUNCTION "public"."enforce_signup_rules"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."enforce_signup_rules"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_or_create_listing_conversation"("p_requester_id" "uuid", "p_seller_id" "uuid", "p_listing_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."hide_conversation"("p_conversation_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."hide_conversation"("p_conversation_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."hide_conversation"("p_conversation_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_valid_edu_email"("raw" "text") TO "anon";
GRANT ALL ON FUNCTION "public"."is_valid_edu_email"("raw" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_valid_edu_email"("raw" "text") TO "service_role";



GRANT ALL ON FUNCTION "public"."normalize_email"("raw" "text") TO "anon";
GRANT ALL ON FUNCTION "public"."normalize_email"("raw" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."normalize_email"("raw" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."restore_conversation"("p_conversation_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."restore_conversation"("p_conversation_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."restore_conversation"("p_conversation_id" "uuid") TO "service_role";



REVOKE ALL ON FUNCTION "public"."restore_conversation_participants_on_message"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."restore_conversation_participants_on_message"() TO "service_role";



GRANT ALL ON TABLE "public"."conversation_participants" TO "anon";
GRANT ALL ON TABLE "public"."conversation_participants" TO "authenticated";
GRANT ALL ON TABLE "public"."conversation_participants" TO "service_role";



GRANT ALL ON TABLE "public"."conversations" TO "anon";
GRANT ALL ON TABLE "public"."conversations" TO "authenticated";
GRANT ALL ON TABLE "public"."conversations" TO "service_role";



GRANT ALL ON TABLE "public"."listing_bookmarks" TO "anon";
GRANT ALL ON TABLE "public"."listing_bookmarks" TO "authenticated";
GRANT ALL ON TABLE "public"."listing_bookmarks" TO "service_role";



GRANT ALL ON TABLE "public"."listings" TO "anon";
GRANT ALL ON TABLE "public"."listings" TO "authenticated";
GRANT ALL ON TABLE "public"."listings" TO "service_role";



GRANT ALL ON TABLE "public"."message_reports" TO "anon";
GRANT ALL ON TABLE "public"."message_reports" TO "authenticated";
GRANT ALL ON TABLE "public"."message_reports" TO "service_role";



GRANT ALL ON TABLE "public"."messages" TO "anon";
GRANT ALL ON TABLE "public"."messages" TO "authenticated";
GRANT ALL ON TABLE "public"."messages" TO "service_role";



GRANT ALL ON TABLE "public"."profiles" TO "anon";
GRANT ALL ON TABLE "public"."profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."profiles" TO "service_role";



GRANT ALL ON TABLE "public"."reports" TO "service_role";
GRANT SELECT,INSERT ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("reporter_id") ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("reported_user_id") ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("type") ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("message_id") ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("reason") ON TABLE "public"."reports" TO "authenticated";



GRANT INSERT("description") ON TABLE "public"."reports" TO "authenticated";



GRANT ALL ON TABLE "public"."reviews" TO "anon";
GRANT ALL ON TABLE "public"."reviews" TO "authenticated";
GRANT ALL ON TABLE "public"."reviews" TO "service_role";



GRANT ALL ON TABLE "public"."user_blocks" TO "anon";
GRANT ALL ON TABLE "public"."user_blocks" TO "authenticated";
GRANT ALL ON TABLE "public"."user_blocks" TO "service_role";



GRANT ALL ON TABLE "public"."user_reports" TO "anon";
GRANT ALL ON TABLE "public"."user_reports" TO "authenticated";
GRANT ALL ON TABLE "public"."user_reports" TO "service_role";



ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "service_role";








-- pg_dump excludes custom triggers on the managed auth schema.
CREATE TRIGGER on_auth_user_created AFTER INSERT OR UPDATE OF email_confirmed_at ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
CREATE TRIGGER tr_enforce_signup_rules BEFORE INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.enforce_signup_rules();

-- Storage policies exported from pg_policies (managed storage tables are provided by Supabase).
CREATE POLICY "Users insert their own profile photo" ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'pfp' AND name = (SELECT auth.uid())::text || '/avatar');
CREATE POLICY "Users update their own profile photo" ON storage.objects FOR UPDATE TO authenticated
USING (bucket_id = 'pfp' AND name = (SELECT auth.uid())::text || '/avatar')
WITH CHECK (bucket_id = 'pfp' AND name = (SELECT auth.uid())::text || '/avatar');
CREATE POLICY "pfp users can select own avatar" ON storage.objects FOR SELECT TO authenticated
USING (bucket_id = 'pfp' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);
