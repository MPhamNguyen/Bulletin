ALTER TABLE public.listings
    ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'AVAILABLE';

UPDATE public.listings
SET status = CASE
    WHEN is_sold THEN 'SOLD'
    WHEN upper(trim(status)) IN ('AVAILABLE', 'SOLD', 'UNKNOWN') THEN upper(trim(status))
    ELSE 'UNKNOWN'
END;

ALTER TABLE public.listings
    ALTER COLUMN status SET DEFAULT 'AVAILABLE',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE public.listings
    DROP CONSTRAINT IF EXISTS listings_status_check;

ALTER TABLE public.listings
    ADD CONSTRAINT listings_status_check CHECK (status IN ('AVAILABLE', 'SOLD', 'UNKNOWN'));

CREATE OR REPLACE FUNCTION public.sync_listing_is_sold_from_status()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.is_sold := NEW.status = 'SOLD';
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS listings_sync_is_sold_from_status ON public.listings;

CREATE TRIGGER listings_sync_is_sold_from_status
BEFORE INSERT OR UPDATE OF status ON public.listings
FOR EACH ROW
EXECUTE FUNCTION public.sync_listing_is_sold_from_status();
