-- Message inserts invalidate each participant's private inbox topic.
-- The broadcast contains identity only; the authorized inbox query supplies the summary.
CREATE OR REPLACE FUNCTION private.notify_inbox_on_message()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
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
REVOKE ALL ON FUNCTION private.notify_inbox_on_message() FROM PUBLIC, anon, authenticated;
CREATE TRIGGER messages_notify_inbox
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION private.notify_inbox_on_message();
CREATE POLICY "Participants receive their own inbox invalidations"
ON realtime.messages
FOR SELECT TO authenticated
USING (
    realtime.messages.extension = 'broadcast'
    AND (SELECT realtime.topic()) = 'inbox:' || (SELECT auth.uid())::text
);
