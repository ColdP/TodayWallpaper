package btm.m.todaywallpaper.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import java.util.Calendar

class AutoSwitchWallpaperActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AutoSwitchWallpaperScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AutoSwitchWallpaperScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()

    // Auto-switch preference states
    var autoSwitchEnabled by remember { mutableStateOf(false) }
    var showExperimentalDialog by remember { mutableStateOf(false) }
    var experimentalCountdown by remember { mutableIntStateOf(5) }
    var switchMode by remember { mutableStateOf("interval") } // "interval" or "daily"
    var intervalHours by remember { mutableIntStateOf(1) }
    var intervalMinutes by remember { mutableIntStateOf(0) }
    var dailyHour by remember { mutableIntStateOf(8) }
    var dailyMinute by remember { mutableIntStateOf(0) }
    var selectedSourceType by remember { mutableStateOf("current") } // "current", built-in key, "custom_pexels_xxx", "collection_xxx"

    // Load saved preferences
    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        autoSwitchEnabled = sp.getBoolean("auto_switch_enabled", false)
        switchMode = sp.getString("auto_switch_mode", "interval") ?: "interval"
        intervalHours = sp.getInt("auto_switch_interval_hours", 1)
        intervalMinutes = sp.getInt("auto_switch_interval_minutes", 0)
        dailyHour = sp.getInt("auto_switch_daily_hour", 8)
        dailyMinute = sp.getInt("auto_switch_daily_minute", 0)
        selectedSourceType = sp.getString("auto_switch_source_type", "current") ?: "current"
    }

    fun savePreferences() {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("auto_switch_enabled", autoSwitchEnabled)
            .putString("auto_switch_mode", switchMode)
            .putInt("auto_switch_interval_hours", intervalHours)
            .putInt("auto_switch_interval_minutes", intervalMinutes)
            .putInt("auto_switch_daily_hour", dailyHour)
            .putInt("auto_switch_daily_minute", dailyMinute)
            .putString("auto_switch_source_type", selectedSourceType)
            .apply()
    }

    fun scheduleAutoSwitch() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AutoSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = if (switchMode == "interval") {
            val intervalMillis = (intervalHours * 3600L + intervalMinutes * 60L) * 1000L
            System.currentTimeMillis() + intervalMillis
        } else {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, dailyHour)
                set(Calendar.MINUTE, dailyMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            calendar.timeInMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    // Fall back to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    fun cancelAutoSwitch() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AutoSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // Status bar styling
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

    // Predictive back gesture
    var backProgress by remember { mutableStateOf(0f) }
    var isBackSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        backProgress = 0f
        isBackSwiping = false
    }

    androidx.activity.compose.PredictiveBackHandler { progressFlow ->
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

    // Built-in options (same as StyleSettingActivity)
    val basicOptions = remember {
        listOf(
            "PexelsCurated" to ("Pexels 山川每日精选" to "Pexels Curated Scenery"),
            "PexelsSpace" to ("Pexels 浩瀚太空星际" to "Pexels Galactic Space"),
            "PexelsMinimalist" to ("Pexels 优雅留白极简" to "Pexels Minimal Art"),
            "PexelsNature" to ("Pexels 壮丽山川自然" to "Pexels Natural Planet"),
            "Nekosia:cute" to ("Nekosia 萌系治愈二次元" to "Nekosia Kawaii Cute"),
            "Nekosia:girl" to ("Nekosia 唯美二次元少女" to "Nekosia Beauty Girl"),
            "Nekosia:maid" to ("Nekosia 黑白经典女仆" to "Nekosia Classic Maid"),
            "Nekosia:vtuber" to ("Nekosia 虚拟次元偶像" to "Nekosia VTubers")
        )
    }

    Box(
        modifier = Modifier
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
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = viewModel.getTranslation("自动切换桌面壁纸", "Auto Switch Wallpaper"),
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
                    // ===== Experimental Feature Warning =====
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = viewModel.getTranslation(
                                    "这是实验性功能，依赖系统定时任务机制，可能因设备厂商限制或系统省电策略而无法正常运行，请自行承担使用风险。",
                                    "This is an experimental feature. It relies on system scheduling and may not work properly due to device vendor restrictions or battery optimization policies. Use at your own risk."
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ===== Auto-switch Master Toggle =====
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        .clickable {
                                    if (!autoSwitchEnabled) {
                                        experimentalCountdown = 5
                                        showExperimentalDialog = true
                                    } else {
                                        autoSwitchEnabled = false
                                        savePreferences()
                                        cancelAutoSwitch()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = "Auto Switch",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = viewModel.getTranslation("自动切换壁纸", "Auto Switch Wallpaper"),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (autoSwitchEnabled) {
                                            viewModel.getTranslation("已开启", "Enabled")
                                        } else {
                                            viewModel.getTranslation("已关闭", "Disabled")
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Switch(
                                checked = autoSwitchEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        // Show experimental warning dialog with 5s countdown
                                        experimentalCountdown = 5
                                        showExperimentalDialog = true
                                    } else {
                                        autoSwitchEnabled = false
                                        savePreferences()
                                        cancelAutoSwitch()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ===== Switch Mode Selection =====
                    if (autoSwitchEnabled) {
                        Text(
                            text = viewModel.getTranslation("切换模式", "Switch Mode"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

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
                                    .padding(vertical = 8.dp)
                            ) {
                                // Interval mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            switchMode = "interval"
                                            savePreferences()
                                            scheduleAutoSwitch()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Interval",
                                            tint = if (switchMode == "interval") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = viewModel.getTranslation("间隔切换", "Interval Switch"),
                                                fontSize = 15.sp,
                                                fontWeight = if (switchMode == "interval") FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (switchMode == "interval") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = viewModel.getTranslation("每隔指定时间自动切换", "Switch every specified time period"),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    if (switchMode == "interval") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF007AFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )

                                // Daily mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            switchMode = "daily"
                                            savePreferences()
                                            scheduleAutoSwitch()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Daily",
                                            tint = if (switchMode == "daily") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = viewModel.getTranslation("每日切换", "Daily Switch"),
                                                fontSize = 15.sp,
                                                fontWeight = if (switchMode == "daily") FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (switchMode == "daily") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = viewModel.getTranslation("每天定时自动切换", "Switch at a fixed time every day"),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    if (switchMode == "daily") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF007AFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ===== Time Configuration =====
                        Text(
                            text = viewModel.getTranslation("时间设置", "Time Setting"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            if (switchMode == "interval") {
                                // Interval time picker
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = viewModel.getTranslation("切换间隔", "Switch Interval"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Hours
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = viewModel.getTranslation("小时", "Hours"),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        if (intervalHours > 0) {
                                                            intervalHours--
                                                            savePreferences()
                                                            scheduleAutoSwitch()
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    text = "$intervalHours",
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.widthIn(min = 48.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                                IconButton(
                                                    onClick = {
                                                        if (intervalHours < 24) {
                                                            intervalHours++
                                                            savePreferences()
                                                            scheduleAutoSwitch()
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(32.dp))

                                        // Minutes
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = viewModel.getTranslation("分钟", "Minutes"),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        val newMin = intervalMinutes - 5
                                                        if (newMin >= 0) {
                                                            intervalMinutes = newMin
                                                        } else {
                                                            intervalMinutes = 55
                                                            if (intervalHours > 0) intervalHours--
                                                        }
                                                        savePreferences()
                                                        scheduleAutoSwitch()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    text = String.format("%02d", intervalMinutes),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.widthIn(min = 48.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val newMin = intervalMinutes + 5
                                                        if (newMin < 60) {
                                                            intervalMinutes = newMin
                                                        } else {
                                                            intervalMinutes = 0
                                                            if (intervalHours < 24) intervalHours++
                                                        }
                                                        savePreferences()
                                                        scheduleAutoSwitch()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quick preset buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val presets = listOf(
                                            0 to 30,
                                            1 to 0,
                                            2 to 0,
                                            4 to 0,
                                            8 to 0
                                        )
                                        presets.forEach { (h, m) ->
                                            val label = if (h == 0) {
                                                "${m}${viewModel.getTranslation("分钟", "min")}"
                                            } else if (m == 0) {
                                                "${h}${viewModel.getTranslation("小时", "h")}"
                                            } else {
                                                "${h}h${m}m"
                                            }
                                            val isActive = intervalHours == h && intervalMinutes == m
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        intervalHours = h
                                                        intervalMinutes = m
                                                        savePreferences()
                                                        scheduleAutoSwitch()
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = viewModel.getTranslation(
                                            "⚠️ 间隔最少30分钟，避免过于频繁地切换壁纸影响使用体验",
                                            "⚠️ Minimum interval is 30 minutes to avoid excessive wallpaper switching"
                                        ),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        lineHeight = 15.sp
                                    )
                                }
                            } else {
                                // Daily time picker
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val timePickerDialog = TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    dailyHour = hourOfDay
                                                    dailyMinute = minute
                                                    savePreferences()
                                                    scheduleAutoSwitch()
                                                },
                                                dailyHour,
                                                dailyMinute,
                                                true // 24-hour format
                                            )
                                            timePickerDialog.show()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = viewModel.getTranslation("每日切换时间", "Daily Switch Time"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = viewModel.getTranslation("点击修改时间", "Tap to change time"),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Time",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format("%02d:%02d", dailyHour, dailyMinute),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ===== Wallpaper Source Selection =====
                        Text(
                            text = viewModel.getTranslation("壁纸来源", "Wallpaper Source"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        // "Current" option
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
                                    .padding(vertical = 8.dp)
                            ) {
                                StyleRowItem(
                                    title = viewModel.getTranslation("跟随首页当前风格", "Follow Current Homepage Style"),
                                    isSelected = selectedSourceType == "current",
                                    onClick = {
                                        selectedSourceType = "current"
                                        savePreferences()
                                        if (autoSwitchEnabled) scheduleAutoSwitch()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Built-in styles
                        Text(
                            text = viewModel.getTranslation("内置风格", "Built-in Styles"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

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
                                    .padding(vertical = 8.dp)
                            ) {
                                basicOptions.forEachIndexed { index, (key, titlePair) ->
                                    val title = if (currentLang == "zh") titlePair.first else titlePair.second
                                    StyleRowItem(
                                        title = title,
                                        isSelected = selectedSourceType == key,
                                        onClick = {
                                            selectedSourceType = key
                                            savePreferences()
                                            if (autoSwitchEnabled) scheduleAutoSwitch()
                                        }
                                    )
                                    if (index < basicOptions.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Categories
                        Text(
                            text = viewModel.getTranslation("自定义分类", "Custom Categories"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        val customItems = categories.filter { it.key.startsWith("custom_pexels_") }
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
                                    .padding(vertical = 8.dp)
                            ) {
                                if (customItems.isEmpty()) {
                                    Text(
                                        text = viewModel.getTranslation("暂无自定义分类", "No Custom Categories"),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp)
                                    )
                                } else {
                                    customItems.forEachIndexed { index, item ->
                                        val name = if (currentLang == "zh") item.zhTitle else item.enTitle
                                        StyleRowItem(
                                            title = name,
                                            isSelected = selectedSourceType == item.key,
                                            onClick = {
                                                selectedSourceType = item.key
                                                savePreferences()
                                                if (autoSwitchEnabled) scheduleAutoSwitch()
                                            }
                                        )
                                        if (index < customItems.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Collections
                        Text(
                            text = viewModel.getTranslation("自定义图集", "Custom Collections"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

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
                                    .padding(vertical = 8.dp)
                            ) {
                                if (collections.isEmpty()) {
                                    Text(
                                        text = viewModel.getTranslation("暂无自定义图集", "No Custom Collections"),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp)
                                    )
                                } else {
                                    collections.forEachIndexed { index, item ->
                                        StyleRowItem(
                                            title = item.name,
                                            isSelected = selectedSourceType == "collection_${item.id}",
                                            onClick = {
                                                selectedSourceType = "collection_${item.id}"
                                                savePreferences()
                                                if (autoSwitchEnabled) scheduleAutoSwitch()
                                            }
                                        )
                                        if (index < collections.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }

        // ===== Experimental Feature Confirmation Dialog =====
        if (showExperimentalDialog) {
            LaunchedEffect(showExperimentalDialog, experimentalCountdown) {
                if (showExperimentalDialog && experimentalCountdown > 0) {
                    kotlinx.coroutines.delay(1000L)
                    experimentalCountdown--
                }
            }

            AlertDialog(
                onDismissRequest = {
                    if (experimentalCountdown <= 0) {
                        showExperimentalDialog = false
                    }
                },
                title = {
                    Text(
                        text = viewModel.getTranslation("⚠️ 实验性功能确认", "⚠️ Experimental Feature"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = viewModel.getTranslation(
                                "自动切换壁纸功能依赖 Android 系统的定时任务机制。由于各厂商对后台任务有不同的限制和省电策略，该功能可能在以下情况失效：",
                                "Auto-switch wallpaper relies on Android's scheduled task system. Due to vendor-specific restrictions and battery optimization, this feature may not work in the following cases:"
                            ),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.getTranslation(
                                "• 设备重启后定时任务被清除\n• 系统省电模式阻止后台任务\n• 应用被厂商自启管理拦截\n• 锁屏状态下任务被延迟执行",
                                "• Schedule cleared after device reboot\n• Battery saver mode blocks background tasks\n• App blocked by vendor auto-start manager\n• Tasks delayed when screen is off"
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.getTranslation(
                                "开启此功能即表示您理解并接受上述风险。建议将应用加入电池优化白名单以获得最佳体验。",
                                "By enabling this feature, you understand and accept the risks above. We recommend adding this app to your battery optimization whitelist for the best experience."
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 17.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExperimentalDialog = false
                            autoSwitchEnabled = true
                            savePreferences()
                            scheduleAutoSwitch()
                        },
                        enabled = experimentalCountdown <= 0
                    ) {
                        Text(
                            text = if (experimentalCountdown > 0) {
                                viewModel.getTranslation("请阅读 (${experimentalCountdown}s)", "Read (${experimentalCountdown}s)")
                            } else {
                                viewModel.getTranslation("我已知悉，开启功能", "I Understand, Enable")
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExperimentalDialog = false }
                    ) {
                        Text(text = viewModel.getTranslation("取消", "Cancel"))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
