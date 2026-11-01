# Retain Vinyl covers, box quality, and use a red indicator

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending

Vinyl previously keyed ArtworkView's bitmap state by song URI, replacing a displayed cover with null before the next decode completed. Enable an opt-in retention mode for Vinyl only: keep the prior bitmap while loading the replacement. A successful load replaces it; a confirmed missing cover or empty selection clears it normally. Track-keyed effects still cancel superseded loads. Other ArtworkView consumers retain their URI-keyed behavior, preventing retained artwork in recycled list rows.

Give the existing small Hi-Res face a black rectangle with 3 dp corners, 6 dp horizontal and 4 dp vertical padding, and white icon/text. Keep its 44 dp minimum transparent interaction area and reserved quality slot, preserving player geometry and the metadata action. Change the left lamp to bright red during playback and muted red while idle. ENERGY 2 / RHYTHM 2 / MOTION 2 remain unchanged.

All 22 selected tests pass: 20 native player UI and 2 artwork checks. The artwork test holds the actual metadata-read boundary behind a latch, proves the prior cover stays visible, releases the new cover, and proves replacement. Native layout checks cover the 24 dp boxed face, black pixels, retained 44 dp touch target, and stable player geometry. Native previews show the small black badge and muted red idle lamp; bright red playback color is verified in source. Debug assembly, lint (zero errors), signature, and whitespace pass. Physical-device verification remains pending.
