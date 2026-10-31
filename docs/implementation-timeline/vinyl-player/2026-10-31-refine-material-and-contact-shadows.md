# Refine turntable materials and contact shadows

> Date: 2026-10-31
> Status: verified locally; physical-device appearance and frame pacing unverified

The owner's marked reference identifies the chassis underside and tonearm bearing/counterweight as too flat/cartoon-like. Use consistent top-left lighting: a broad downward/right chassis shadow plus a tighter contact shadow, soft shadows under the bearing, counterweight, and tube, and restrained metallic reflections. Replace the blue concentric bearing rings with dark beveled metal and a smaller shaded cap. Add fine deterministic cast-metal grain with a repeating 128 px shader rather than sparse large dots. Preserve geometry, artwork, controls, progress, metadata, and the red indicator.

Blur shadows once into software-backed ARGB bitmaps, then draw those cached layers on the hardware canvas. Direct Paint mask filters are not supported by the hardware drawing pipeline, as documented by [Android](https://developer.android.com/develop/ui/views/graphics/hardware-accel). The chassis layer uses half-resolution soft-shadow pixels with padded bounds; the bearing, weight, and tube layers are small remembered images. Size changes rebuild the chassis cache; moving the arm transforms the existing layers. No per-frame blur or full-screen software layer is introduced. Reference branding and screenshots are not bundled.

ENERGY 2 / RHYTHM 2 / MOTION 2 remain appropriate. This is material/depth refinement of the accepted physical turntable, not a layout redesign. All 22 selected regressions pass (20 native player UI, 2 artwork). Standard, compact large-title, and close-up labelled artwork fixtures were visually inspected. Debug assembly, APK signature, and whitespace pass. Lint has zero errors and 127 warnings, including a new non-blocking KTX bitmap-factory suggestion. Native previews cannot establish hardware-device appearance or frame pacing; physical-device inspection remains outstanding.
