# Use the original Studio Black render as the machine skin

> Date: 2026-11-01
> Status: verified

The owner requires the photographic Studio Black design from the generated concept, rather than a vector approximation or recolor of Classic. Replace the preliminary Canvas chassis/arm with pixels from the original comparison render. Preserve Classic unchanged and keep the existing saved second-theme choice.

The authored source is app/src/main/res/raw/studio_black_reference.png (1536 x 1024). Only rectangle (527,90)-(1026,472) is rendered. A shared background worker decodes the region and prepares three bounded image layers: the machine with paper color removed, a rectified 512 px black-record texture with the original placeholder label and arm occlusion removed, and original foreground arm/spindle pixels. Assets stay cached; bitmap processing is not repeated per song or animation frame. No remote image request is introduced.

Calibrate the record plane to source spindle (745.5,238), horizontal radius 171, vertical scale 141 and perspective coefficient -0.102. Reproject through a cached four-point perspective matrix derived from those source coordinates, while the existing rotation runs inside that plane. Actual local artwork replaces the concept's label. Full black texture and artwork rotate together; machine, photographed arm pose and spindle remain fixed. The Studio arm deliberately keeps the original photographic pose instead of drawing or moving a substitute arm. Existing play/pause and repeat hit areas still invoke real callbacks and expose state; their visual hardware is the photographed pose. Classic mechanical animations remain unchanged.

VinylRecord reads a locally scoped record texture only in Studio Black; default material rendering remains the Classic path. Studio label diameter is 32%, matching the original smaller label, while Classic stays at 50%. Keep metadata, quality, lower cover gradient, library and lyrics shared. Never resize the source sheet without recalibrating the region, plane and foreground mask.

Reference handling: source artwork is the owner's generated concept from this conversation. The original file is retained to avoid another redraw; no external brand, copied UI text or third-party product image is introduced. A built-in imagegen isolation was explored, but the selected implementation uses the original render's pixels to preserve its design identity. ENERGY 2 / RHYTHM 2 / MOTION 2 remain.

Verification covers actual source appearance in native renders, loaded artwork, Classic equality against revision 24, Studio record motion/pause/resume, screen sizes and real control callbacks. Physical-device appearance, frame pacing and TalkBack remain unverified.
Verified: 41 selected tests pass (25 player UI, 5 artwork/assets/motion, 4 library, 2 palette, 3 theme settings, 2 lyrics settings). Source chassis pixel identity, reused asset cache, photo-plane motion, pause/resume, short-screen visibility and enlarged text are covered. Native machine and full-screen previews inspected. Classic fixture PNG remains byte-identical to revision 24. Debug assembly and full Android Lint complete without errors; whitespace and index links pass.
