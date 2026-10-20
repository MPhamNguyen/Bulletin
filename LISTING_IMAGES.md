# How marketplace listing images are displayed

Marketplace photos are stored as Supabase Storage objects and linked to listings through ordered rows in `listing_images`. The browse query returns each listing's object paths; the marketplace mapper turns those paths into Storage URLs, and Compose loads those URLs for the detail carousel. Upload, edit, and delete use the listings context's image management workflow.

## Data and display flow

1. The marketplace source queries `listings` and embeds `listing_images(object_path,position)` with each result. It also reads the seller profile. Results are mapped to marketplace snapshots with photo URLs sorted by `position`.
2. The Supabase image URL function calls `storage.from("listing_imgs").publicUrl(objectPath)`. The database stores an object path, not a URL; the Storage URL is derived when mapping the listing.
3. The marketplace detail screen receives the snapshot's `photos` and renders them in a horizontal `LazyRow` with Coil `SubcomposeAsyncImage`. It shows a placeholder when there are no photos or an image fails to load. Bookmark views also render listing photos with Coil.

The relationship is:

```text
listings.id  1 ─── many  listing_images.listing_id
                              │
                              └── object_path + position
                                      │
                                      └── object in Storage bucket listing_imgs
```

Each image row identifies its listing and its object path and display order. Object paths are generated under `<listing-id>/<generated-id>.<extension>`; the extension comes from the validated JPEG, PNG, or WebP media type.

## Upload and edit flow

The listing form picks up to five images. `ListingImageUpload` validates non-empty bytes, a 5 MB maximum per image, and supported media types. On creation, the listing is saved first, then `ManageListingImages` uploads the files, inserts ordered metadata rows, derives their public URLs, and updates the listing view/cache. If upload fails, the listing remains posted and the UI tells the seller its images can be retried by editing.

When editing, existing photos are represented by their URLs and new photos by their upload bytes. The repository maps retained URLs back to existing object paths, rejects URLs that do not belong to that listing, assigns positions to retained and new photos, uploads new objects, updates metadata, and removes rows and Storage objects for removed photos. Listing deletion calls image cleanup before deleting the listing row.

Image operations check that the authenticated Supabase user matches the seller ID. Failed multi-image uploads are cleaned up where possible. Expected repository failures are returned as `Result.Error`; coroutine cancellation is propagated.

## Configuration and limits

`AppContainer` binds `SupabaseListingImagesRepository` and `SupabaseListingsRepository` when a Supabase client is configured; in-memory image and listing repositories are used in inspection or explicitly enabled fallback modes. The marketplace source and listing repository both resolve paths using the `listing_imgs` bucket.

The repository has no committed `supabase/migrations/` directory, so its required `listing_images` table, foreign key, bucket, and access policies are not defined or verified here. The current adapter generates public URLs, which requires the Storage bucket and object access configuration to support those reads. Apply and verify the matching schema and Storage policies in the configured Supabase project before treating remote image display as operational. This documentation describes the application code path; it does not claim live-project configuration has been validated.

## Relevant code

- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/marketplace/infrastructure/repository/SupabaseMarketplaceListingSource.kt` — browse query, embedded image rows, and URL resolution.
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/marketplace/infrastructure/mapper/SupabaseMarketplaceListingMapper.kt` — ordered object paths mapped to marketplace photo URLs.
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/marketplace/presentation/MarketplaceDetailSections.kt` — detail photo carousel and empty/error placeholders.
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/listings/infrastructure/repository/SupabaseListingImagesRepository.kt` — Storage upload/delete and `listing_images` row synchronization.
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/listings/presentation/ListingCreationActions.kt` and `ListingsViewModel.kt` — create and edit orchestration.
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/app/di/AppContainer.kt` — configured Supabase and in-memory bindings.
