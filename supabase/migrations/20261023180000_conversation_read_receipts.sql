-- Keep the watermark monotonic by requiring all client updates to use the RPC.
REVOKE UPDATE ON TABLE public.conversation_participants FROM PUBLIC, anon, authenticated;
REVOKE UPDATE (
    conversation_id,
    user_id,
    joined_at,
    history_visible_after,
    deleted_at,
    last_read_at
) ON TABLE public.conversation_participants FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.mark_conversation_read(p_conversation_id uuid)
RETURNS timestamptz
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    marked_at timestamptz;
BEGIN
    UPDATE public.conversation_participants
    SET last_read_at = GREATEST(
        COALESCE(last_read_at, '-infinity'::timestamptz),
        statement_timestamp()
    )
    WHERE conversation_id = p_conversation_id
      AND user_id = (SELECT auth.uid())
    RETURNING last_read_at INTO marked_at;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Conversation is unavailable';
    END IF;

    RETURN marked_at;
END;
$$;

REVOKE ALL ON FUNCTION public.mark_conversation_read(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mark_conversation_read(uuid) TO authenticated;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
       AND NOT EXISTS (
           SELECT 1
           FROM pg_publication_tables
           WHERE pubname = 'supabase_realtime'
             AND schemaname = 'public'
             AND tablename = 'conversation_participants'
       ) THEN
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.conversation_participants';
    END IF;
END;
$$;
