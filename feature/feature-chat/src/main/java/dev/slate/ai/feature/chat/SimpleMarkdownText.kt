package dev.slate.ai.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Syntax highlighting colors
private val CodeKeyword = Color(0xFFA8C4D4)    // ice-blue
private val CodeString = Color(0xFF81C784)      // green
private val CodeComment = Color(0xFF6E6E6E)     // gray
private val CodeNumber = Color(0xFFFFB74D)      // orange
private val CodeDefault = Color(0xFFD4D4D4)     // light gray
private val CodeBackground = Color(0xFF1A1A1A)  // near black

@Composable
fun SimpleMarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseBlocks(text) }
    val context = LocalContext.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(block = block, context = context)
                }
                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text) { parseInlineFormatting(block.text, color) }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is MarkdownBlock.Header -> {
                    Text(
                        text = remember(block.text) { parseInlineFormatting(block.text, color) },
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                is MarkdownBlock.BulletList -> {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "  •  ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                            )
                            Text(
                                text = remember(item) { parseInlineFormatting(item, color) },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MarkdownBlock.NumberedList -> {
                    block.items.forEachIndexed { idx, item ->
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "  ${idx + 1}.  ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                            )
                            Text(
                                text = remember(item) { parseInlineFormatting(item, color) },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MarkdownBlock.Table -> {
                    MarkdownTableView(block = block, textColor = color)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableView(block: MarkdownBlock.Table, textColor: Color) {
    val headerBg = Color(0xFF1E2A32)
    val rowBgAlt = Color(0xFF1A1A1A).copy(alpha = 0.3f)
    val borderColor = Color(0xFF2E2E2E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141414))
            .horizontalScroll(rememberScrollState()),
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            block.headers.forEachIndexed { idx, header ->
                if (idx > 0) Spacer(Modifier.width(1.dp).height(20.dp).background(borderColor))
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
                )
            }
        }

        // Data rows
        block.rows.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (rowIdx % 2 == 1) Modifier.background(rowBgAlt) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                row.forEachIndexed { idx, cell ->
                    if (idx > 0) Spacer(Modifier.width(1.dp).height(18.dp).background(borderColor))
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(block: MarkdownBlock.CodeBlock, context: Context) {
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBackground),
    ) {
        // Header with language label + copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252525))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.language.ifEmpty { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("code", block.code))
                    copied = true
                    scope.launch { delay(1500); copied = false }
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (copied) "Copied" else "Copy code",
                    tint = if (copied) CodeString else Color(0xFF9E9E9E),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Syntax-highlighted code
        val highlighted = remember(block.code, block.language) {
            highlightSyntax(block.code, block.language)
        }
        Text(
            text = highlighted,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .horizontalScroll(rememberScrollState()),
        )
    }
}

// --- Block parser ---

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
    data class Header(val text: String, val level: Int) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class NumberedList(val items: List<String>) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private fun parseBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var i = 0
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        val content = paragraphBuffer.toString().trim()
        if (content.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(content))
        }
        paragraphBuffer.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()

        when {
            // Code block
            trimmed.startsWith("```") -> {
                flushParagraph()
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), lang))
                i++ // skip closing ```
            }

            // Header
            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("### "), 3))
                i++
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("## "), 2))
                i++
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("# "), 1))
                i++
            }

            // Bullet list item
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trimStart()
                    if (t.startsWith("- ") || t.startsWith("* ")) {
                        items.add(t.substring(2))
                        i++
                    } else break
                }
                blocks.add(MarkdownBlock.BulletList(items))
            }

            // Numbered list
            trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trimStart()
                    val match = Regex("^\\d+\\.\\s(.*)").find(t)
                    if (match != null) {
                        items.add(match.groupValues[1])
                        i++
                    } else break
                }
                blocks.add(MarkdownBlock.NumberedList(items))
            }

            // Table (lines with | pipes, followed by separator |---|)
            trimmed.contains('|') && i + 1 < lines.size && lines[i + 1].trimStart().matches(Regex("^\\|?[\\s-:|]+\\|[\\s-:|]*$")) -> {
                flushParagraph()
                val headerCells = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                i += 2 // skip header + separator
                val dataRows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].contains('|')) {
                    val cells = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (cells.isNotEmpty()) dataRows.add(cells)
                    i++
                }
                blocks.add(MarkdownBlock.Table(headerCells, dataRows))
            }

            // Blank line
            line.isBlank() && paragraphBuffer.isNotEmpty() -> {
                flushParagraph()
                i++
            }

            // Regular text
            else -> {
                if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append("\n")
                paragraphBuffer.append(line)
                i++
            }
        }
    }
    flushParagraph()
    return blocks
}

