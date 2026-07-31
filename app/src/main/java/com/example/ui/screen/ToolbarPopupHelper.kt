package com.example.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object ToolbarPopupSessionState {
    var preferredPosition by mutableStateOf<IntOffset?>(null)
}

class BubbleShape(
    private val isBelow: Boolean,
    private val arrowX: Float? = null,
    private val arrowWidthDp: Dp = 12.dp,
    private val arrowHeightDp: Dp = 7.dp,
    private val cornerRadiusDp: Dp = 10.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val arrowWidth = with(density) { arrowWidthDp.toPx() }
        val arrowHeight = with(density) { arrowHeightDp.toPx() }
        val cornerRadius = with(density) { cornerRadiusDp.toPx() }

        val targetX = arrowX ?: (size.width / 2f)
        val clampedArrowX = targetX.coerceIn(
            cornerRadius + arrowWidth / 2f,
            size.width - cornerRadius - arrowWidth / 2f
        )

        val path = Path()

        if (!isBelow) {
            val bodyBottom = size.height - arrowHeight
            path.moveTo(cornerRadius, 0f)
            path.lineTo(size.width - cornerRadius, 0f)
            path.quadraticTo(size.width, 0f, size.width, cornerRadius)
            path.lineTo(size.width, bodyBottom - cornerRadius)
            path.quadraticTo(size.width, bodyBottom, size.width - cornerRadius, bodyBottom)
            
            path.lineTo(clampedArrowX + arrowWidth / 2f, bodyBottom)
            path.lineTo(clampedArrowX, size.height)
            path.lineTo(clampedArrowX - arrowWidth / 2f, bodyBottom)
            
            path.lineTo(cornerRadius, bodyBottom)
            path.quadraticTo(0f, bodyBottom, 0f, bodyBottom - cornerRadius)
            path.lineTo(0f, cornerRadius)
            path.quadraticTo(0f, 0f, cornerRadius, 0f)
            path.close()
        } else {
            val bodyTop = arrowHeight
            path.moveTo(cornerRadius, bodyTop)
            
            path.lineTo(clampedArrowX - arrowWidth / 2f, bodyTop)
            path.lineTo(clampedArrowX, 0f)
            path.lineTo(clampedArrowX + arrowWidth / 2f, bodyTop)
            
            path.lineTo(size.width - cornerRadius, bodyTop)
            path.quadraticTo(size.width, bodyTop, size.width, bodyTop + cornerRadius)
            path.lineTo(size.width, size.height - cornerRadius)
            path.quadraticTo(size.width, size.height, size.width - cornerRadius, size.height)
            path.lineTo(cornerRadius, size.height)
            path.quadraticTo(0f, size.height, 0f, size.height - cornerRadius)
            path.lineTo(0f, bodyTop + cornerRadius)
            path.quadraticTo(0f, bodyTop, cornerRadius, bodyTop)
            path.close()
        }

        return Outline.Generic(path)
    }
}

