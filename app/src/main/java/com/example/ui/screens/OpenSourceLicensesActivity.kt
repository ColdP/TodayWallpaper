package btm.m.todaywallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.widget.enableMomentumTransparentWindow

class OpenSourceLicensesActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMomentumTransparentWindow()
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                OpenSourceLicensesScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}