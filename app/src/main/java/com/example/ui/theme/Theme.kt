package btm.m.todaywallpaper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

private val DarkColorScheme =
  darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color.Black,
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFFE5E5EA),
    tertiary = AccentGold
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF636366),
    tertiary = AccentGold
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val controller = remember {
    ThemeController(
      if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
      keyColor = Color(0xFF1C1C1E)
    )
  }

  MiuixTheme(
    controller = controller,
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
