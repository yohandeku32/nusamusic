# Slightly stronger artwork gradient

The owner requested a small increase in the Vinyl main player's album-color intensity because the current gradient looked too soft. Keep the existing ivory upper surface, artwork hue extraction, dual-color blending, animation, layout and controls.

Raise the tint saturation multiplier from 0.82 to 0.94 and its cap from 0.60 to 0.68 while retaining lightness 0.82 and the contrast guard. Increase the lower-panel blend at 58% height from 0.10 to 0.14 and at 80% from 0.42 to 0.52. The unchanged gradient endpoint and neutral upper 43% keep the increase restrained.

Direction remains ENERGY 2 / RHYTHM 2 / MOTION 1. The stronger color identifies the playing artwork rather than introducing a new accent or component. Verification uses the existing whole-hue contrast tests and native single-/dual-hue player tests; physical-device rendering remains unverified.

Verification: assembleDebug succeeded; four VinylArtworkTintTest cases and the two existing single-/dual-hue player gradient tests passed. Inspected the regenerated native purple preview. No layout or interaction changes. These checks cover this adjustment; v27's 48-test catalog/player baseline remains recorded separately.
