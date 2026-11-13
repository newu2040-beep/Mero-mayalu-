package com.example.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// --- SPACING SCALE (STRICT: 4, 8, 12, 16, 24, 32 ONLY) ---
object MeroSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// --- RADIUS SCALE (STRICT: 8, 16, 999 ONLY) ---
object MeroRadius {
    val small = 8.dp
    val card = 16.dp
    val pill = 999.dp
}

// --- SELECTABLE NEPALI + LATIN FONT FAMILIES (Max 3 weights: 400, 500, 700) ---
val MuktaFontFamily = FontFamily(
    Font(R.font.mukta, FontWeight.Normal),
    Font(R.font.mukta, FontWeight.Medium),
    Font(R.font.mukta, FontWeight.Bold)
)

val NotoDevanagariFontFamily = FontFamily(
    Font(R.font.noto_sans_devanagari, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari, FontWeight.Medium),
    Font(R.font.noto_sans_devanagari, FontWeight.Bold)
)

val HindFontFamily = FontFamily(
    Font(R.font.hind, FontWeight.Normal),
    Font(R.font.hind, FontWeight.Medium),
    Font(R.font.hind, FontWeight.Bold)
)

val KalamFontFamily = FontFamily(
    Font(R.font.kalam, FontWeight.Normal),
    Font(R.font.kalam, FontWeight.Medium),
    Font(R.font.kalam, FontWeight.Bold)
)

enum class MeroFontCatalog(
    val id: String,
    val labelEn: String,
    val labelNe: String,
    val fontFamily: FontFamily
) {
    MUKTA("mukta", "Mukta", "मुक्ता", MuktaFontFamily),
    NOTO_DEVANAGARI("noto_devanagari", "Noto Devanagari", "नोटो देवनागरी", NotoDevanagariFontFamily),
    HIND("hind", "Hind", "हिन्द", HindFontFamily),
    KALAM("kalam", "Kalam", "कलम (हस्तलिखित)", KalamFontFamily);

    companion object {
        fun fromId(id: String): MeroFontCatalog =
            entries.firstOrNull { it.id == id } ?: MUKTA
    }
}

val LocalMeroFontFamily = staticCompositionLocalOf { MuktaFontFamily }

// --- STRICT TYPE SCALE (Title 22/28 Bold, Heading 18/24 Medium, Body 15/22 Regular, Caption 12/16 Regular) ---
object MeroType {
    val Title: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalMeroFontFamily.current,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        )

    val Heading: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalMeroFontFamily.current,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp
        )

    val Body: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalMeroFontFamily.current,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

    val Caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalMeroFontFamily.current,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
}

@Immutable
data class MeroColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val danger: Color,
    val success: Color,
    val isDark: Boolean
)

enum class MeroThemeCatalog(val id: String, val labelEn: String, val labelNe: String) {
    HIMALAYAN_DUSK("himalayan_dusk", "Himalayan Dusk", "हिमाली गोधूलि"),
    NEWARI_HERITAGE("newari_heritage", "Newari Heritage", "नेवारी सम्पदा"),
    RHODODENDRON("rhododendron", "Rhododendron", "लालीगुराँस"),
    MONSOON_MIST("monsoon_mist", "Monsoon Mist", "वर्षा कुहिरो"),
    TIHAR_GLOW("tihar_glow", "Tihar Glow", "तिहार झिलिमिली"),
    PATAN_EVENING("patan_evening", "Patan Evening", "पाटन साँझ"),
    EVEREST_SNOW("everest_snow", "Everest Snow", "सगरमाथा हिउँ"),
    POKHARA_PHEWA("pokhara_phewa", "Pokhara Phewa", "फेवा ताल"),
    MUSTANG_CANYON("mustang_canyon", "Mustang Canyon", "मुस्ताङ घाटी"),
    ILAM_TEA("ilam_tea", "Ilam Tea Hills", "इलाम चियाबारी");

    companion object {
        fun fromId(id: String): MeroThemeCatalog =
            entries.firstOrNull { it.id == id } ?: HIMALAYAN_DUSK
    }
}

