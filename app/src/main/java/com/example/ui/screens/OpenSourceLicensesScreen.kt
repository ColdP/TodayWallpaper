package btm.m.todaywallpaper.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

@Composable
fun OpenSourceLicensesScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(darkTheme) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {}
    }

    var backProgress by remember { mutableStateOf(0f) }
    var isBackSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        backProgress = 0f
        isBackSwiping = false
    }

    PredictiveBackHandler { progressFlow ->
        try {
            isBackSwiping = true
            progressFlow.collect { backEvent ->
                backProgress = backEvent.progress
            }
            isBackSwiping = false
            backProgress = 1f
            onBack()
        } catch (e: Exception) {
            isBackSwiping = false
            backProgress = 0f
        }
    }

    val scale = 1f - (backProgress * 0.08f)
    val translationXDp = (backProgress * 120).dp
    val alpha = 1f - (backProgress * 0.2f)
    val cornerRadius = (backProgress * 24).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = with(LocalDensity.current) { translationXDp.toPx() },
                    alpha = alpha,
                    clip = cornerRadius > 0.dp,
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            // Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = viewModel.getTranslation("返回", "Back"),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = viewModel.getTranslation("开源代码声明", "Open Source Licenses"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Text(
                    text = viewModel.getTranslation(
                        "Today Wallpaper 基于以下开源库构建，我们衷心感谢其作者的贡献。",
                        "Today Wallpaper is built upon the following open source libraries. We gratefully acknowledge the work of their authors."
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Library cards
                val libraries = listOf(
                    LicenseEntry(
                        name = "Jetpack Compose / AndroidX",
                        copyright = "Copyright © The Android Open Source Project",
                        license = "Apache License 2.0",
                        url = "https://developer.android.com/jetpack"
                    ),
                    LicenseEntry(
                        name = "Kotlin & Coroutines",
                        copyright = "Copyright © JetBrains s.r.o.",
                        license = "Apache License 2.0",
                        url = "https://github.com/JetBrains/kotlin"
                    ),
                    LicenseEntry(
                        name = "Retrofit 2",
                        copyright = "Copyright © 2013 Square, Inc.",
                        license = "Apache License 2.0",
                        url = "https://github.com/square/retrofit"
                    ),
                    LicenseEntry(
                        name = "OkHttp",
                        copyright = "Copyright © 2019 Square, Inc.",
                        license = "Apache License 2.0",
                        url = "https://github.com/square/okhttp"
                    ),
                    LicenseEntry(
                        name = "Moshi",
                        copyright = "Copyright © 2015 Square, Inc.",
                        license = "Apache License 2.0",
                        url = "https://github.com/square/moshi"
                    ),
                    LicenseEntry(
                        name = "Coil",
                        copyright = "Copyright © 2024 Coil Contributors",
                        license = "Apache License 2.0",
                        url = "https://github.com/coil-kt/coil"
                    ),
                    LicenseEntry(
                        name = "Miuix UI",
                        copyright = "Copyright © YuKongA",
                        license = "Apache License 2.0",
                        url = "https://github.com/compose-miuix-ui/miuix"
                    ),
                    LicenseEntry(
                        name = "Haze",
                        copyright = "Copyright © 2023 Chris Banes",
                        license = "Apache License 2.0",
                        url = "https://github.com/chrisbanes/haze"
                    ),
                    LicenseEntry(
                        name = "AndroidLiquidGlassView",
                        copyright = "Copyright © 2025–2026 Donny Yang",
                        license = "MIT License",
                        url = "https://github.com/QmDeve/AndroidLiquidGlassView"
                    ),
                    LicenseEntry(
                        name = "Roborazzi",
                        copyright = "Copyright © 2023 takahirom",
                        license = "Apache License 2.0",
                        url = "https://github.com/takahirom/roborazzi"
                    ),
                    LicenseEntry(
                        name = "Robolectric",
                        copyright = "Copyright © 2010 Xtreme Labs, Pivotal Labs and Google Inc.",
                        license = "MIT License",
                        url = "https://github.com/robolectric/robolectric"
                    ),
                    LicenseEntry(
                        name = "JUnit 4",
                        copyright = "Copyright © 2002–2022 JUnit contributors",
                        license = "Eclipse Public License 1.0",
                        url = "https://github.com/junit-team/junit4"
                    ),
                    LicenseEntry(
                        name = "Firebase Android SDK",
                        copyright = "Copyright © Google LLC",
                        license = "Apache License 2.0",
                        url = "https://github.com/firebase/firebase-android-sdk"
                    ),
                    LicenseEntry(
                        name = "Secrets Gradle Plugin",
                        copyright = "Copyright © Google LLC",
                        license = "Apache License 2.0",
                        url = "https://github.com/google/secrets-gradle-plugin"
                    )
                )

                libraries.forEach { entry ->
                    LicenseCard(entry = entry, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apache License 2.0 notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = viewModel.getTranslation(
                                "Apache License 2.0 全文",
                                "Apache License 2.0 — Full Text"
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.getTranslation(
                                    "http://www.apache.org/licenses/LICENSE-2.0\n\n\u6839\u636E Apache License 2.0 \u6388\u6743\uFF1B\u9664\u975E\u7B26\u5408\u8BB8\u53EF\u8BC1\u8981\u6C42\uFF0C\u5426\u5219\u4E0D\u5F97\u4F7F\u7528\u6B64\u6587\u4EF6\u3002\u60A8\u53EF\u4EE5\u5728\u4E0A\u8FF0\u7F51\u5740\u83B7\u53D6\u8BB8\u53EF\u8BC1\u526F\u672C\u3002\u9664\u975E\u9002\u7528\u6CD5\u5F8B\u8981\u6C42\u6216\u4E66\u9762\u540C\u610F\uFF0C\u5426\u5219\u5728\u8BB8\u53EF\u8BC1\u4E0B\u5206\u53D1\u7684\u8F6F\u4EF6\u6309\u539F\u6837\u5206\u53D1\uFF0C\u4E0D\u9644\u5E26\u4EFB\u4F55\u660E\u793A\u6216\u6697\u793A\u7684\u4FDD\u8BC1\u6216\u6761\u4EF6\u3002",
                                    "http://www.apache.org/licenses/LICENSE-2.0\n\nLicensed under the Apache License, Version 2.0 (the \"License\"); you may not use files except in compliance with the License. You may obtain a copy of the License at the URL above. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
                                ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

private data class LicenseEntry(
    val name: String,
    val copyright: String,
    val license: String,
    val url: String
)

@Composable
private fun LicenseCard(
    entry: LicenseEntry,
    viewModel: WallpaperViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = entry.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.copyright,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.license,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.url,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}