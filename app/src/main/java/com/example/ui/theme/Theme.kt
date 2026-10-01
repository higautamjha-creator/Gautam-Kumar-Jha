package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = VaultTealLight,
    onPrimary = Color(0xFF04263A),
    primaryContainer = VaultTealContainerDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = VaultEmeraldLight,
    onSecondary = Color(0xFF032E20),
    secondaryContainer = VaultEmeraldContainerDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = VaultAmberWarning,
    onTertiary = Color(0xFF3B2100),
    tertiaryContainer = VaultAmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = VaultBackgroundDark,
    onBackground = VaultOnSurfaceDark,
    surface = VaultSurfaceDark,
    onSurface = VaultOnSurfaceDark,
    surfaceVariant = VaultSurfaceVariantDark,
    onSurfaceVariant = VaultOnSurfaceVariantDark,
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = VaultTealPrimary,
    onPrimary = Color.White,
    primaryContainer = VaultTealContainerLight,
    onPrimaryContainer = Color(0xFF07324A),
    secondary = VaultEmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = VaultEmeraldContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = VaultAmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = VaultAmberContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
    background = VaultBackgroundLight,
    onBackground = VaultOnSurfaceLight,
    surface = VaultSurfaceLight,
    onSurface = VaultOnSurfaceLight,
    surfaceVariant = VaultSurfaceVariantLight,
    onSurfaceVariant = VaultOnSurfaceVariantLight,
    outline = Color(0xFFCBD5E1)
)

val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VaultShapes,
        content = content
    )
}
