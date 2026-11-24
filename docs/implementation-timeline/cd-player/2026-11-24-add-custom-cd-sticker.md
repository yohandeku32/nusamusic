# Add a custom CD case sticker

> Date: 2026-11-24
> Status: partial
> Change type: feature
> Scope: CD player appearance

## Outcome

With CD selected, Settings → Player appearance lets listeners choose or replace the image on the upper-left sticker of the main jewel case. It also lets them restore the original artist sticker.

## Implementation

- [CdStickerStore](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/CdStickerStore.kt) resizes the selected image and writes an app-private PNG atomically. The app can restore the sticker on startup without access to the original image.
- [CdJewelCase](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/CdJewelCase.kt) fits the image within the original sticker's bounds, preserving aspect ratio and transparency. Placing it on the lid keeps it stationary while the disc rotates. The yellow inventory sticker remains visible.
- [PlayerSettings](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/PlayerSettings.kt) uses the existing appearance controls so choose, replace, and reset sit with the other CD options.

## Verification

Git's whitespace check passed. The storage code compiled against Android API 36 with Kotlin 2.2.10. An on-device check of the system picker, transparent images, and persistence across restarts is pending.