// --- Inline formatting ---

private fun parseInlineFormatting(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Inline code: `code`
                text[i] == '`' && !text.substring(i).startsWith("```") -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = CodeKeyword,
                            background = Color(0xFF2A2A2A),
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                        i++
                    }
                }

                // Strikethrough: ~~text~~
                i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = defaultColor,
                        )) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                        i++
                    }
                }

                // Bold+Italic: ***text***
                i + 2 < text.length && text.substring(i).startsWith("***") -> {
                    val end = text.indexOf("***", i + 3)
                    if (end > i) {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = defaultColor,
                        )) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                        i++
                    }
                }

                // Bold: **text**
                i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                        i++
                    }
                }

                // Italic: *text*
                text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i && end < text.length) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                        i++
                    }
                }

                // Regular text
                else -> {
                    withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                    i++
                }
            }
        }
    }
}

// --- Syntax highlighting ---

private val KEYWORDS = setOf(
    // Kotlin/Java
    "fun", "val", "var", "class", "object", "interface", "override", "private", "public",
    "protected", "internal", "abstract", "open", "data", "sealed", "enum", "companion",
    "import", "package", "return", "if", "else", "when", "for", "while", "do", "break",
    "continue", "try", "catch", "finally", "throw", "suspend", "coroutine", "null",
    "true", "false", "this", "super", "is", "in", "as", "by", "init", "constructor",
    // Python
    "def", "lambda", "yield", "from", "with", "assert", "pass", "raise", "except",
    "global", "nonlocal", "async", "await", "None", "True", "False", "and", "or", "not",
    // JavaScript/TypeScript
    "function", "const", "let", "new", "typeof", "instanceof", "void", "delete",
    "export", "default", "switch", "case", "async", "await", "undefined", "NaN",
    // General
    "int", "float", "double", "long", "boolean", "string", "char", "byte", "void",
    "static", "final", "extends", "implements", "struct", "typedef", "sizeof",
)

private fun highlightSyntax(code: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            if (lineIdx > 0) append("\n")
            highlightLine(line, language)
        }
    }
}

private fun AnnotatedString.Builder.highlightLine(line: String, language: String) {
    val commentPrefix = when {
        language in setOf("python", "py", "bash", "sh", "ruby", "yaml", "yml") -> "#"
        else -> "//"
    }

    // Check for line comment
    val commentIdx = line.indexOf(commentPrefix)
    val codePart = if (commentIdx >= 0) line.substring(0, commentIdx) else line
    val commentPart = if (commentIdx >= 0) line.substring(commentIdx) else null

    // Highlight code part
    highlightCodeSegment(codePart)

    // Highlight comment
    if (commentPart != null) {
        withStyle(SpanStyle(color = CodeComment, fontStyle = FontStyle.Italic)) {
            append(commentPart)
        }
    }
}

private fun AnnotatedString.Builder.highlightCodeSegment(code: String) {
    var i = 0
    while (i < code.length) {
        when {
            // String literals (double quotes)
            code[i] == '"' -> {
                val end = code.indexOf('"', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(color = CodeString)) {
                        append(code.substring(i, end + 1))
                    }
                    i = end + 1
                } else {
                    withStyle(SpanStyle(color = CodeString)) { append(code.substring(i)) }
                    i = code.length
                }
            }

            // String literals (single quotes)
            code[i] == '\'' -> {
                val end = code.indexOf('\'', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(color = CodeString)) {
                        append(code.substring(i, end + 1))
                    }
                    i = end + 1
                } else {
                    withStyle(SpanStyle(color = CodeString)) { append(code.substring(i)) }
                    i = code.length
                }
            }

            // Numbers
            code[i].isDigit() -> {
                val start = i
                while (i < code.length && (code[i].isDigit() || code[i] == '.' || code[i] == 'f' || code[i] == 'L')) i++
                withStyle(SpanStyle(color = CodeNumber)) {
                    append(code.substring(start, i))
                }
            }

            // Words (keywords or identifiers)
            code[i].isLetter() || code[i] == '_' -> {
                val start = i
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                val word = code.substring(start, i)
                if (word in KEYWORDS) {
                    withStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                } else {
                    withStyle(SpanStyle(color = CodeDefault)) {
                        append(word)
                    }
                }
            }

            // Everything else (operators, punctuation, whitespace)
            else -> {
                withStyle(SpanStyle(color = CodeDefault)) { append(code[i]) }
                i++
            }
        }
    }
}
