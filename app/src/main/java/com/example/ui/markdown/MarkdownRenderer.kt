package com.example.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TablesExtension

private fun preprocessMarkdown(input: String): String {
    val lines = input.split("\n")
    val processedLines = lines.map { line ->
        val leadingHashesMatch = Regex("^([#]{1,6})([^#\\s].*)").matchEntire(line)
        if (leadingHashesMatch != null) {
            val hashes = leadingHashesMatch.groupValues[1]
            val remaining = leadingHashesMatch.groupValues[2]
            hashes + " " + remaining
        } else {
            line
        }
    }
    return processedLines.joinToString("\n")
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val processedMarkdown = remember(markdown) { preprocessMarkdown(markdown) }
    val parser = remember {
        Parser.builder()
            .extensions(listOf(TablesExtension.create()))
            .build()
    }
    val document = remember(processedMarkdown) { parser.parse(processedMarkdown) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var child = document.firstChild
        while (child != null) {
            RenderBlockNode(node = child)
            child = child.next
        }
    }
}

@Composable
private fun RenderBlockNode(node: Node) {
    when (node) {
        is Heading -> {
            val headingColor = when (node.level) {
                1 -> Color(0xFF8C7CD4) // H1: #8C7CD4
                2 -> Color(0xFF8F84BE) // H2: #8F84BE
                3 -> Color(0xFF9C99AE) // H3: #9C99AE
                4 -> Color(0xFFABADD3) // H4: #ABADD3
                else -> Color(0xFFABADD3)
            }
            val headingText = buildInlineAnnotatedString(node, overrideTextColor = headingColor)
            val style = when (node.level) {
                1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp)
                2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                3 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Text(
                    text = headingText,
                    style = style,
                    color = headingColor
                )
                if (node.level == 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5E2D9))
                    )
                }
            }
        }
        is Paragraph -> {
            val annotatedText = buildInlineAnnotatedString(node)
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        is BlockQuote -> {
            val borderColor = Color(0xFF1A73E8)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1A73E8).copy(alpha = 0.05f),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var child = node.firstChild
                while (child != null) {
                    RenderBlockNode(node = child)
                    child = child.next
                }
            }
        }
        is FencedCodeBlock -> {
            CodeBlockView(literal = node.literal.trimEnd(), info = node.info)
        }
        is IndentedCodeBlock -> {
            CodeBlockView(literal = node.literal.trimEnd(), info = null)
        }
        is TableBlock -> {
            RenderTable(node)
        }
        is BulletList -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                var item = node.firstChild
                while (item != null) {
                    if (item is ListItem) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1A73E8),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                var child = item.firstChild
                                while (child != null) {
                                    RenderBlockNode(node = child)
                                    child = child.next
                                }
                            }
                        }
                    }
                    item = item.next
                }
            }
        }
        is OrderedList -> {
            var counter = node.startNumber
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                var item = node.firstChild
                while (item != null) {
                    if (item is ListItem) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$counter. ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1A73E8),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                var child = item.firstChild
                                while (child != null) {
                                    RenderBlockNode(node = child)
                                    child = child.next
                                }
                            }
                        }
                        counter++
                    }
                    item = item.next
                }
            }
        }
        is ThematicBreak -> {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private enum class MatchType {
    HIGHLIGHT, TAG
}

private data class MatchToken(
    val start: Int,
    val end: Int,
    val type: MatchType,
    val text: String
)

private fun buildTextWithHighlight(literal: String, spanStyle: SpanStyle? = null): AnnotatedString {
    return buildAnnotatedString {
        val highlightRegex = Regex("==([^=]+)==")
        val matches = mutableListOf<MatchToken>()
        
        highlightRegex.findAll(literal).forEach { match ->
            matches.add(MatchToken(match.range.first, match.range.last + 1, MatchType.HIGHLIGHT, match.groupValues[1]))
        }
        
        val sortedMatches = matches.sortedBy { it.start }
        var lastIdx = 0
        
        for (match in sortedMatches) {
            if (match.start < lastIdx) continue
            
            if (match.start > lastIdx) {
                val normalText = literal.substring(lastIdx, match.start)
                if (spanStyle != null) {
                    pushStyle(spanStyle)
                    append(normalText)
                    pop()
                } else {
                    append(normalText)
                }
            }
            
            when (match.type) {
                MatchType.HIGHLIGHT -> {
                    pushStyle(SpanStyle(
                        background = Color(0xFFFFF9C4),
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium
                    ))
                    append(match.text)
                    pop()
                }
                else -> {}
            }
            lastIdx = match.end
        }
        
        if (lastIdx < literal.length) {
            val normalText = literal.substring(lastIdx)
            if (spanStyle != null) {
                pushStyle(spanStyle)
                append(normalText)
                pop()
            } else {
                append(normalText)
            }
        }
    }
}

@Composable
private fun buildInlineAnnotatedString(
    blockNode: Node,
    overrideTextColor: Color? = null
): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        var node = blockNode.firstChild
        while (node != null) {
            when (node) {
                is Text -> {
                    if (overrideTextColor != null) {
                        pushStyle(SpanStyle(color = overrideTextColor))
                        append(buildTextWithHighlight(node.literal))
                        pop()
                    } else {
                        append(buildTextWithHighlight(node.literal))
                    }
                }
                is StrongEmphasis -> {
                    val childText = getInlineTextContent(node)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = overrideTextColor ?: Color.Unspecified))
                    append(buildTextWithHighlight(childText))
                    pop()
                }
                is Emphasis -> {
                    val childText = getInlineTextContent(node)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = overrideTextColor ?: Color.Unspecified))
                    append(buildTextWithHighlight(childText))
                    pop()
                }
                is Code -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFFFFF0F2),
                            color = Color(0xFFC7254E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                    append(" ${node.literal} ")
                    pop()
                }
                is Link -> {
                    val childText = getInlineTextContent(node)
                    pushStyle(
                        SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(buildTextWithHighlight(childText))
                    pop()
                }
                is SoftLineBreak -> {
                    append("\n")
                }
                is HardLineBreak -> {
                    append("\n")
                }
            }
            node = node.next
        }
    }
}

