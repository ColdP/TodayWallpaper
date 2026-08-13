package btm.m.todaywallpaper.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.theme.isAppDarkTheme

@Composable
fun WallpaperScopeDialog(
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit,
    onAlways: (WallpaperViewModel.WallpaperScope) -> Unit,
    onJustOnce: (WallpaperViewModel.WallpaperScope) -> Unit
) {
    var selectedScope by remember { mutableStateOf(WallpaperViewModel.WallpaperScope.HOME_SCREEN) }

    val darkTheme = isAppDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary

    var backProgress by remember { mutableStateOf(0f) }

    // Drag-to-dismiss state
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 200.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    PredictiveBackHandler { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                backProgress = backEvent.progress
                dragOffsetY = backEvent.progress * dismissThresholdPx
            }
            backProgress = 1f
            onDismiss()
        } catch (_: Exception) {
            backProgress = 0f
            dragOffsetY = 0f
        }
    }

    // Combined progress for visual effects
    val visualProgress = if (isDragging) {
        (dragOffsetY / dismissThresholdPx).coerceIn(0f, 1f)
    } else if (backProgress > 0f) {
        backProgress
    } else {
        0f
    }

    val dialogScale = 1f - (visualProgress * 0.04f)
    val dialogAlpha = 1f - (visualProgress * 0.25f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .graphicsLayer(
                    alpha = dialogAlpha
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume click to prevent dismiss */ }
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        translationY = dragOffsetY
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle indicator — swipe down to dismiss
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(24.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = {
                                        isDragging = false
                                        if (dragOffsetY > dismissThresholdPx * 0.35f) {
                                            onDismiss()
                                        } else {
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffsetY = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY =
                                            (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (isDragging) 0.5f else 0.3f
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = viewModel.getTranslation("设置壁纸范围", "Set Wallpaper Scope"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = viewModel.getTranslation(
                            "选择要将壁纸应用到哪个屏幕",
                            "Choose where to apply this wallpaper"
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Scope options
                    val scopes = listOf(
                        WallpaperViewModel.WallpaperScope.HOME_SCREEN,
                        WallpaperViewModel.WallpaperScope.LOCK_SCREEN,
                        WallpaperViewModel.WallpaperScope.BOTH
                    )

                    scopes.forEach { scope ->
                        val isSelected = selectedScope == scope
                        val bgAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0.08f else 0.03f,
                            animationSpec = tween(250),
                            label = "scopeBgAlpha"
                        )

                        val scopeLabel = viewModel.getTranslation(scope.zhLabel, scope.enLabel)
                        val scopeHint = when (scope) {
                            WallpaperViewModel.WallpaperScope.HOME_SCREEN -> viewModel.getTranslation(
                                "仅应用到主屏幕桌面",
                                "Home screen only"
                            )
                            WallpaperViewModel.WallpaperScope.LOCK_SCREEN -> viewModel.getTranslation(
                                "仅应用到锁屏界面",
                                "Lock screen only"
                            )
                            WallpaperViewModel.WallpaperScope.BOTH -> viewModel.getTranslation(
                                "同时应用到桌面和锁屏",
                                "Both home & lock screen"
                            )
                            else -> ""
                        }

                        val borderAlpha = if (isSelected) 0.2f else 0.06f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedScope = scope }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Check circle
                            val checkBgColor by animateColorAsState(
                                targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                animationSpec = tween(250),
                                label = "checkBg"
                            )
                            val checkContentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
                                animationSpec = tween(250),
                                label = "checkContent"
                            )

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(checkBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Checked",
                                        tint = checkContentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scopeLabel,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = scopeHint,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            // Right indicator dot
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.6f))
                                )
                            }
                        }

                        if (scope != scopes.last()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Two buttons: Always / Just Once
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // "Always" button
                        Button(
                            onClick = { onAlways(selectedScope) },
                            shape = RoundedCornerShape(percent = 50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor.copy(alpha = 0.12f),
                                contentColor = accentColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = viewModel.getTranslation("总是", "Always"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // "Just Once" button
                        Button(
                            onClick = { onJustOnce(selectedScope) },
                            shape = RoundedCornerShape(percent = 50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = if (darkTheme) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = viewModel.getTranslation("仅一次", "Just Once"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}