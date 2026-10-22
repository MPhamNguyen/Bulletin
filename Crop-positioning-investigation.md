# Crop positioning investigation

## What the current code tells us

The Home feed card renders each listing photo twice in `HomeFeedCardBackground` in `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/home/presentation/HomeFeedCard.kt`:

- A blurred, `ContentScale.Crop` copy fills the card as a background.
- A foreground copy uses `ContentScale.Fit` in a `Modifier.matchParentSize()` image and receives the saved crop through `graphicsLayer`.

The foreground crop is applied like this:

```kotlin
scaleX = crop.scale
scaleY = crop.scale
translationX = -crop.offsetX * size.width * crop.scale
translationY = -crop.offsetY * size.height * crop.scale
```

`size` in this graphics layer is the measured size of the content being transformed. With `ContentScale.Fit`, the displayed image can be smaller than the card in one dimension. The crop editor and the display can therefore use different effective frames. That makes the translation a credible cause of the reported displacement. It is not yet proven without the reported temporary-removal check or a screenshot reproduction.

The observed numbers are internally consistent with a centered `Fit` image: a 1535 px card with an approximately 840 px tall image leaves 695 px, or about 347.5 px above and below. A 348 px downward displacement is approximately half that leftover space. This observation alone does not establish whether the graphics layer, image alignment, or another modifier causes the final position.

## Where crop values come from

`ImageCropSettings` lives in `shared/src/commonMain/kotlin/com/jdrms/bulletin/core/common/ImageCropSettings.kt`. It contains `scale` (default `1f`), `offsetX` and `offsetY` (default `0f`). Construction validates scale in `1f..4f` and offsets in `-1f..1f`. The same file parses and serializes values in a URL fragment:

```text
#bulletin_crop=<scale>,<offsetX>,<offsetY>
```

Malformed or out-of-range metadata falls back to the defaults. `imageRequestUrl()` removes the fragment before requesting the image. The crop metadata therefore does not itself request a server-side crop or padding transformation.

The crop is selected in `ListingPhotoCropDialog` in `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/listings/presentation/ListingImageSelectionField.kt`. Its preview is square (`aspectRatio(1f)`), and `frameSize` is set from the preview's measured width. During a drag, offsets are updated using `pan / frameSize / current.scale`; zoom changes scale, constrained to `1f..4f`, and offsets are clamped to `((scale - 1) / (2 * scale))` in either direction. The gesture math is thus normalized against the square editor frame, not explicitly against the Home card's dimensions or the fitted source image dimensions.

The preview itself uses `ContentScale.Fit` and applies the same graphics-layer formula to `SubcomposeAsyncImageContent`. The Home feed, marketplace item card, and marketplace detail photo pager also apply the saved scale and offsets to image content with graphics layers. Those other call sites make this a shared crop convention; a fix should check whether it belongs in a shared crop/display helper or is specific to the Home card's taller frame.

## Request for the remaining evidence

The repository supplies the crop parser, model, request URL handling, editor, and rendering call sites. It does not supply the specific failing URL, its original pixel dimensions, or a screenshot that can be reproduced here. Please provide:

1. The failing image URL including its `#bulletin_crop=...` fragment (the fragment is local metadata and is not sent in the image request).
2. The original image's width and height in pixels.
3. The card's measured width and height, plus the screenshot or a repeatable device/layout setup.
4. The result of the proposed temporary diagnostic: remove the foreground `Modifier.graphicsLayer { ... }` in `HomeFeedCardBackground` and report whether the photo centers. This is a diagnostic only, not a fix.

With these details, the fitted content dimensions and the translation can be compared directly. A durable fix should preserve the crop selected in the square editor while positioning it correctly in the Home card's frame, and should be checked in the other crop consumers.

## Example already present in tests

The repository has a representative serialized value in `shared/src/commonTest/kotlin/com/jdrms/bulletin/domain/marketplace/infrastructure/repository/SupabaseListingBookmarksTest.kt`:

```text
https://cdn.example/listing/first.jpg#bulletin_crop=1.5,0.1,-0.2
```

This is test fixture data only; it is not the reported failing image and has no associated original dimensions.
