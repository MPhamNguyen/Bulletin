-- Non-user configuration exported from the live project on 2026-10-17.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('pfp', 'pfp', true, 5242880, ARRAY['image/jpeg','image/png','image/webp']),
       ('listing_imgs', 'listing_imgs', true, 52428800, ARRAY['image/*'])
ON CONFLICT (id) DO NOTHING;

ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
