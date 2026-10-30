-- Listing attributes: optional, typed item facts stored as JSONB on public.listings.
--
-- Additive and backward compatible. Deploy this BEFORE releasing a client that writes `attributes`. To roll back a
-- client release, leave the column and its data in place and disable the client UI; never drop seller data.
--
-- The catalog below mirrors `ListingAttributeCatalog` (Kotlin, listings context). Keep the two in step: stable keys
-- are a contract, and retiring a key means keeping it readable here rather than deleting it.
--
-- The linked project's six remote migrations were inspected read-only on 2026-10-30. This additive
-- migration is absent remotely. Its listing columns, constraints and policies were reproduced locally
-- for verification; recovering and replaying the complete hosted history remains separate work.

alter table public.listings
  add column if not exists attributes jsonb not null default '{}'::jsonb;

alter table public.listings
  drop constraint if exists listings_attributes_is_object;
alter table public.listings
  add constraint listings_attributes_is_object check (jsonb_typeof(attributes) = 'object');

create schema if not exists private;

-- Canonical brand matching key. Mirrors ListingAttributeText.matchKey / MarketplaceAttributeMatching.brandKey:
-- lowercase, '&' -> ' and ', '-', '_' and '/' -> space, drop apostrophes/periods/commas, keep letters, digits and
-- spaces, collapse whitespace. ASCII is exact; other scripts follow the database locale for [:alnum:].
create or replace function private.listing_text_match_key(input text)
returns text
language sql
immutable
set search_path = ''
as $$
  select pg_catalog.btrim(
    pg_catalog.regexp_replace(
      pg_catalog.regexp_replace(
        pg_catalog.regexp_replace(
          pg_catalog.regexp_replace(
            pg_catalog.regexp_replace(pg_catalog.lower(input), '&', ' and ', 'g'),
            '[-_/]', ' ', 'g'),
          '[''’.,]', '', 'g'),
        '[^[:alnum:] ]', '', 'g'),
      '\s+', ' ', 'g')
  );
$$;

create or replace function private.listing_isbn_is_valid(isbn text)
returns boolean
language plpgsql
immutable
set search_path = ''
as $$
declare
  total integer := 0;
  i integer;
  ch text;
begin
  if isbn ~ '^[0-9]{9}[0-9X]$' then
    for i in 1..10 loop
      ch := pg_catalog.substr(isbn, i, 1);
      total := total + (11 - i) * (case when ch = 'X' then 10 else ch::integer end);
    end loop;
    return total % 11 = 0;
  elsif isbn ~ '^97[89][0-9]{10}$' then
    for i in 1..13 loop
      total := total + pg_catalog.substr(isbn, i, 1)::integer * (case when i % 2 = 1 then 1 else 3 end);
    end loop;
    return total % 10 = 0;
  end if;
  return false;
end;
$$;

