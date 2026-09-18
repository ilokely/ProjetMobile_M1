package mg.itu.tsenamalagasy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette "marché agricole" : vert dominant (végétal), ocre en accent (terre, épices),
// terracotta pour attirer l'œil sur les annonces "à négocier".

private val ColorSchemeClair = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFC77B32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF7A4A00),
    tertiary = Color(0xFFD84315),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFCCBC),
    onTertiaryContainer = Color(0xFF7A1F00),
    background = Color(0xFFF9FBF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE0E4DA),
    onSurfaceVariant = Color(0xFF44483E),
    outline = Color(0xFF75796D),
)

private val ColorSchemeSombre = darkColorScheme(
    primary = Color(0xFF8BD289),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF14522A),
    onPrimaryContainer = Color(0xFFA6F3A0),
    secondary = Color(0xFFFFB870),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C00),
    onSecondaryContainer = Color(0xFFFFDCB8),
    tertiary = Color(0xFFFFB4A0),
    onTertiary = Color(0xFF5F1500),
    tertiaryContainer = Color(0xFF852000),
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFE1E3DC),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFE1E3DC),
    surfaceVariant = Color(0xFF44483E),
    onSurfaceVariant = Color(0xFFC4C8BA),
    outline = Color(0xFF8E9285),
)

private val FormesArrondies = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TsenaMalagasyTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) ColorSchemeSombre else ColorSchemeClair
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = FormesArrondies,
        content = content,
    )
}
