package btm.m.todaywallpaper.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import btm.m.todaywallpaper.ui.theme.isAppDarkTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

@Composable
fun OpenSourceLicensesScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isAppDarkTheme()

    DisposableEffect(darkTheme) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
        onDispose {}
    }

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveBackMaxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    var backProgress by remember { mutableStateOf(0f) }
    var backDirection by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        backProgress = 0f
    }

    PredictiveBackHandler(enabled = predictiveBackEnabled) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                backProgress = kotlin.math.min(
                    backEvent.progress,
                    predictiveBackMaxProgress / 100f
                )
                backDirection = if (
                    backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT
                ) {
                    -1f
                } else {
                    1f
                }
            }
            backProgress = 1f
            onBack()
        } catch (_: Exception) {
            backProgress = 0f
        }
    }

    // Keep the secondary Activity's gesture transform identical to UpdateScreen.
    val scale = 1f - (backProgress * 0.12f)
    val translationXDp = (backProgress * 48f * backDirection).dp
    val alpha = 1f
    val cornerRadius = 28.dp * backProgress
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = with(density) { translationXDp.toPx() },
                    translationY = with(density) { (backProgress * 16f).dp.toPx() },
                    alpha = alpha,
                    clip = cornerRadius > 0.dp,
                    shape = RoundedCornerShape(cornerRadius)
                ),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LicenseIntroCard(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = viewModel.getTranslation(
                                    "使用的开源库",
                                    "Open source libraries"
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OPEN_SOURCE_LIBRARIES.forEachIndexed { index, entry ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                                LicenseItem(
                                    entry = entry,
                                    viewModel = viewModel,
                                    context = context
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = viewModel.getTranslation(
                                    "许可证说明",
                                    "License notice"
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = viewModel.getTranslation(
                                    "Apache License 2.0",
                                    "Apache License 2.0"
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.getTranslation(
                                    "https://www.apache.org/licenses/LICENSE-2.0\n\n根据 Apache License 2.0 授权；除非符合许可证要求，否则不得使用此文件。您可以在上述地址获取许可证副本。除非适用法律要求或书面同意，否则在许可证下分发的软件按原样分发，不附带任何明示或暗示的保证或条件。",
                                    "https://www.apache.org/licenses/LICENSE-2.0\n\nLicensed under the Apache License, Version 2.0 (the \"License\"); you may not use files except in compliance with the License. You may obtain a copy of the License at the URL above. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
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
}

@Composable
private fun LicenseIntroCard(viewModel: WallpaperViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.getTranslation(
                            "第三方开源库",
                            "Third-party libraries"
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.getTranslation(
                            "感谢所有开源项目及其贡献者。",
                            "With thanks to the open source projects and their contributors."
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = viewModel.getTranslation(
                    "Today Wallpaper 基于以下开源库构建。点击条目可访问对应项目页面，查看项目详情与许可证信息。",
                    "Today Wallpaper is built with the open source libraries below. Tap an entry to visit its project page and review its license information."
                ),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun LicenseItem(
    entry: LicenseEntry,
    viewModel: WallpaperViewModel,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openLicenseUrl(context, entry.url) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = entry.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.copyright,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = entry.license,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.url,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = viewModel.getTranslation("打开项目页面", "Open project page"),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun openLicenseUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // Ignore silently when no browser is available.
    }
}

private data class LicenseEntry(
    val name: String,
    val copyright: String,
    val license: String,
    val url: String
)

private val OPEN_SOURCE_LIBRARIES = listOf(
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