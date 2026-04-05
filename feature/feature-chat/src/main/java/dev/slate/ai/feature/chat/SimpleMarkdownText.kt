package dev.slate.ai.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal markdown rendering: paragraphs, code blocks, inline code, bold, italic.
 * Not a full parser — just handles the most common patterns safely.
 */
@Composable
fun SimpleMarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseBlocks(text) }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState()),
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text) { parseInlineFormatting(block.text, color) }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
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
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), lang))
            i++ // skip closing ```
        } else if (line.isBlank() && paragraphBuffer.isNotEmpty()) {
            flushParagraph()
            i++
        } else {
            if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append("\n")
            paragraphBuffer.append(line)
            i++
        }
    }
    flushParagraph()
    return blocks
}

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
                // Regular text
                else -> {
                    withStyle(SpanStyle(color = defaultColor)) { append(text[i]) }
                    i++
                }
            }
        }
    }
}
