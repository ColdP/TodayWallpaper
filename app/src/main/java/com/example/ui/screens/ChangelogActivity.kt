package btm.m.todaywallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChangelogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val versionCode = intent.getIntExtra("versionCode", 0)
        val versionName = intent.getStringExtra("versionName") ?: ""
        val changelogUrl = intent.getStringExtra("changelogUrl") ?: ""

        setContent {
            MyApplicationTheme {
                ChangelogScreen(
                    versionCode = versionCode,
                    versionName = versionName,
                    changelogUrl = changelogUrl,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun ChangelogScreen(
    versionCode: Int,
    versionName: String,
    changelogUrl: String,
    onBack: () -> Unit
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
    val alphaVal = 1f - (backProgress * 0.2f)
    val cornerRadius = (backProgress * 24).dp

    val coroutineScope = rememberCoroutineScope()

    // Changelog loading state
    var isLoading by remember { mutableStateOf(true) }
    var markdownContent by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(changelogUrl) {
        if (changelogUrl.isBlank()) {
            isLoading = false
            loadError = "No changelog URL provided"
            return@LaunchedEffect
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url(changelogUrl)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        markdownContent = body
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadError = "HTTP ${response.code}"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadError = e.localizedMessage ?: "Failed to load changelog"
                    isLoading = false
                }
            }
        }
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
                    translationX = with(LocalDensity.current) { translationXDp.toPx() },
                    alpha = alphaVal,
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
                        text = "更新日志 v$versionName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "加载中...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else if (loadError != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载失败: $loadError",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Markdown rendered content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                val parsedBlocks = parseMarkdown(markdownContent)
                                parsedBlocks.forEach { block ->
                                    MarkdownBlock(block = block)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ========================
// Simple Markdown Parser
// ========================

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class HorizontalRule(val dummy: Unit = Unit) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
}

fun parseMarkdown(input: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = input.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Empty line
        if (line.isBlank()) {
            i++
            continue
        }

        // Code block (```)
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // Horizontal rule (---, ***, ___)
        if (line.trim().matches(Regex("^[-*_]{3,}$"))) {
            blocks.add(MarkdownBlock.HorizontalRule())
            i++
            continue
        }

        // Heading (#, ##, ###, etc.)
        val headingMatch = Regex("^(#{1,6})\\s+(.+)").matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.Heading(level, text))
            i++
            continue
        }

        // Blockquote (>)
        if (line.trimStart().startsWith("> ")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().startsWith("> ")) {
                quoteLines.add(lines[i].trimStart().removePrefix("> "))
                i++
            }
            blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
            continue
        }

        // Unordered list (- or * or +)
        if (line.trimStart().matches(Regex("^[\\-\\*\\+]\\s+.*"))) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^[\\-\\*\\+]\\s+.*"))) {
                items.add(lines[i].trimStart().replaceFirst(Regex("^[\\-\\*\\+]\\s+"), ""))
                i++
            }
            blocks.add(MarkdownBlock.BulletList(items))
            continue
        }

        // Ordered list (1. 2. 3.)
        if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                items.add(lines[i].trimStart().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
                i++
            }
            blocks.add(MarkdownBlock.NumberedList(items))
            continue
        }

        // Paragraph (collect consecutive non-blank lines)
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("#") &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].trim().matches(Regex("^[-*_]{3,}$")) &&
            !lines[i].trimStart().startsWith("> ") &&
            !lines[i].trimStart().matches(Regex("^[\\-\\*\\+]\\s+.*")) &&
            !lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))
        ) {
            paragraphLines.add(lines[i])
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
        }
    }

    return blocks
}

