package btm.m.todaywallpaper.ui.screens

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalDensity
import btm.m.todaywallpaper.MainActivity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import btm.m.todaywallpaper.ui.viewmodel.WallpaperUiState
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.PointerEventPass

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    onViewDetail: (String, String, String?, String) -> Unit,
    homeRefreshGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayState by viewModel.todayWallpaper.collectAsState()
    val settingState by viewModel.wallpaperSettingState.collectAsState()
    val gestureEnabled by viewModel.homeGestureEnabled.collectAsState()
    val hasPrevious by viewModel.hasPreviousWallpaper.collectAsState()
    val hasNext by viewModel.hasNextWallpaper.collectAsState()

    DisposableEffect(homeRefreshGlassView) {
        onDispose {
            homeRefreshGlassView?.post {
                try {
                    homeRefreshGlassView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Handle Toast outcomes for setting wallpapers
    LaunchedEffect(settingState) {
        when (settingState) {
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Success -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation("壁纸设置成功！", "Wallpaper applied successfully!"),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetWallpaperSettingState()
            }
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation(
                        "壁纸设置失败: ${(settingState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error).message}",
                        "Apply failed: ${(settingState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error).message}"
                    ),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetWallpaperSettingState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = todayState) {
            is WallpaperUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("home_loader")
                    )
                }
            }
            is WallpaperUiState.Success -> {
                val wallpaper = state.data

                // 1. Full Imperial Background Wallpaper with crossfade animation
                AnimatedContent(
                    targetState = wallpaper,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 500)) togetherWith
                            fadeOut(tween(durationMillis = 400))
                    },
                    contentKey = { it.id },
                    label = "WallpaperTransition"
                ) { wp ->
                    AsyncImage(
                        model = wp.imageUrl,
                        contentDescription = "Today Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
 
                // Light tint overlay so that widgets always stand out cleanly
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
 
                // 2. Gesture overlay - transparent Box that catches swipe and tap gestures
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(wallpaper.id, gestureEnabled) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                if (!gestureEnabled) {
                                    // Gesture disabled: wait for up and treat as tap
                                    var released = false
                                    while (!released) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        released = event.changes.all { !it.pressed }
                                    }
                                    onViewDetail(wallpaper.id, wallpaper.imageUrl, wallpaper.author, wallpaper.source)
                                    return@awaitEachGesture
                                }
                                val startX = down.position.x
                                val startY = down.position.y
                                var totalDx = 0f
                                var totalDy = 0f
                                // Wait for drag to start
                                val up = drag(down.id) { change ->
                                    totalDx += change.positionChange().x
                                    totalDy += change.positionChange().y
                                    change.consume()
                                }
                                val threshold = 80f
                                val absDx = kotlin.math.abs(totalDx)
                                val absDy = kotlin.math.abs(totalDy)
                                if (absDx > threshold || absDy > threshold) {
                                    when {
                                        absDx >= absDy -> {
                                            if (totalDx < 0) viewModel.goToNextWallpaper()
                                            else viewModel.goToPreviousWallpaper()
                                        }
                                        else -> {
                                            if (totalDy < 0) viewModel.goToNextWallpaper()
                                            else viewModel.goToPreviousWallpaper()
                                        }
                                    }
                                } else {
                                    // Short drag = tap → open detail
                                    onViewDetail(wallpaper.id, wallpaper.imageUrl, wallpaper.author, wallpaper.source)
                                }
                            }
                        }
                )
 
                // 3. Immersive Content Layout conforming with safe system bars padding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        // Since bottom has floating capsule, padding bottom moves title safely above it
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 100.dp)
                ) {
                    // Top Row: contains "Today Wallpaper" on Left and capsule nav on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 28.sp
                            )
                            Text(
                                text = "Wallpaper",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 28.sp
                            )
                        }
 
                        // Capsule with Refresh | Prev | Next buttons
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coordinates ->
                                    val size = coordinates.size
                                    val position = coordinates.positionInRoot()
                                    homeRefreshGlassView?.let { hg ->
                                        hg.post {
                                            val lp = hg.layoutParams as? android.widget.FrameLayout.LayoutParams
                                            if (lp != null) {
                                                lp.width = size.width
                                                lp.height = size.height
                                                hg.layoutParams = lp
                                            } else {
                                                hg.layoutParams = android.widget.FrameLayout.LayoutParams(size.width, size.height)
                                            }
                                            hg.translationX = position.x
                                            hg.translationY = position.y
                                            
                                            val buttonCornerRadiusPx = 22f * context.resources.displayMetrics.density
                                            val blurRadius = 8f * context.resources.displayMetrics.density
                                            btm.m.todaywallpaper.MainActivity.safeConfigure(
                                                view = hg,
                                                red = 1.0f,
                                                green = 1.0f,
                                                blue = 1.0f,
                                                alpha = 0.15f,
                                                cornerRadius = buttonCornerRadiusPx,
                                                blurRadius = blurRadius,
                                                refractionHeight = 10f
                                            )
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Refresh button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewModel.fetchTodayWallpaper() }
                                    .testTag("home_refresh_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Divider between Refresh and Prev
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            // Previous button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = hasPrevious) {
                                        viewModel.goToPreviousWallpaper()
                                    }
                                    .testTag("home_prev_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Divider between Prev and Next
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            // Next button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        viewModel.goToNextWallpaper()
                                    }
                                    .testTag("home_next_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
 
                    // Middle-Left Date info floating beautifully
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(top = 40.dp) // Offset from top
                    ) {
                        val calendar = Calendar.getInstance()
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val dayOfMonth = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
 
                        val weekdayFormatZh = SimpleDateFormat("EEEE", Locale.CHINA)
                        val weekdayFormatEn = SimpleDateFormat("EEEE", Locale.ENGLISH)
                        val language by viewModel.language.collectAsState()
                        val weekdayStr = if (language == "zh") weekdayFormatZh.format(calendar.time) else weekdayFormatEn.format(calendar.time)
 
                        val lunarStr = getLunarDateHelper(calendar)
 
                        Text(
                            text = "$year/$month",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dayOfMonth,
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.displayLarge,
                            lineHeight = 76.sp
                        )
                        Text(
                            text = weekdayStr,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (language == "zh") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$lunarStr",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
 
                    // Bottom-Right Wallpaper Metadata info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = wallpaper.author ?: viewModel.getTranslation("和风意境", "Aesthetic Vector"),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "From ${wallpaper.source}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            is WallpaperUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.getTranslation("壁纸加载错误", "Failed to Load Wallpaper"),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.fetchTodayWallpaper() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = viewModel.getTranslation("重试", "Retry"))
                    }
                }
            }
        }
    }
}

