package btm.m.todaywallpaper.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.widget.enableMomentumTransparentWindow
import btm.m.todaywallpaper.ui.widget.momentumBackTransform
import btm.m.todaywallpaper.ui.widget.rememberMomentumPredictiveBack

class ApiKeysActivity : ComponentActivity() {
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMomentumTransparentWindow()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ApiKeysScreen(viewModel = viewModel, onBack = ::finish)
            }
        }
    }
}

@Composable
private fun ApiKeysScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val predictiveEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveMax by viewModel.predictiveBackMaxProgress.collectAsState()
    val pexelsKey by viewModel.pexelsApiKey.collectAsState()
    val pixabayKey by viewModel.pixabayApiKey.collectAsState()
    val wallhavenKey by viewModel.wallhavenApiKey.collectAsState()
    val deviantArtClientId by viewModel.deviantArtClientId.collectAsState()
    val deviantArtClientSecret by viewModel.deviantArtClientSecret.collectAsState()
    val nekosiaNsfw by viewModel.nekosiaNsfwEnabled.collectAsState()
    val wallhavenNsfw by viewModel.wallhavenNsfwEnabled.collectAsState()
    val deviantArtNsfw by viewModel.deviantArtNsfwEnabled.collectAsState()
    val blurNsfw by viewModel.blurNsfw.collectAsState()
    var ageGateSource by remember { mutableStateOf<NsfwSource?>(null) }
    var showWallhavenKeyRequired by remember { mutableStateOf(false) }
    var showDeviantArtCredentialsRequired by remember { mutableStateOf(false) }
    val backState = rememberMomentumPredictiveBack(predictiveEnabled, predictiveMax, onBack)

    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .momentumBackTransform(backState)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = viewModel.getTranslation("返回", "Back"),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = viewModel.getTranslation("API 设置", "API Settings"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ApiKeyCard(
                    serviceName = "Pexels",
                    key = pexelsKey,
                    registrationUrl = "https://www.pexels.com/api/",
                    hint = viewModel.getTranslation(
                        "长按此说明可快速打开 Pexels API 网页，按网页提示完成注册后，将 API Key 复制到这里。",
                        "Long press this note to open Pexels API. Register there, then paste the API Key here."
                    ),
                    enteredLabel = viewModel.getTranslation("已输入", "Entered"),
                    missingLabel = viewModel.getTranslation("未输入", "Not entered"),
                    onSave = viewModel::updatePexelsApiKey
                )
                ApiKeyCard(
                    serviceName = "Pixabay",
                    key = pixabayKey,
                    registrationUrl = "https://pixabay.com/api/docs/",
                    hint = viewModel.getTranslation(
                        "长按此说明可快速打开 Pixabay API 网页，按网页提示完成注册后，将 API Key 复制到这里。",
                        "Long press this note to open Pixabay API. Register there, then paste the API Key here."
                    ),
                    enteredLabel = viewModel.getTranslation("已输入", "Entered"),
                    missingLabel = viewModel.getTranslation("未输入", "Not entered"),
                    onSave = viewModel::updatePixabayApiKey
                )
                ApiKeyCard(
                    serviceName = "Wallhaven",
                    key = wallhavenKey,
                    registrationUrl = "https://wallhaven.cc/help/api",
                    hint = viewModel.getTranslation(
                        "Wallhaven 默认无需 API Key 即可获取 SFW 壁纸。开启 NSFW 浏览前必须填写 Key，并在 Wallhaven 账号设置中开启 NSFW 浏览权限。",
                        "Wallhaven does not require an API key for SFW wallpapers. NSFW browsing requires a key and NSFW access enabled in your Wallhaven account."
                    ),
                    enteredLabel = viewModel.getTranslation("已输入", "Entered"),
                    missingLabel = viewModel.getTranslation("未输入", "Not entered"),
                    onSave = viewModel::updateWallhavenApiKey,
                    expandedContent = {
                        ApiSwitchSettingRow(
                            label = viewModel.getTranslation("NSFW 内容", "NSFW Content"),
                            checked = wallhavenNsfw,
                            onCheckedChange = { enabled ->
                                if (enabled) ageGateSource = NsfwSource.WALLHAVEN
                                else viewModel.setWallhavenNsfwEnabled(false)
                            }
                        )
                        ApiSwitchSettingRow(
                            label = viewModel.getTranslation("NSFW 内容模糊", "Blur NSFW Content"),
                            checked = blurNsfw,
                            onCheckedChange = viewModel::setBlurNsfw
                        )
                    }
                )
                DeviantArtCredentialCard(
                    clientId = deviantArtClientId,
                    clientSecret = deviantArtClientSecret,
                    hint = viewModel.getTranslation(
                        "长按此说明可打开 DeviantArt 开发者页面。创建应用后，将 Client ID 和 Client Secret 分别填写到下方输入框。",
                        "Long press this note to open DeviantArt developers. Create an app, then enter its Client ID and Client Secret below."
                    ),
                    enteredLabel = viewModel.getTranslation("已输入", "Entered"),
                    missingLabel = viewModel.getTranslation("未输入", "Not entered"),
                    onSave = viewModel::updateDeviantArtCredentials,
                    expandedContent = {
                        ApiSwitchSettingRow(
                            label = viewModel.getTranslation("NSFW 内容", "NSFW Content"),
                            checked = deviantArtNsfw,
                            onCheckedChange = { enabled ->
                                if (enabled) ageGateSource = NsfwSource.DEVIANTART
                                else viewModel.setDeviantArtNsfwEnabled(false)
                            }
                        )
                    }
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    ApiSwitchSettingRow(
                        label = viewModel.getTranslation("Nekosia API NSFW 内容", "Nekosia API NSFW Content"),
                        checked = nekosiaNsfw,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        onCheckedChange = { enabled ->
                            if (enabled) ageGateSource = NsfwSource.NEKOSIA
                            else viewModel.setNekosiaNsfwEnabled(false)
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    ageGateSource?.let { source ->
        NsfwAgeGateDialog(
            onDismiss = { ageGateSource = null },
            onConfirm = {
                ageGateSource = null
                when (source) {
                    NsfwSource.NEKOSIA -> viewModel.setNekosiaNsfwEnabled(true)
                    NsfwSource.WALLHAVEN -> {
                        if (wallhavenKey.isBlank() || !viewModel.setWallhavenNsfwEnabled(true)) {
                            showWallhavenKeyRequired = true
                        }
                    }
                    NsfwSource.DEVIANTART -> {
                        if (deviantArtClientId.isBlank() || deviantArtClientSecret.isBlank() ||
                            !viewModel.setDeviantArtNsfwEnabled(true)
                        ) {
                            showDeviantArtCredentialsRequired = true
                        }
                    }
                }
            }
        )
    }
    if (showWallhavenKeyRequired) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showWallhavenKeyRequired = false },
            title = { Text(viewModel.getTranslation("需要 API Key", "API Key Required")) },
            text = {
                Text(viewModel.getTranslation(
                    "Wallhaven 的 NSFW 内容必须先填写 API Key，并在 Wallhaven 官网账号设置中开启 NSFW 浏览。",
                    "Wallhaven NSFW requires an API Key and NSFW browsing enabled on the Wallhaven account."
                ))
            },
            confirmButton = {
                TextButton(onClick = { showWallhavenKeyRequired = false }) {
                    Text(viewModel.getTranslation("知道了", "OK"))
                }
            }
        )
    }
    if (showDeviantArtCredentialsRequired) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeviantArtCredentialsRequired = false },
            title = { Text(viewModel.getTranslation("需要 DeviantArt 凭证", "DeviantArt Credentials Required")) },
            text = {
                Text(viewModel.getTranslation(
                    "DeviantArt 的 NSFW 内容必须先在此页面填写 Client ID 和 Client Secret。",
                    "DeviantArt NSFW requires a Client ID and Client Secret configured on this page."
                ))
            },
            confirmButton = {
                TextButton(onClick = { showDeviantArtCredentialsRequired = false }) {
                    Text(viewModel.getTranslation("知道了", "OK"))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviantArtCredentialCard(
    clientId: String,
    clientSecret: String,
    hint: String,
    enteredLabel: String,
    missingLabel: String,
    onSave: (String, String) -> Unit,
    expandedContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var draftClientId by remember(clientId, expanded) { mutableStateOf(clientId) }
    var draftClientSecret by remember(clientSecret, expanded) { mutableStateOf(clientSecret) }
    val configured = clientId.isNotBlank() && clientSecret.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(220))
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = { expanded = !expanded }),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Key, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "DeviantArt API",
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (configured) enteredLabel else missingLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = hint,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).combinedClickable(
                    onClick = { expanded = !expanded },
                    onLongClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.deviantart.com/developers/")))
                    }
                ),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 5 },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 5 }
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = draftClientId,
                        onValueChange = { draftClientId = it },
                        modifier = Modifier.fillMaxWidth().testTag("deviantart_client_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        placeholder = { Text("Client ID", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draftClientSecret,
                        onValueChange = { draftClientSecret = it },
                        modifier = Modifier.fillMaxWidth().testTag("deviantart_client_secret_input"),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(50),
                        placeholder = { Text("Client Secret", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilledIconButton(
                            onClick = {
                                draftClientId = clientId
                                draftClientSecret = clientSecret
                                expanded = false
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.Rounded.Close, "Cancel", Modifier.size(19.dp)) }
                        Spacer(Modifier.width(6.dp))
                        FilledIconButton(
                            onClick = {
                                onSave(draftClientId, draftClientSecret)
                                expanded = false
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Icon(Icons.Rounded.Check, "Save", Modifier.size(19.dp)) }
                    }
                    expandedContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ApiKeyCard(
    serviceName: String,
    key: String,
    registrationUrl: String,
    hint: String,
    enteredLabel: String,
    missingLabel: String,
    onSave: (String) -> Unit,
    expandedContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(key, expanded) { mutableStateOf(key) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(220))
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = { expanded = !expanded }),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$serviceName API Key",
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (key.isBlank()) missingLabel else enteredLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (key.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = hint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .combinedClickable(
                        onClick = { expanded = !expanded },
                        onLongClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(registrationUrl)))
                        }
                    ),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 5 },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 5 }
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${serviceName.lowercase()}_api_key_input"),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(50),
                            placeholder = { Text("API Key", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                draft = key
                                expanded = false
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel", Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        FilledIconButton(
                            onClick = {
                                onSave(draft)
                                expanded = false
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "Save", Modifier.size(19.dp))
                        }
                    }
                    expandedContent()
                }
            }
        }
    }
}

@Composable
private fun ApiSwitchSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}