enum class MeroThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String): MeroThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class ProfileBannerPreset(
    val id: String,
    val labelEn: String,
    val labelNe: String,
    @param:DrawableRes val drawableRes: Int?
) {
    HIMALAYAN_DUSK("preset:himalayan_dusk", "Himalayan Dusk", "हिमाली गोधूलि", R.drawable.img_profile_cover_dusk),
    TIHAR_LIGHTS("preset:tihar", "Tihar Lights", "तिहार झिलिमिली", R.drawable.img_culture_festival_tihar),
    PATAN_HERITAGE("preset:patan", "Patan Durbar", "पाटन दरबार", R.drawable.img_culture_heritage_patan),
    ANNAPURNA_PEAKS("preset:peaks", "Annapurna Peaks", "अन्नपूर्ण शिखर", R.drawable.img_welcome_himalayan_dusk),
    THEME_SILK("preset:gradient", "Theme Silk", "थिम रेशम", null);

    companion object {
        fun fromId(id: String?): ProfileBannerPreset? =
            entries.firstOrNull { it.id == id }
    }
}

fun tokensFor(catalog: MeroThemeCatalog, dark: Boolean): MeroColors {
    return when (catalog) {
        MeroThemeCatalog.HIMALAYAN_DUSK -> if (!dark) MeroColors(
            bg = Color(0xFFFDF5F7),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF5E3EA),
            primary = Color(0xFF8C1D4E),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF231019),
            textMuted = Color(0xFF664857),
            border = Color(0xFFE7CED8),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1A7440),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF1A0C13),
            surface = Color(0xFF2A1520),
            surface2 = Color(0xFF3D2030),
            primary = Color(0xFFD84B82),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFFFCF2F5),
            textMuted = Color(0xFFD0B2C0),
            border = Color(0xFF543043),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.NEWARI_HERITAGE -> if (!dark) MeroColors(
            bg = Color(0xFFFBF6F1),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF3E5D8),
            primary = Color(0xFF9E3623),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF241610),
            textMuted = Color(0xFF6A5044),
            border = Color(0xFFE5D1C1),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1F6E3D),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF1A100C),
            surface = Color(0xFF2B1C16),
            surface2 = Color(0xFF3E2921),
            primary = Color(0xFFE06B53),
            onPrimary = Color(0xFF1A0C08),
            text = Color(0xFFFCF5EE),
            textMuted = Color(0xFFD4B9AC),
            border = Color(0xFF573B2F),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.RHODODENDRON -> if (!dark) MeroColors(
            bg = Color(0xFFFCF6F8),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF6E2EA),
            primary = Color(0xFFB3245B),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF221118),
            textMuted = Color(0xFF644754),
            border = Color(0xFFE8CCD8),
            danger = Color(0xFFB3261E),
            success = Color(0xFF226F43),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF190C12),
            surface = Color(0xFF291620),
            surface2 = Color(0xFF3B212E),
            primary = Color(0xFFEA5C93),
            onPrimary = Color(0xFF190C12),
            text = Color(0xFFFDF3F7),
            textMuted = Color(0xFFD2B3C1),
            border = Color(0xFF523041),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.MONSOON_MIST -> if (!dark) MeroColors(
            bg = Color(0xFFF5F7FA),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE5EAF2),
            primary = Color(0xFF39557A),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF141D29),
            textMuted = Color(0xFF4F5E73),
            border = Color(0xFFD0D9E6),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1A7440),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF0F151F),
            surface = Color(0xFF1B2433),
            surface2 = Color(0xFF283549),
            primary = Color(0xFF7CA4D9),
            onPrimary = Color(0xFF0F151F),
            text = Color(0xFFF2F6FC),
            textMuted = Color(0xFFB4C2D6),
            border = Color(0xFF3A4B66),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.TIHAR_GLOW -> if (!dark) MeroColors(
            bg = Color(0xFFFCF8F2),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF6E8D4),
            primary = Color(0xFF9C4A12),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF24160A),
            textMuted = Color(0xFF6B523B),
            border = Color(0xFFE7D3BA),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1F6E3D),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF1B120B),
            surface = Color(0xFF2B1D12),
            surface2 = Color(0xFF3F2B1C),
            primary = Color(0xFFF09D48),
            onPrimary = Color(0xFF1B120B),
            text = Color(0xFFFCF6EE),
            textMuted = Color(0xFFD6BEAA),
            border = Color(0xFF593E2A),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.PATAN_EVENING -> if (!dark) MeroColors(
            bg = Color(0xFFFBF5F4),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF2E2DE),
            primary = Color(0xFF6E1A2B),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF221114),
            textMuted = Color(0xFF63484D),
            border = Color(0xFFE3CAC6),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1F6E3D),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF170B0E),
            surface = Color(0xFF271419),
            surface2 = Color(0xFF3A1F26),
            primary = Color(0xFFD97284),
            onPrimary = Color(0xFF170B0E),
            text = Color(0xFFFCF3F4),
            textMuted = Color(0xFFD0B0B6),
            border = Color(0xFF522E37),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.EVEREST_SNOW -> if (!dark) MeroColors(
            bg = Color(0xFFF4F9FC),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE1EFF6),
            primary = Color(0xFF15658A),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF0E1E29),
            textMuted = Color(0xFF476070),
            border = Color(0xFFC8DFEC),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1A7440),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF0C171F),
            surface = Color(0xFF152633),
            surface2 = Color(0xFF20384A),
            primary = Color(0xFF6EC6EA),
            onPrimary = Color(0xFF081A24),
            text = Color(0xFFF0F8FC),
            textMuted = Color(0xFFB0CAD9),
            border = Color(0xFF305066),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.POKHARA_PHEWA -> if (!dark) MeroColors(
            bg = Color(0xFFF3FAF8),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFDDF1EC),
            primary = Color(0xFF0E7368),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF0E211E),
            textMuted = Color(0xFF43645E),
            border = Color(0xFFC4E3DC),
            danger = Color(0xFFB3261E),
            success = Color(0xFF14754B),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF0A1A18),
            surface = Color(0xFF132A27),
            surface2 = Color(0xFF1D3E39),
            primary = Color(0xFF56C9BA),
            onPrimary = Color(0xFF061B18),
            text = Color(0xFFEDF9F7),
            textMuted = Color(0xFFAAD1CA),
            border = Color(0xFF2C5750),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.MUSTANG_CANYON -> if (!dark) MeroColors(
            bg = Color(0xFFFDF6F2),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF5E3DA),
            primary = Color(0xFFA6451F),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF26140D),
            textMuted = Color(0xFF6B4D40),
            border = Color(0xFFE7CFC3),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1F6E3D),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF1C100B),
            surface = Color(0xFF2C1A12),
            surface2 = Color(0xFF40261B),
            primary = Color(0xFFEC8B63),
            onPrimary = Color(0xFF210E06),
            text = Color(0xFFFDF4EF),
            textMuted = Color(0xFFD6B8AA),
            border = Color(0xFF5C3829),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )

        MeroThemeCatalog.ILAM_TEA -> if (!dark) MeroColors(
            bg = Color(0xFFF5F9F4),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE3EFE1),
            primary = Color(0xFF2F6B35),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF132114),
            textMuted = Color(0xFF49624B),
            border = Color(0xFFCAE0C8),
            danger = Color(0xFFB3261E),
            success = Color(0xFF1E6E3B),
            isDark = false
        ) else MeroColors(
            bg = Color(0xFF0E190F),
            surface = Color(0xFF18291A),
            surface2 = Color(0xFF243B26),
            primary = Color(0xFF7CC883),
            onPrimary = Color(0xFF0A1D0C),
            text = Color(0xFFF2F9F2),
            textMuted = Color(0xFFB2CEB5),
            border = Color(0xFF355438),
            danger = Color(0xFFF28B82),
            success = Color(0xFF6DD58C),
            isDark = true
        )
    }
}

