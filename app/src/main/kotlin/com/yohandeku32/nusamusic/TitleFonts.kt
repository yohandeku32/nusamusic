package com.yohandeku32.nusamusic

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFont
import androidx.compose.ui.text.font.FontWeight

@Suppress("MentionsGoogle")
private val GOOGLE_FONT_PROVIDER = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val GOOGLE_TITLE_FONT_NAMES = listOf(
    "Inter", "Roboto", "Open Sans", "Lato", "Montserrat", "Poppins",
    "Nunito", "Nunito Sans", "DM Sans", "Manrope", "Outfit", "Urbanist",
    "Plus Jakarta Sans", "Rubik", "Work Sans", "Source Sans 3", "Barlow",
    "Archivo", "IBM Plex Sans", "Roboto Condensed", "Raleway", "Quicksand",
    "Comfortaa", "Oswald", "Bebas Neue", "Space Grotesk", "Josefin Sans",
    "Fira Sans", "Fira Code", "Cinzel", "Playfair Display", "Merriweather",
    "Lora", "Libre Baskerville", "Cormorant Garamond", "Source Serif 4",
    "Roboto Slab", "Bodoni Moda", "Dancing Script", "Pacifico", "Lobster",
    "Kalam", "Caveat", "Permanent Marker"
)

internal val TITLE_FONT_OPTIONS = listOf(
    "Default", "Sans Serif", "Serif", "Monospace", "Cursive"
) + GOOGLE_TITLE_FONT_NAMES

@Suppress("MentionsGoogle")
internal fun titleFontFamilyFor(fontName: String): FontFamily = when (fontName) {
    "Default" -> FontFamily.Default
    "Sans Serif" -> FontFamily.SansSerif
    "Serif" -> FontFamily.Serif
    "Monospace" -> FontFamily.Monospace
    "Cursive" -> FontFamily.Cursive
    in GOOGLE_TITLE_FONT_NAMES -> {
        val googleFont = GoogleFont(fontName, bestEffort = true)
        FontFamily(
            GoogleFontFont(
                googleFont = googleFont,
                fontProvider = GOOGLE_FONT_PROVIDER,
                weight = FontWeight.Normal
            ),
            GoogleFontFont(
                googleFont = googleFont,
                fontProvider = GOOGLE_FONT_PROVIDER,
                weight = FontWeight.Bold
            )
        )
    }
    else -> FontFamily.Serif
}