fun Modifier.simpleVerticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 2.dp,
    color: Color = Color.Gray.copy(alpha = 0.4f)
): Modifier = this.drawWithContent {
    drawContent()
    val totalHeight = scrollState.maxValue + this.size.height
    if (totalHeight > this.size.height) {
        val scrollPercentage = scrollState.value.toFloat() / totalHeight
        val scrollbarHeight = (this.size.height / totalHeight) * this.size.height
        val scrollbarOffset = scrollPercentage * this.size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(this.size.width - width.toPx(), scrollbarOffset),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

@Composable
fun CustomToolbarPopup(
    rect: Rect,
    onCopy: (() -> Unit)?,
    onPaste: (() -> Unit)?,
    onCut: (() -> Unit)?,
    onSelectAll: (() -> Unit)?,
    onDismiss: () -> Unit,
    contentValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showSubMenu by remember { mutableStateOf(false) }
    var isBelow by remember { mutableStateOf(false) }
    var currentPopupPosition by remember { mutableStateOf(IntOffset.Zero) }
    var arrowXOffset by remember { mutableStateOf<Float?>(null) }
    
    val entranceAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val entranceScale = remember { androidx.compose.animation.core.Animatable(0.9f) }

    LaunchedEffect(Unit) {
        launch {
            entranceAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
        launch {
            entranceScale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    Popup(
        popupPositionProvider = ToolbarPopupPositionProvider(rect, showSubMenu, ToolbarPopupSessionState.preferredPosition) { below, offset, arrowX ->
            if (isBelow != below) {
                isBelow = below
            }
            if (currentPopupPosition != offset) {
                currentPopupPosition = offset
            }
            if (arrowXOffset != arrowX) {
                arrowXOffset = arrowX
            }
        },
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            shape = BubbleShape(isBelow = isBelow, arrowX = arrowXOffset),
            shadowElevation = 6.dp,
            color = Color(0xE6FFFFFF), // More opaque white for frosted glass feel
            border = BorderStroke(0.5.dp, Color(0x33000000)),
            modifier = Modifier
                .alpha(entranceAlpha.value)
                .scale(entranceScale.value)
                .pointerInput(showSubMenu) {
                    if (showSubMenu) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val prev = ToolbarPopupSessionState.preferredPosition ?: currentPopupPosition
                            ToolbarPopupSessionState.preferredPosition = IntOffset(
                                prev.x + dragAmount.x.toInt(),
                                prev.y + dragAmount.y.toInt()
                            )
                        }
                    }
                }
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Box(
                modifier = Modifier.padding(
                    top = if (isBelow) 7.dp else 0.dp,
                    bottom = if (!isBelow) 7.dp else 0.dp
                )
            ) {
            if (!showSubMenu) {
                Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 1. Cut (剪切)
                    val hasSelection = contentValue.selection.length > 0
                    if (hasSelection) {
                        TextButton(onClick = {
                            if (onCut != null) {
                                onCut()
                            } else {
                                val selectedText = contentValue.text.substring(contentValue.selection.min, contentValue.selection.max)
                                clipboardManager.setText(AnnotatedString(selectedText))
                                val newText = contentValue.text.removeRange(contentValue.selection.min, contentValue.selection.max)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min)))
                            }
                            onDismiss()
                        }) { Text("剪切") }
                    }
                    
                    // 2. Copy (复制)
                    if (hasSelection) {
                        TextButton(onClick = {
                            if (onCopy != null) {
                                onCopy()
                            } else {
                                val selectedText = contentValue.text.substring(contentValue.selection.min, contentValue.selection.max)
                                clipboardManager.setText(AnnotatedString(selectedText))
                            }
                            onDismiss()
                        }) { Text("复制") }
                    }
                    
                    // 3. Paste (粘贴)
                    TextButton(onClick = {
                        if (onPaste != null) {
                            onPaste()
                        } else {
                            val clipText = clipboardManager.getText()?.text ?: ""
                            val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, clipText)
                            onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min + clipText.length)))
                        }
                        onDismiss()
                    }) { Text("粘贴") }
                    
                    // 4. Select All (全选)
                    val isFullySelected = contentValue.selection.length == contentValue.text.length && contentValue.text.isNotEmpty()
                    if (contentValue.text.isNotEmpty() && !isFullySelected) {
                        TextButton(onClick = {
                            if (onSelectAll != null) {
                                onSelectAll()
                            } else {
                                onValueChange(contentValue.copy(selection = TextRange(0, contentValue.text.length)))
                            }
                            onDismiss()
                        }) { Text("全选") }
                    }
                    
                    // 5. Paragraph Only (仅段落)
                    if (!hasSelection) {
                        TextButton(onClick = {
                            val clipText = clipboardManager.getText()?.text ?: ""
                            val paragraphs = clipText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
                            val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, paragraphs)
                            onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min + paragraphs.length)))
                            onDismiss()
                        }) { Text("仅段落") }
                    }

                    // 6. Delete (删除)
                    if (hasSelection) {
                        TextButton(onClick = { 
                            val newText = contentValue.text.removeRange(contentValue.selection.min, contentValue.selection.max)
                            onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min)))
                            onDismiss()
                        }) { Text("删除") }
                    }

                    // 7. More Vert (三个点)
                    if (hasSelection) {
                        IconButton(
                            onClick = { showSubMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多功能",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState)
                        .simpleVerticalScrollbar(scrollState)
                        .padding(vertical = 4.dp)
                ) {
                    val hasSelection = contentValue.selection.length > 0
                    val selectedText = if (hasSelection) {
                        contentValue.text.substring(contentValue.selection.min, contentValue.selection.max)
                    } else {
                        ""
                    }
                    
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                    
                    if (isBelow) {
                        // Back Arrow Row at Top
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showSubMenu = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "返回",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        androidx.compose.material3.HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Option 1: 交换
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = smartSwap(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("智能交换", fontSize = 14.sp)
                        }
                    }
                    
                    // Option 2: 中英空格
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = addChineseEnglishSpaces(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("中英空格", fontSize = 14.sp)
                        }
                    }
                    
                    // Option 3: 大小写
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = toggleCase(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("大小写切换", fontSize = 14.sp)
                        }
                    }
                    
                    // Option 4: 首字母
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = capitalizeWords(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("首字母", fontSize = 14.sp)
                        }
                    }
                    
                    // Option 5: 双引号
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = wrapInDoubleQuotes(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("双引号", fontSize = 14.sp)
                        }
                    }
                    
                    // Option 6: 清除格式
                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                val transformed = clearMarkdownFormatting(selectedText)
                                val newText = contentValue.text.replaceRange(contentValue.selection.min, contentValue.selection.max, transformed)
                                onValueChange(TextFieldValue(newText, TextRange(contentValue.selection.min, contentValue.selection.min + transformed.length)))
                            }
                            onDismiss()
                        },
                        enabled = hasSelection,
                        modifier = itemModifier,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text("清除格式", fontSize = 14.sp)
                        }
                    }
                    
                    if (!isBelow) {
                        androidx.compose.material3.HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        // Back Arrow Row at Bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showSubMenu = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "返回",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private fun smartSwap(text: String): String {
    val separators = listOf(",", "，", " ", "/", "、", "和", "与", "及")
    for (sep in separators) {
        if (text.contains(sep)) {
            val parts = text.split(sep, limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                return parts[1] + sep + parts[0]
            }
        }
    }
    if (text.length >= 2) {
        val mid = text.length / 2
        return text.substring(mid) + text.substring(0, mid)
    }
    return text
}

