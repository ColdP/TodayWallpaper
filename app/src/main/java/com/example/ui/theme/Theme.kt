package btm.m.todaywallpaper.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

enum class AppThemeMode(val preferenceValue: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromPreference(value: String?): AppThemeMode =
      entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
  }
}

/** Effective app theme, including an explicit light/dark choice from Settings. */
val LocalAppDarkTheme = compositionLocalOf { false }

@Composable
fun isAppDarkTheme(): Boolean = LocalAppDarkTheme.current

/** Process-wide theme preference so every visible Activity refreshes at the same time. */
object AppThemePreference {
  private const val PREFS_NAME = "app_gallery_prefs"
  private const val KEY_THEME_MODE = "theme_mode"
  private val _mode = MutableStateFlow(AppThemeMode.SYSTEM)
  val mode = _mode.asStateFlow()
  private var initialized = false

  fun initialize(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      _mode.value = AppThemeMode.fromPreference(prefs.getString(KEY_THEME_MODE, null))
      initialized = true
    }
  }

  fun setMode(context: Context, mode: AppThemeMode) {
    initialize(context)
    _mode.value = mode
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit().putString(KEY_THEME_MODE, mode.preferenceValue).apply()
  }
}

private val DarkColorScheme =
  darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF38383A),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color.Black,
    scrim = Color.Black,
    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2C2C2E),
    onTertiaryContainer = Color.White,
    error = Color.White,
    onError = Color.Black,
    errorContainer = Color(0xFF2C2C2E),
    onErrorContainer = Color.White,
    surfaceTint = Color.White,
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerHighest = Color(0xFF2C2C2E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color.Black,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD1D1D6),
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
    scrim = Color.Black,
    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE5E5EA),
    onTertiaryContainer = Color.Black,
    error = Color.Black,
    onError = Color.White,
    errorContainer = Color(0xFFE5E5EA),
    onErrorContainer = Color.Black,
    surfaceTint = Color(0xFF1C1C1E),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean? = null,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  AppThemePreference.initialize(context)
  val preferredMode by AppThemePreference.mode.collectAsState()
  val systemDark = isSystemInDarkTheme()
  val useDarkTheme = darkTheme ?: when (preferredMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }
  // Keep controls, dialogs and tracks monochrome on every Android version.
  // In particular, never let Android 12+ Monet colors leak into Material defaults.
  val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

  val controller = remember(useDarkTheme) {
    ThemeController(
      if (useDarkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
      keyColor = Color(0xFF1C1C1E)
    )
  }

  // Refresh system-bar icon contrast whenever the effective app theme changes.
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !useDarkTheme
        isAppearanceLightNavigationBars = !useDarkTheme
      }
      window.statusBarColor = android.graphics.Color.TRANSPARENT
      window.navigationBarColor = android.graphics.Color.TRANSPARENT
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
      }
    }
  }

  CompositionLocalProvider(LocalAppDarkTheme provides useDarkTheme) {
    MiuixTheme(
      controller = controller,
    ) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
  }
}
