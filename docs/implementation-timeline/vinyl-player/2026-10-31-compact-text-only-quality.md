# Compact text-only Vinyl quality

> Date: 2026-10-31
> Status: verified locally; physical-device inspection pending

Remove the lossless/Hi-Res icon and its obsolete flag from VinylQualityBadge and callers. Render only the real quality label, reduced from 11 sp/14 sp to 10 sp/12 sp. The owner's follow-up requests a lower-opacity shape: use 12% black over the cream page, with dark text for contrast. Reduce padding to 5 dp horizontal and 3 dp vertical and corners to 2 dp, yielding an 18 dp visual height. Keep the existing 44 dp minimum touch target, quality slot, metadata action, availability behavior, and player geometry. Other player-mode logos are unaffected. ENERGY 2 / RHYTHM 2 / MOTION 2 remain unchanged.

All 20 native player UI regressions pass. The face height is 18 dp while the touch target remains at least 44 dp; pixel checks verify the low-opacity neutral background and the metadata action still works. Text contrast against the composited cream/12%-black face is approximately 10.45:1. Standard native preview inspected; final debug assembly, lint (zero errors), APK signature, and whitespace pass. Physical-device inspection remains outstanding.
