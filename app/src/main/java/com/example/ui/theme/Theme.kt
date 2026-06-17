package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val AmethystLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFDF8FD),
    surface = Color(0xFFFDF8FD),
    surfaceVariant = Color(0xFFF3E7F3),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val AmethystDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF49454F),
    onSurface = Color(0xFFE6E1E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCEBEB),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607A),
    background = Color(0xFFF4FBF7),
    surface = Color(0xFFF4FBF7),
    surfaceVariant = Color(0xFFE0ECE9),
    onSurface = Color(0xFF191C1C),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF004F4B),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFB2DFDB),
    onSecondary = Color(0xFF1E3533),
    secondaryContainer = Color(0xFF334B49),
    background = Color(0xFF0C1312),
    surface = Color(0xFF121A19),
    surfaceVariant = Color(0xFF33403D),
    onSurface = Color(0xFFE0E3E2),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4948)
)

private val AmberLightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB7),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF725C42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEE0B9),
    onSecondaryContainer = Color(0xFF281805),
    tertiary = Color(0xFF55633C),
    background = Color(0xFFFCF8F3),
    surface = Color(0xFFFCF8F3),
    surfaceVariant = Color(0xFFF0E1D0),
    onSurface = Color(0xFF201B16),
    onSurfaceVariant = Color(0xFF4E453A),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD3BFAB)
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB85E),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3B00),
    onPrimaryContainer = Color(0xFFFFDDB7),
    secondary = Color(0xFFDFC2A1),
    onSecondary = Color(0xFF402D17),
    secondaryContainer = Color(0xFF58442C),
    background = Color(0xFF17130E),
    surface = Color(0xFF211B14),
    surfaceVariant = Color(0xFF4E453A),
    onSurface = Color(0xFFECE0D5),
    onSurfaceVariant = Color(0xFFD3BFAB),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4E453A)
)

private val GeometricLightColorScheme = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF76F8E1),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCECE4),
    onSecondaryContainer = Color(0xFF051F1B),
    background = Color(0xFFF4FAF8),
    surface = Color(0xFFF4FAF8),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurface = Color(0xFF191C1B),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C4)
)

private val GeometricDarkColorScheme = darkColorScheme(
    primary = Color(0xFF56DBC5),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFF76F8E1),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF324B46),
    background = Color(0xFF0E1413),
    surface = Color(0xFF191F1E),
    surfaceVariant = Color(0xFF3F4946),
    onSurface = Color(0xFFE1E3E2),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4946)
)

@Composable
fun MyApplicationTheme(
  themeMode: String = "amethyst",
  isDark: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeMode.lowercase()) {
    "emerald" -> if (isDark) EmeraldDarkColorScheme else EmeraldLightColorScheme
    "amber" -> if (isDark) AmberDarkColorScheme else AmberLightColorScheme
    "geometric" -> if (isDark) GeometricDarkColorScheme else GeometricLightColorScheme
    "charcoal", "dark" -> AmethystDarkColorScheme
    else -> if (isDark) AmethystDarkColorScheme else AmethystLightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
