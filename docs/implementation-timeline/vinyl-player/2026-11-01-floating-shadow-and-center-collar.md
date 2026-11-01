# Floating chassis shadow and larger center label

> Date: 2026-11-01
> Status: verified

The owner's marked reference requests a visibly lifted machine and a defined dark collar around the center artwork. Increase the existing cached software shadow to three layers: broad ambient, directional cast shadow offset down/right, and close contact shading. Keep the chassis size, margins, controls, and lower cover gradient unchanged.

Increase the artwork diameter from 56% to 60% of the record and frame it with a smooth charcoal collar ending at 70% radius. Playing grooves begin at 72% radius, preserving the opposing specular sectors and stationary lighting. Replace the large black spindle disc with a small silver pin and restrained highlight. Reuse actual cached artwork, rotation, and retention; no reference image enters the bundle.

ENERGY 2 / RHYTHM 2 / MOTION 2 remain unchanged. Verified: production native machine and full-screen renders inspected; all 30 selected player/library/artwork/palette tests pass. Debug assembly and full Android Lint pass without errors. Existing geometry, callbacks, cover retention, and gradient regression coverage remain green. Physical-device appearance remains unverified. Revisit this record when the chassis lighting, center proportions, or shadow pipeline changes.

## Follow-up: flat artwork edge

The owner requested removal of shading at the artwork boundary. Replace the collar radial bevel with a flat charcoal fill, and remove the label vignette and dark perimeter stroke. Keep the larger artwork, metal pin, outer record reflections, and chassis cast/contact shadows. The follow-up also removes the record cast/contact shadow and the recessed platter shading where black vinyl meets the gray ring. The ring uses a flat gray face. Only the chassis exterior retains cast/contact shadows. Verified: the native artwork render shows clean flat boundaries; all 30 selected tests pass, debug assembly succeeds, and full Android Lint reports no errors.


## Follow-up: slightly lighter exterior shadow

Reduce only exterior shadow opacity by about 15%: ambient alpha 34 to 29, cast 82 to 70, contact 48 to 41. Keep blur radii, offsets, record-to-machine shadow, and complete-record rotation unchanged. Verified: three artwork/rotation renderer tests and debug assembly pass; native preview inspected. The previous revision supplies the full lint and broader regression baseline.