private fun getInlineTextContent(node: Node): String {
    val builder = StringBuilder()
    var child = node.firstChild
    while (child != null) {
        if (child is Text) {
            builder.append(child.literal)
        } else {
            builder.append(getInlineTextContent(child))
        }
        child = child.next
    }
    return builder.toString()
}

@Composable
private fun CodeBlockView(literal: String, info: String?) {
    val languageName = info?.trim()?.uppercase() ?: "CODE"
    val highlightedText = remember(literal, info) { highlightCode(literal, info) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
            .padding(bottom = 8.dp)
    ) {
        // Top Header mimic
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252526), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), shape = RoundedCornerShape(50)))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), shape = RoundedCornerShape(50)))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), shape = RoundedCornerShape(50)))
            }
            Text(
                text = languageName,
                color = Color(0xFF858585),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal scrolling code
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp, start = 8.dp, end = 12.dp)
        ) {
            val lines = literal.split("\n")
            // Line numbers Column
            Column(
                modifier = Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (idx in 1..lines.size) {
                    Text(
                        text = idx.toString(),
                        color = Color(0xFF5A5A5A),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Highlighted Code
            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun highlightCode(literal: String, language: String?): AnnotatedString {
    return buildAnnotatedString {
        val keywords = setOf(
            "fun", "class", "interface", "val", "var", "import", "package", "return", "if", "else", 
            "for", "while", "true", "false", "null", "this", "super", "private", "public", "protected",
            "internal", "override", "const", "object", "typealias", "try", "catch", "finally", "throw",
            "when", "is", "as", "in", "by", "get", "set", "out", "init", "constructor", "companion"
        )
        val types = setOf(
            "String", "Int", "Boolean", "Float", "Double", "Long", "Char", "Byte", "Short", "Any", "Unit",
            "List", "Map", "Set", "ArrayList", "HashMap", "HashSet", "Color", "Modifier", "Composable"
        )

        val lines = literal.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            var i = 0
            while (i < line.length) {
                val char = line[i]

                // Line comment
                if (char == '/' && i + 1 < line.length && line[i + 1] == '/') {
                    pushStyle(SpanStyle(color = Color(0xFF6A9955), fontStyle = FontStyle.Italic)) // Comment Green
                    append(line.substring(i))
                    pop()
                    break
                }

                // String literal
                if (char == '"') {
                    val start = i
                    i++
                    var escaped = false
                    while (i < line.length) {
                        if (line[i] == '"' && !escaped) {
                            break
                        }
                        escaped = line[i] == '\\' && !escaped
                        i++
                    }
                    val end = if (i < line.length) i + 1 else line.length
                    pushStyle(SpanStyle(color = Color(0xFFCE9178))) // String Orange
                    append(line.substring(start, end))
                    pop()
                    i = end
                    continue
                }

                // Number literal
                if (char.isDigit()) {
                    val start = i
                    while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L')) {
                        i++
                    }
                    pushStyle(SpanStyle(color = Color(0xFFB5CEA8))) // Number Light Green
                    append(line.substring(start, i))
                    pop()
                    continue
                }

                // Identifiers or punctuation
                if (char.isLetter() || char == '_') {
                    val start = i
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                        i++
                    }
                    val word = line.substring(start, i)
                    when {
                        keywords.contains(word) -> {
                            pushStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold)) // Keyword Blue
                            append(word)
                            pop()
                        }
                        types.contains(word) || (word.first().isUpperCase() && word.any { it.isLowerCase() }) -> {
                            pushStyle(SpanStyle(color = Color(0xFF4EC9B0))) // Type Cyan
                            append(word)
                            pop()
                        }
                        else -> {
                            append(word)
                        }
                    }
                } else {
                    val pColor = when (char) {
                        '{', '}', '(', ')', '[', ']' -> Color(0xFFFFD700) // Brackets Gold
                        '=', '+', '-', '*', '/', '%', '<', '>', '!', '&', '|', '?', ':' -> Color(0xFFD4D4D4) // Operators Light Grey
                        else -> Color(0xFF9CDCFE) // General symbols Light Blue
                    }
                    pushStyle(SpanStyle(color = pColor))
                    append(char.toString())
                    pop()
                    i++
                }
            }
            if (lineIdx < lines.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
private fun RenderTable(tableBlock: TableBlock) {
    val rows = remember(tableBlock) {
        val list = mutableListOf<List<TableCell>>()
        var child = tableBlock.firstChild
        while (child != null) {
            if (child is TableHead || child is TableBody) {
                var rowNode = child.firstChild
                while (rowNode != null) {
                    if (rowNode is TableRow) {
                        val cells = mutableListOf<TableCell>()
                        var cellNode = rowNode.firstChild
                        while (cellNode != null) {
                            if (cellNode is TableCell) {
                                cells.add(cellNode)
                            }
                            cellNode = cellNode.next
                        }
                        list.add(cells)
                    }
                    rowNode = rowNode.next
                }
            }
            child = child.next
        }
        list
    }

    if (rows.isEmpty()) return

    val maxCols = remember(rows) { rows.maxOfOrNull { it.size } ?: 0 }
    if (maxCols == 0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFFFBFBF9), shape = RoundedCornerShape(4.dp))
            .horizontalScroll(rememberScrollState())
            .drawBehind {
                // Draw thin outer boundary
                drawRect(
                    color = Color(0xFFE5E2D9),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        rows.forEachIndexed { rowIndex, cells ->
            val isHeader = cells.firstOrNull()?.isHeader ?: false
            val rowBg = when {
                isHeader -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                rowIndex % 2 == 1 -> Color(0xFFF7F7F5) // Zebra alternating color
                else -> Color.White
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .drawBehind {
                        // Horizontal divider
                        drawLine(
                            color = Color(0xFFE5E2D9),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIndex in 0 until maxCols) {
                    val cell = cells.getOrNull(colIndex)
                    val cellAlignment = cell?.alignment
                    val boxAlignment = when (cellAlignment) {
                        TableCell.Alignment.LEFT -> Alignment.CenterStart
                        TableCell.Alignment.CENTER -> Alignment.Center
                        TableCell.Alignment.RIGHT -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    }
                    val textAlign = when (cellAlignment) {
                        TableCell.Alignment.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
                        TableCell.Alignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                        TableCell.Alignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                        else -> androidx.compose.ui.text.style.TextAlign.Left
                    }

                    Box(
                        contentAlignment = boxAlignment,
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 250.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .drawBehind {
                                // Vertical divider between columns
                                if (colIndex < maxCols - 1) {
                                    drawLine(
                                        color = Color(0xFFE5E2D9),
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                    ) {
                        if (cell != null) {
                            val annotatedText = buildInlineAnnotatedString(cell)
                            Text(
                                text = annotatedText,
                                textAlign = textAlign,
                                style = if (isHeader) {
                                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