/**
 * Desk Calendar Card displaying live system calendar states and randomized bilingual zen quote.
 */
@Composable
fun DeskCalendarCard(viewModel: WallpaperViewModel) {
    val language by viewModel.language.collectAsState()
    
    // Calendar configurations
    val calendar = Calendar.getInstance()
    val dayOfMonth = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
    
    val monthFormatZh = SimpleDateFormat("M月", Locale.CHINA)
    val monthFormatEn = SimpleDateFormat("MMMM", Locale.ENGLISH)
    val monthStr = if (language == "zh") monthFormatZh.format(calendar.time) else monthFormatEn.format(calendar.time)
    
    val weekdayFormatZh = SimpleDateFormat("EEEE", Locale.CHINA)
    val weekdayFormatEn = SimpleDateFormat("EEEE", Locale.ENGLISH)
    val weekdayStr = if (language == "zh") weekdayFormatZh.format(calendar.time) else weekdayFormatEn.format(calendar.time)
    
    val yearStr = calendar.get(Calendar.YEAR).toString()

    // Deterministic selection of visual greetings depending on calendar day
    val quotes = listOf(
        Pair("每一次抬头，都是一场完美的视觉奇遇。", "Every look is a seamless visual escape."),
        Pair("墨染山河，生活如一幅隽永的画卷。", "With colors brushed, life scrolls like an eternal canvas."),
        Pair("眼中有山河，心中有大美。", "Gaze upon nature, embrace the beauty inside."),
        Pair("极简是终极的复杂，黑白是纯正的永恒。", "Minimalism is the ultimate polish; monochrome is timeless."),
        Pair("在平淡的日子里，寻找一抹亮眼的光影。", "Amid standard routines, discover a brilliant glimpse of light."),
        Pair("美不假外求，皆蕴藏在细微之处。", "Elegance is never distant; it lives in the details."),
        Pair("宇宙星河璀璨，皆是造物给你的背景。", "The bright cosmic sky acts as a backdrop crafted for you."),
        Pair("温柔的光影里，住着一整天的美好心情。", "Gentle light hosts a whole day of peaceful mind.")
    )
    val quoteIndex = calendar.get(Calendar.DAY_OF_MONTH) % quotes.size
    val selectedQuote = quotes[quoteIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = CardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Desk Calendar Header (Ring binder aesthetics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Year and Weekday row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekdayStr,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$monthStr, $yearStr",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large desk calendar date
            Text(
                text = dayOfMonth,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displayLarge
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Dynamic Bilingual Quote
            Text(
                text = selectedQuote.first,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedQuote.second,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun CardBorder(): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )
}

fun getLunarDateHelper(calendar: Calendar): String {
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    return if (month == 6) {
        when (day) {
            1 -> "四月十六"
            2 -> "四月十七"
            3 -> "四月十八"
            4 -> "四月十九"
            5 -> "四月二十"
            6 -> "四月廿一"
            7 -> "四月廿二"
            8 -> "四月廿三"
            9 -> "四月廿四"
            10 -> "四月廿五"
            11 -> "四月廿六"
            12 -> "四月廿七"
            13 -> "四月廿八"
            14 -> "四月廿九"
            15 -> "五月初一"
            16 -> "五月初二"
            17 -> "五月初三"
            18 -> "五月初四"
            19 -> "五月初五"
            20 -> "五月初六"
            21 -> "五月初七"
            22 -> "五月初八"
            23 -> "五月初九"
            24 -> "五月初十"
            25 -> "五月十一"
            26 -> "五月十二"
            27 -> "五月十三"
            28 -> "五月十四"
            29 -> "五月十五"
            30 -> "五月十六"
            else -> "五月十七"
        }
    } else if (month == 5) {
        when (day) {
            31 -> "四月十五"
            else -> "三月十五"
        }
    } else {
        "五月初二"
    }
}