private fun addChineseEnglishSpaces(text: String): String {
    var result = text
    result = result.replace(Regex("([\\u4e00-\\u9fa5])([a-zA-Z0-9])"), "$1 $2")
    result = result.replace(Regex("([a-zA-Z0-9])([\\u4e00-\\u9fa5])"), "$1 $2")
    return result
}

private fun toggleCase(text: String): String {
    val hasLower = text.any { it.isLowerCase() }
    return if (hasLower) text.uppercase() else text.lowercase()
}

private fun capitalizeWords(text: String): String {
    if (text.isEmpty()) return text
    return text.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

private fun wrapInDoubleQuotes(text: String): String {
    return "“$text”"
}

private fun clearMarkdownFormatting(text: String): String {
    var result = text
    
    // 1. Strip headers: e.g. # Header -> Header
    result = result.replace(Regex("^(?m)^[#]{1,6}\\s+"), "")
    
    // 2. Strip blockquotes: e.g. > Quote -> Quote
    result = result.replace(Regex("^(?m)^>\\s+"), "")
    
    // 3. Strip bold & italic: e.g. ***bold-italic***, **bold**, *italic*
    result = result.replace(Regex("\\*\\*\\*(.*?)\\*\\*\\*"), "$1")
    result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
    result = result.replace(Regex("\\*(.*?)\\*"), "$1")
    result = result.replace(Regex("___(.*?)___"), "$1")
    result = result.replace(Regex("__(.*?)__"), "$1")
    result = result.replace(Regex("_(.*?)_"), "$1")
    
    // 4. Strip strikethrough: e.g. ~~strikethrough~~
    result = result.replace(Regex("~~(.*?)~~"), "$1")
    
    // 5. Strip inline code: e.g. `code`
    result = result.replace(Regex("`(.*?)`"), "$1")
    
    // 6. Strip links: e.g. [title](url) -> title
    result = result.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
    
    // 7. Strip lists symbols if any at start of line
    result = result.replace(Regex("^(?m)^[\\*\\-\\+]\\s+"), "")
    result = result.replace(Regex("^(?m)^\\d+\\.\\s+"), "")
    
    return result
}