@Composable
fun MarkdownBlock(block: MarkdownBlock) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    when (block) {
        is MarkdownBlock.Heading -> {
            val (fontSize, topPadding) = when (block.level) {
                1 -> 24.sp to 16.dp
                2 -> 20.sp to 14.dp
                3 -> 17.sp to 12.dp
                else -> 15.sp to 10.dp
            }
            Spacer(modifier = Modifier.height(topPadding))
            Text(
                text = parseInlineMarkdown(block.text),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor,
                lineHeight = (fontSize.value * 1.4f).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        is MarkdownBlock.Paragraph -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = parseInlineMarkdown(block.text),
                fontSize = 14.sp,
                color = onSurfaceColor,
                lineHeight = 22.sp
            )
        }

        is MarkdownBlock.BulletList -> {
            Spacer(modifier = Modifier.height(8.dp))
            block.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "•  ",
                        fontSize = 14.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = parseInlineMarkdown(item),
                        fontSize = 14.sp,
                        color = onSurfaceColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        is MarkdownBlock.NumberedList -> {
            Spacer(modifier = Modifier.height(8.dp))
            block.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "${index + 1}.  ",
                        fontSize = 14.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = parseInlineMarkdown(item),
                        fontSize = 14.sp,
                        color = onSurfaceColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        is MarkdownBlock.HorizontalRule -> {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
        }

        is MarkdownBlock.CodeBlock -> {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = surfaceVariantColor.copy(alpha = 0.5f)
            ) {
                Text(
                    text = block.code,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }

        is MarkdownBlock.BlockQuote -> {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(IntrinsicSize.Max)
                        .background(primaryColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = parseInlineMarkdown(block.text),
                    fontSize = 14.sp,
                    color = secondaryColor,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Parse inline markdown: **bold**, *italic*, `code`, ~~strikethrough~~, [text](url)
 */
@Composable
fun parseInlineMarkdown(text: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            // Bold: **text** or __text__
            val boldMatch = Regex("^\\*\\*(.+?)\\*\\*|^__(.+?)__").find(remaining)
            if (boldMatch != null) {
                val content = boldMatch.groupValues[1].ifEmpty { boldMatch.groupValues[2] }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(content)
                }
                remaining = remaining.substring(boldMatch.range.last + 1)
                continue
            }

            // Italic: *text* or _text_
            val italicMatch = Regex("^\\*(.+?)\\*|^_(.+?)_").find(remaining)
            if (italicMatch != null) {
                val content = italicMatch.groupValues[1].ifEmpty { italicMatch.groupValues[2] }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(content)
                }
                remaining = remaining.substring(italicMatch.range.last + 1)
                continue
            }

            // Strikethrough: ~~text~~
            val strikeMatch = Regex("^~~(.+?)~~").find(remaining)
            if (strikeMatch != null) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(strikeMatch.groupValues[1])
                }
                remaining = remaining.substring(strikeMatch.range.last + 1)
                continue
            }

            // Inline code: `text`
            val codeMatch = Regex("^`(.+?)`").find(remaining)
            if (codeMatch != null) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    background = surfaceVariantColor.copy(alpha = 0.7f)
                )) {
                    append(codeMatch.groupValues[1])
                }
                remaining = remaining.substring(codeMatch.range.last + 1)
                continue
            }

            // Link: [text](url)
            val linkMatch = Regex("^\\[(.+?)\\]\\((.+?)\\)").find(remaining)
            if (linkMatch != null) {
                val linkText = linkMatch.groupValues[1]
                val url = linkMatch.groupValues[2]
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(linkText)
                }
                pop()
                remaining = remaining.substring(linkMatch.range.last + 1)
                continue
            }

            // Regular text - find next special character
            val nextSpecial = remaining.indexOfFirst { it == '*' || it == '_' || it == '`' || it == '~' || it == '[' }
            if (nextSpecial > 0) {
                append(remaining.substring(0, nextSpecial))
                remaining = remaining.substring(nextSpecial)
            } else if (nextSpecial == 0) {
                // Couldn't match any pattern, just append the character
                append(remaining[0])
                remaining = remaining.substring(1)
            } else {
                append(remaining)
                remaining = ""
            }
        }
    }
}