val LocalMeroColors = staticCompositionLocalOf {
    tokensFor(MeroThemeCatalog.HIMALAYAN_DUSK, dark = false)
}

@Composable
fun MeroMayaluTheme(
    themeCatalog: MeroThemeCatalog = MeroThemeCatalog.HIMALAYAN_DUSK,
    themeMode: MeroThemeMode = MeroThemeMode.SYSTEM,
    fontCatalog: MeroFontCatalog = MeroFontCatalog.MUKTA,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        MeroThemeMode.SYSTEM -> isSystemInDarkTheme()
        MeroThemeMode.LIGHT -> false
        MeroThemeMode.DARK -> true
    }
    val colors = tokensFor(themeCatalog, isDark)
    val fontFamily = fontCatalog.fontFamily

    val m3Scheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textMuted,
            outline = colors.border,
            error = colors.danger
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textMuted,
            outline = colors.border,
            error = colors.danger
        )
    }

    val typography = Typography(
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    )

    val shapes = Shapes(
        small = RoundedCornerShape(MeroRadius.small),
        medium = RoundedCornerShape(MeroRadius.card),
        large = RoundedCornerShape(MeroRadius.card)
    )

    CompositionLocalProvider(
        LocalMeroColors provides colors,
        LocalMeroFontFamily provides fontFamily
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
