package btm.m.todaywallpaper.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Update
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.BuildConfig
import btm.m.todaywallpaper.R
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import java.util.Calendar
import java.util.Locale

@Composable
fun AboutScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLicensesPage by remember { mutableStateOf(false) }

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

    val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val deviceRom = resolveDeviceRomLabel()
    val androidVersion = "Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}"
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val appIconBitmap = remember(context) {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    AnimatedContent(
        targetState = showLicensesPage,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(initialOffsetY = { it / 8 }))
                .togetherWith(fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 8 }))
        },
        label = "AboutLicensesTransition"
    ) { isLicensesPage ->
        if (isLicensesPage) {
            OpenSourceLicensesScreen(
                viewModel = viewModel,
                onBack = { showLicensesPage = false },
                modifier = modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = with(androidx.compose.ui.platform.LocalDensity.current) { translationXDp.toPx() },
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
                                text = viewModel.getTranslation("关于", "About"),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // App Info Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = appIconBitmap,
                                                contentDescription = viewModel.getTranslation("应用图标", "App icon"),
                                                modifier = Modifier.size(42.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(14.dp))
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(
                                                text = context.getString(btm.m.todaywallpaper.R.string.app_name),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Start
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = appVersion,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Start
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Developed by btm_m",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = viewModel.getTranslation(
                                            "一个基于Jetpack Compose + Kotlin的壁纸应用。",
                                            "A wallpaper app built with Jetpack Compose + Kotlin."
                                        ),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = viewModel.getTranslation(
                                            "本项目基于MIT协议开源",
                                            "This project is open source under the MIT License."
                                        ),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = viewModel.getTranslation("设备信息", "Device info"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = deviceRom,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Build.MODEL,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = androidVersion,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Project Links Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    Text(
                                        text = viewModel.getTranslation("项目链接", "Project links"),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AboutLinkItem(
                                        title = "GitHub",
                                        subtitle = "https://github.com/ColdP/TodayWallpaper",
                                        onClick = { openUrl(context, "https://github.com/ColdP/TodayWallpaper") }
                                    )
                                    AboutLinkItem(
                                        title = viewModel.getTranslation("应用官网", "Official Website"),
                                        subtitle = "https://tdwp.btm-m.site",
                                        onClick = { openUrl(context, "https://tdwp.btm-m.site") }
                                    )
                                    AboutLinkItem(
                                        title = "btm_m's Official Site",
                                        subtitle = "https://btm-m.site",
                                        onClick = { openUrl(context, "https://btm-m.site") }
                                    )
                                    AboutLinkItem(
                                        title = "btm_m's Blog",
                                        subtitle = "https://btm-m.live",
                                        onClick = { openUrl(context, "https://btm-m.live") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Check for Updates Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(context, UpdateActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = "Check Update",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = viewModel.getTranslation("软件更新", "Software Update"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start
                                        )
                                        Text(
                                            text = viewModel.getTranslation("查看应用更新与版本日志", "View app updates & version changelog"),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Open Source Licenses Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLicensesPage = true },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = viewModel.getTranslation("开源代码声明", "Open Source Licenses"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = viewModel.getTranslation("开源代码声明", "Open Source Licenses"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = viewModel.getTranslation("查看使用的第三方开源库许可信息", "View third-party open source library licenses"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start
                                )
                            }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "© $year btm_m",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutLinkItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // ignore silently if no browser is available
    }
}

private fun resolveDeviceRomLabel(): String {
    val normalized = listOf(Build.BRAND, Build.MANUFACTURER)
        .joinToString(" ")
        .lowercase(Locale.ROOT)

    val label = when {
        normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("poco") -> "HyperOS / MIUI"
        normalized.contains("oneplus") -> "OxygenOS"
        normalized.contains("oppo") -> "ColorOS"
        normalized.contains("realme") -> "realme UI"
        normalized.contains("vivo") || normalized.contains("iqoo") -> "OriginOS / Funtouch OS"
        normalized.contains("huawei") -> "HarmonyOS / EMUI"
        normalized.contains("honor") -> "MagicOS"
        normalized.contains("samsung") -> "One UI"
        normalized.contains("google") || normalized.contains("pixel") -> "Pixel UI"
        normalized.contains("nothing") -> "Nothing OS"
        normalized.contains("asus") -> "ZenUI"
        normalized.contains("motorola") -> "Hello UI"
        normalized.contains("sony") -> "Xperia UI"
        normalized.contains("lenovo") -> "ZUI"
        normalized.contains("meizu") -> "Flyme"
        normalized.contains("nubia") || normalized.contains("redmagic") || normalized.contains("zte") -> "MyOS / nubia UI"
        normalized.contains("infinix") -> "XOS"
        normalized.contains("tecno") -> "HiOS"
        normalized.contains("itel") -> "itel OS"
        normalized.contains("tcl") -> "TCL UI"
        normalized.contains("hmd") || normalized.contains("nokia") -> "HMD UI"
        normalized.contains("sharp") -> "AQUOS UI"
        normalized.contains("lg") -> "LG UX"
        else -> if (Build.MANUFACTURER.isNotBlank()) "${Build.MANUFACTURER} Android" else "${Build.BRAND} Android"
    }

    return "${label}  ·  ${Build.BRAND.ifBlank { Build.MANUFACTURER }}"
}