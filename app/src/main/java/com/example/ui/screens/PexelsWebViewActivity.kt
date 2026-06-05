package btm.m.todaywallpaper.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

class PexelsWebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PexelsWebViewScreen(
                    onBack = { finish() },
                    onKeyDetected = { key ->
                        // Return the detected API key to caller
                        val resultIntent = Intent().apply {
                            putExtra("detected_api_key", key)
                        }
                        setResult(RESULT_OK, resultIntent)
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PexelsWebViewScreen(
    onBack: () -> Unit,
    onKeyDetected: (String) -> Unit
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
    var currentUrl by remember { mutableStateOf("https://www.pexels.com/api/") }
    var isLoading by remember { mutableStateOf(true) }
    var capturedApiKey by remember { mutableStateOf<String?>(null) }
    var showManualExtract by remember { mutableStateOf(false) }
    var manualKeyInput by remember { mutableStateOf("") }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

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

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pexels API",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentUrl,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

            // WebView area
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef.value = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    url?.let { currentUrl = it }
                                    
                                    // Attempt to extract API key from the page using JS injection
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            // Try to find API key in common patterns
                                            var scripts = document.querySelectorAll('script');
                                            for (var i = 0; i < scripts.length; i++) {
                                                var text = scripts[i].textContent || scripts[i].innerText;
                                                var match = text.match(/api[_-]?key['":\s]*['"]([A-Za-z0-9]{50,})['"]/i);
                                                if (match) return match[1];
                                                var match2 = text.match(/authorization['":\s]*['"]([A-Za-z0-9]{50,})['"]/i);
                                                if (match2) return match2[1];
                                            }
                                            // Try to find key in page content / code blocks
                                            var allText = document.body.innerText;
                                            var keyMatch = allText.match(/([A-Za-z0-9]{56})/);
                                            if (keyMatch) return keyMatch[1];
                                            return '';
                                        })()
                                        """.trimIndent()
                                    ) { result ->
                                        val cleaned = result?.replace("\"", "")?.trim() ?: ""
                                        if (cleaned.isNotEmpty() && cleaned.length >= 40) {
                                            capturedApiKey = cleaned
                                            onKeyDetected(cleaned)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                    ): Boolean {
                                    // Allow all navigation within pexels.com
                                    val url = request?.url?.toString() ?: return false
                                    return if (url.contains("pexels.com")) {
                                        false // let WebView handle it
                                    } else {
                                        // For external links, open in external browser
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                        true
                                    }
                                }
                            }

                            loadUrl("https://www.pexels.com/api/")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom action bar with detected key or manual input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    if (capturedApiKey != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Detected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "API Key Detected!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${capturedApiKey!!.take(16)}...${capturedApiKey!!.takeLast(8)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { showManualExtract = !showManualExtract },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VpnKey,
                                contentDescription = "Manual",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "手动输入 API Key (Manual Entry)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showManualExtract && capturedApiKey == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualKeyInput,
                                onValueChange = { manualKeyInput = it },
                                placeholder = { Text("Paste your API Key here...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmed = manualKeyInput.trim()
                                    if (trimmed.isNotEmpty()) {
                                        capturedApiKey = trimmed
                                        onKeyDetected(trimmed)
                                    }
                                },
                                enabled = manualKeyInput.trim().isNotEmpty()
                            ) {
                                Text("OK", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "注册完成后 API Key 将自动保存，返回即可使用。\nAfter registering, the API Key will be auto-saved.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}