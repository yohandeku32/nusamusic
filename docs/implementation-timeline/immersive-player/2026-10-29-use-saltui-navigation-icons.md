# Use SaltUI for Immersive navigation icons

Date: 2026-10-29

The listener requested trying SaltUI icons in navigation after the lyric Settings pilot. The normal and overlay Immersive navigation now use SaltUI's public Icon component, with explicit existing size and tint. Discovery, Lyrics, Library and Settings retain their existing glyphs. Return to Now Playing uses the actual SaltIcons.ArrowBack vector in both the Library return slot and Artist overlay action.

SaltUI 2.6.0-beta03 exports ArrowBack, Check, ChevronRight, Success and Uncheck. It does not supply Discovery, Lyrics, Library or Settings glyphs. This is partial use of SaltUI's built-in artwork, not a claim that SaltUI contains a complete navigation icon pack. The other glyphs are inputs to Salt's Icon renderer. See the [pinned icon sources](https://github.com/Moriafly/SaltUI/tree/2.6.0-beta03/ui2/src/commonMain/kotlin/com/moriafly/salt/ui/icons) and [renderer](https://github.com/Moriafly/SaltUI/blob/2.6.0-beta03/ui2/src/commonMain/kotlin/com/moriafly/salt/ui/Icon.kt).

The ArrowBack vector has upstream autoMirror enabled for RTL. The localized Back to Now Playing label, callbacks, selected/disabled state, touch areas, press motion, centering and existing Hi-Res navigation material retain their owners. Non-navigation player actions continue using their previous Icon renderer. No dependency or framework upgrade is added. The bundled SaltUI Apache-2.0 license already covers this use, and THIRD_PARTY_NOTICES now lists it.

Verification uses the existing native navigation, centering, player-scale and mini lyric checks plus Android compilation and lint. These exercise both player themes, navigation callbacks, Library return, narrow large text, empty state, centering transitions and preserved player layout. Device screenshots and physical-device frame pacing remain separate acceptance checks. No APK is assembled.

Result: all 23 focused tests passed with zero failures/errors/skips. Android compilation and lintDebug passed, with zero lint errors, 120 existing warnings and three hints. Native dark and narrow/light previews were inspected. CI's existing Navigation-glass-previews artifact retains these fixtures.