-- True when `v` is an acceptable value for catalog key `k`. Unknown keys return false (callers decide).
create or replace function private.listing_attribute_value_is_valid(k text, v jsonb)
returns boolean
language plpgsql
immutable
set search_path = ''
as $$
declare
  t text := pg_catalog.jsonb_typeof(v);
  s text := case when pg_catalog.jsonb_typeof(v) = 'string' then v #>> '{}' end;
  n numeric := case when pg_catalog.jsonb_typeof(v) = 'number' then (v #>> '{}')::numeric end;
begin
  return case k
    when 'brand' then t = 'string' and s = pg_catalog.btrim(s) and s !~ '\s{2,}' and pg_catalog.length(s) between 1 and 40
      and private.listing_text_match_key(s) <> ''
    when 'device_type' then s in ('laptop','desktop','tablet','phone','monitor','console','accessory','other')
    when 'ram_gb' then t = 'number' and n = pg_catalog.trunc(n) and n between 1 and 4096
    when 'storage_gb' then t = 'number' and n = pg_catalog.trunc(n) and n between 1 and 100000
    when 'isbn' then t = 'string' and private.listing_isbn_is_valid(s)
    when 'course_code' then t = 'string' and s ~ '^[A-Z]{2,5} [0-9]{2,4}[A-Z]{0,2}$'
    when 'edition' then t = 'number' and n = pg_catalog.trunc(n) and n between 1 and 99
    when 'furniture_type' then s in ('chair','desk','table','bed','sofa','shelf','dresser','lamp','other')
    when 'material' then s in ('wood','metal','plastic','glass','fabric','leather','mixed','other')
    when 'width_cm' then t = 'number' and n between 0.1 and 1000 and n * 10 = pg_catalog.trunc(n * 10)
    when 'clothing_type' then s in ('top','bottom','outerwear','dress','shoes','accessory','other')
    when 'size' then t = 'string' and s ~ '^[A-Z0-9][A-Z0-9 ./-]{0,11}$'
    when 'monthly_rent_usd' then t = 'number' and n between 0.01 and 99999.99 and n * 100 = pg_catalog.trunc(n * 100)
    when 'available_date' then t = 'string' and s ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
      and pg_catalog.to_char(s::date, 'YYYY-MM-DD') = s
      and pg_catalog.date_part('year', s::date) between 2000 and 2100
    when 'housing_type' then s in ('room','shared_room','studio','apartment','house','other')
    else false
  end;
exception when others then
  return false;
end;
$$;

create or replace function private.listing_category_attribute_keys(category text)
returns text[]
language sql
immutable
set search_path = ''
as $$
  select case category
    when 'ELECTRONICS' then array['brand','device_type','ram_gb','storage_gb']
    when 'TEXTBOOKS' then array['isbn','course_code','edition']
    when 'FURNITURE' then array['furniture_type','material','width_cm']
    when 'CLOTHING' then array['clothing_type','size','brand']
    when 'HOUSING' then array['monthly_rent_usd','available_date','housing_type']
    else array[]::text[]
  end;
$$;

-- Server-side mirror of the client rules. Object count and payload bounds apply whenever attributes change.
-- Entry shape and catalog rules apply to new or changed values; unchanged opaque entries survive unrelated edits.
-- Device-type changes also recheck dependent capacities. Category-only edits from older clients remain valid.
create or replace function private.validate_listing_attributes()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  attrs jsonb := new.attributes;
  previous jsonb := case when tg_op = 'UPDATE' then old.attributes else '{}'::jsonb end;
  all_keys constant text[] := array[
    'brand','device_type','ram_gb','storage_gb','isbn','course_code','edition','furniture_type','material',
    'width_cm','clothing_type','size','monthly_rent_usd','available_date','housing_type'];
  retired_keys constant text[] := array[]::text[];
  k text;
  v jsonb;
  t text;
  device text;
begin
  if tg_op = 'UPDATE' and attrs is not distinct from old.attributes then
    return new;
  end if;
  if attrs is null or pg_catalog.jsonb_typeof(attrs) <> 'object' then
    raise exception 'listing attributes must be a JSON object' using errcode = '22023';
  end if;
  if (select pg_catalog.count(*) from pg_catalog.jsonb_object_keys(attrs)) > 16 then
    raise exception 'listing attributes allow at most 16 keys' using errcode = '22023';
  end if;
  if pg_catalog.octet_length(attrs::text) > 2048 then
    raise exception 'listing attributes are larger than 2048 bytes' using errcode = '22023';
  end if;
  device := attrs ->> 'device_type';
  for k, v in select key, value from pg_catalog.jsonb_each(attrs) loop
    -- Opaque stored entries must survive edits to other facts, including nested future values.
    if previous -> k = v and not (
      k in ('ram_gb', 'storage_gb') and attrs -> 'device_type' is distinct from previous -> 'device_type'
    ) then
      continue;
    end if;
    t := pg_catalog.jsonb_typeof(v);
    if k !~ '^[a-z][a-z0-9_]{0,39}$' then
      raise exception 'invalid listing attribute key %', k using errcode = '22023';
    end if;
    if t not in ('string', 'number', 'boolean') then
      raise exception 'listing attribute % must be a string, number or boolean', k using errcode = '22023';
    end if;
    if t = 'string' and ((v #>> '{}') ~ '[\x01-\x1f\x7f]' or pg_catalog.length(v #>> '{}') > 60) then
      raise exception 'listing attribute % has an invalid text value', k using errcode = '22023';
    end if;
    if not (k = any (all_keys)) or k = any (retired_keys) then
      raise exception 'unknown listing attribute %', k using errcode = '22023';
    end if;
    if not (k = any (private.listing_category_attribute_keys(new.category))) then
      raise exception 'listing attribute % does not apply to category %', k, new.category using errcode = '22023';
    end if;
    if private.listing_attribute_value_is_valid(k, v) is distinct from true then
      raise exception 'invalid value for listing attribute %', k using errcode = '22023';
    end if;
    if k = 'ram_gb' and not (coalesce(device, '') = any (array['laptop','desktop','tablet','phone'])) then
      raise exception 'RAM requires a device type that has RAM' using errcode = '22023';
    end if;
    if k = 'storage_gb'
      and not (coalesce(device, '') = any (array['laptop','desktop','tablet','phone','console'])) then
      raise exception 'storage requires a device type that has storage' using errcode = '22023';
    end if;
  end loop;
  return new;
end;
$$;

drop trigger if exists listing_attributes_validate on public.listings;
create trigger listing_attributes_validate
  before insert or update of attributes, category on public.listings
  for each row execute function private.validate_listing_attributes();

revoke all on function private.validate_listing_attributes() from public;

-- Filter columns for marketplace queries. Generated from `attributes`, so they can never disagree with it, and
-- NULL when the attribute is missing or not of the expected shape; NULL never matches an equality or range filter.
alter table public.listings
  add column if not exists attr_device_type text generated always as (
    case when pg_catalog.jsonb_typeof(attributes -> 'device_type') = 'string' then attributes ->> 'device_type' end
  ) stored,
  add column if not exists attr_brand_key text generated always as (
    case when pg_catalog.jsonb_typeof(attributes -> 'brand') = 'string'
      then private.listing_text_match_key(attributes ->> 'brand') end
  ) stored,
  add column if not exists attr_ram_gb integer generated always as (
    case when pg_catalog.jsonb_typeof(attributes -> 'ram_gb') = 'number' and (attributes ->> 'ram_gb') ~ '^[0-9]{1,6}$'
      then (attributes ->> 'ram_gb')::integer end
  ) stored,
  add column if not exists attr_storage_gb integer generated always as (
    case when pg_catalog.jsonb_typeof(attributes -> 'storage_gb') = 'number' and (attributes ->> 'storage_gb') ~ '^[0-9]{1,6}$'
      then (attributes ->> 'storage_gb')::integer end
  ) stored;

comment on column public.listings.attributes is
  'Optional typed item facts (brand, device_type, ram_gb, ...). Item facts only: never contact details, precise locations or credentials.';

-- Partial indexes support Electronics equality filters with chronological paging and numeric ranges.
-- Queries must include exact category equality to use these predicates.
create index if not exists listings_electronics_device_idx
  on public.listings (attr_device_type, created_at desc, id desc)
  where category = 'ELECTRONICS' and not is_sold;
create index if not exists listings_electronics_brand_idx
  on public.listings (attr_brand_key, created_at desc, id desc)
  where category = 'ELECTRONICS' and not is_sold and attr_brand_key is not null;
create index if not exists listings_electronics_ram_idx
  on public.listings (attr_ram_gb)
  where category = 'ELECTRONICS' and not is_sold and attr_ram_gb is not null;
create index if not exists listings_electronics_storage_idx
  on public.listings (attr_storage_gb)
  where category = 'ELECTRONICS' and not is_sold and attr_storage_gb is not null;
