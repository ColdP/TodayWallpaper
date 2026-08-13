package btm.m.todaywallpaper.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

@Composable
fun ProfileOverlay(
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit
) {
    val colors = CreateCollectionTokens.colors()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentUsername by viewModel.username.collectAsState()
    val currentAvatarUrl by viewModel.avatarUrl.collectAsState()
    val currentProfileSubtitle by viewModel.profileSubtitle.collectAsState()

    var inputUsername by remember { mutableStateOf(currentUsername) }
    var inputAvatarUrl by remember { mutableStateOf(currentAvatarUrl.orEmpty()) }
    var inputSubtitle by remember { mutableStateOf(currentProfileSubtitle) }
    var panelDragOffset by remember { mutableStateOf(0f) }
    val animatedPanelDragOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = panelDragOffset,
        animationSpec = if (panelDragOffset == 0f) {
            androidx.compose.animation.core.tween(260)
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "profile_panel_drag"
    )

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val timeStamp = java.text.SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    val fileName = "avatar_$timeStamp.jpg"
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("avatar_") && file.name.endsWith(".jpg")) {
                            file.delete()
                        }
                    }
                    val avatarFile = java.io.File(context.filesDir, fileName)
                    java.io.FileOutputStream(avatarFile).use { output ->
                        inputStream.use { input -> input.copyTo(output) }
                    }
                    inputAvatarUrl = avatarFile.absolutePath
                    Toast.makeText(
                        context,
                        viewModel.getTranslation(
                            "已加载本地图片预览",
                            "Local image loaded for preview"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (error: Exception) {
                error.printStackTrace()
                Toast.makeText(
                    context,
                    viewModel.getTranslation(
                        "无法读取该图片文件",
                        "Failed to load selected image"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun closeOverlay() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    BackHandler(enabled = true, onBack = ::closeOverlay)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                indication = null,
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                onClick = ::closeOverlay
            )
            .semantics { contentDescription = "自定义个人档案" }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.51f)
                .heightIn(min = 340.dp)
                .graphicsLayer { translationY = animatedPanelDragOffset }
                .shadow(22.dp, CreateCollectionTokens.panelShape)
                .clip(CreateCollectionTokens.panelShape)
                .background(colors.panel)
                .border(
                    BorderStroke(1.dp, colors.panelBorder),
                    CreateCollectionTokens.panelShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                    onClick = {}
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .padding(top = 14.dp)
                    .width(72.dp)
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                panelDragOffset = (panelDragOffset + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (panelDragOffset > 110.dp.toPx()) {
                                    closeOverlay()
                                } else {
                                    panelDragOffset = 0f
                                }
                            },
                            onDragCancel = { panelDragOffset = 0f }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.handle)
                )
            }

            ProfileForm(
                username = inputUsername,
                avatarUrl = inputAvatarUrl,
                subtitle = inputSubtitle,
                onUsernameChange = { inputUsername = it },
                onSubtitleChange = { inputSubtitle = it },
                onPickAvatar = { avatarPickerLauncher.launch("image/*") },
                onSave = {
                    // Keep the existing rule: an empty username does not overwrite
                    // the saved name, while avatar and subtitle are still persisted.
                    if (inputUsername.isNotBlank()) {
                        viewModel.updateUsername(inputUsername.trim())
                    }
                    viewModel.updateAvatar(inputAvatarUrl.trim().ifEmpty { null })
                    viewModel.updateProfileSubtitle(inputSubtitle.trim())
                    closeOverlay()
                },
                onCancel = ::closeOverlay,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ProfileForm(
    username: String,
    avatarUrl: String,
    subtitle: String,
    onUsernameChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WallpaperViewModel
) {
    val colors = CreateCollectionTokens.colors()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = CreateCollectionTokens.contentHorizontal,
                        end = CreateCollectionTokens.contentHorizontal,
                        bottom = 12.dp
                    ),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = viewModel.getTranslation("自定义个人档案", "Custom Profile"),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.field)
                        .border(
                            1.dp,
                            colors.fieldBorder,
                            CircleShape
                        )
                        .clickable(onClick = onPickAvatar)
                        .semantics {
                            contentDescription = viewModel.getTranslation(
                                "点击上传头像",
                                "Upload avatar"
                            )
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = viewModel.getTranslation(
                                "头像预览",
                                "Avatar preview"
                            ),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AlbumBasicInput(
                        value = username,
                        onValueChange = onUsernameChange,
                        placeholder = viewModel.getTranslation("用户名", "Username"),
                        label = viewModel.getTranslation("用户名", "Username"),
                        imeAction = ImeAction.Next,
                        modifier = Modifier.testTag("dialog_profile_username_input")
                    )
                    AlbumBasicInput(
                        value = subtitle,
                        onValueChange = onSubtitleChange,
                        placeholder = viewModel.getTranslation(
                            "个性签名",
                            "Custom subtitle"
                        ),
                        label = viewModel.getTranslation(
                            "个性签名",
                            "Custom subtitle"
                        ),
                        imeAction = ImeAction.Done,
                        modifier = Modifier.testTag("dialog_profile_subtitle_input")
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CreateCollectionTokens.contentHorizontal,
                    end = CreateCollectionTokens.contentHorizontal,
                    top = CreateCollectionTokens.contentGap,
                    bottom = CreateCollectionTokens.contentBottom
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OverlayTextButton(
                text = viewModel.getTranslation("保存", "Save"),
                onClick = onSave,
                primary = true,
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("dialog_profile_confirm_btn")
            )
            OverlayTextButton(
                text = viewModel.getTranslation("取消", "Cancel"),
                onClick = onCancel,
                primary = false,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
