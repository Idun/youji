package com.example.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.component.ExportBottomSheet
import com.example.ui.component.QuickPhraseCapsuleWidget
import com.example.domain.model.QuickPhrase
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.focus.onFocusChanged

import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.Inspiration
import com.example.ui.markdown.MarkdownRenderer
import com.example.ui.viewmodel.InspirationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousInspirationCard(
    inspiration: Inspiration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "已创建笔记",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            val titleText = if (inspiration.title.isNotBlank()) inspiration.title else "无标题"
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (inspiration.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = inspiration.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationEditScreen(
    viewModel: InspirationViewModel,
    inspirationId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val context = LocalContext.current
    val themeColorLong by viewModel.themeColor.collectAsState()
    val brandColor = Color(themeColorLong)
    var titleValue by remember { mutableStateOf(TextFieldValue("")) }
    var focusedField by remember { mutableStateOf("content") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var tag by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var createdTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var sortOrder by remember { mutableStateOf(0L) }

    var currentId by remember(inspirationId) { mutableStateOf(inspirationId) }
    var autoSaveStatus by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var previousInspiration by remember { mutableStateOf<Inspiration?>(null) }
    val allInspirations by viewModel.inspirations.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val quickPhrases by viewModel.quickPhrases.collectAsState()

    LaunchedEffect(inspirationId) {
        if (inspirationId != 0) {
            val inspiration = viewModel.getInspirationById(inspirationId).firstOrNull()
            if (inspiration != null) {
                titleValue = TextFieldValue(inspiration.title)
                contentValue = TextFieldValue(inspiration.content)
                tag = inspiration.tag
                category = inspiration.category
                isPinned = inspiration.isPinned
                isArchived = inspiration.isArchived
                createdTimestamp = inspiration.timestamp
                sortOrder = inspiration.sortOrder
            }
        } else {
            val currentGroup = viewModel.selectedGroup.value
            if (currentGroup != "全部笔记" && currentGroup != "未分类") {
                category = currentGroup
            }
        }
        isLoaded = true
    }

    // Auto save with debounce (1 second delay)
    LaunchedEffect(titleValue.text, contentValue.text, tag, category, isPinned, isArchived, isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        if (titleValue.text.isNotBlank() || contentValue.text.isNotBlank() || currentId > 0) {
            delay(1000L)
            autoSaveStatus = "保存中..."
            val savedId = viewModel.saveInspiration(
                title = titleValue.text,
                content = contentValue.text,
                tag = tag,
                category = category,
                isPinned = isPinned,
                isArchived = isArchived,
                existingId = currentId,
                sortOrder = sortOrder,
                createdTimestamp = createdTimestamp
            )
            if (savedId > 0) {
                currentId = savedId
            }
            autoSaveStatus = "已保存"
            delay(2000L)
            if (autoSaveStatus == "已保存") {
                autoSaveStatus = ""
            }
        }
    }

    // Save automatically when entering background or leaving screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (isLoaded && (titleValue.text.isNotBlank() || contentValue.text.isNotBlank() || currentId > 0)) {
                    scope.launch {
                        val savedId = viewModel.saveInspiration(
                            title = titleValue.text,
                            content = contentValue.text,
                            tag = tag,
                            category = category,
                            isPinned = isPinned,
                            isArchived = isArchived,
                            existingId = currentId,
                            sortOrder = sortOrder,
                            createdTimestamp = createdTimestamp
                        )
                        if (savedId > 0) {
                            currentId = savedId
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isLoaded && (titleValue.text.isNotBlank() || contentValue.text.isNotBlank() || currentId > 0)) {
                scope.launch {
                    viewModel.saveInspiration(
                        title = titleValue.text,
                        content = contentValue.text,
                        tag = tag,
                        category = category,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        existingId = currentId,
                        sortOrder = sortOrder,
                        createdTimestamp = createdTimestamp
                    )
                }
            }
        }
    }

    LaunchedEffect(allInspirations, inspirationId) {
        val sortedList = allInspirations.sortedByDescending { it.timestamp }
        previousInspiration = if (inspirationId == 0 && sortedList.isNotEmpty()) {
            sortedList.first()
        } else if (inspirationId != 0) {
            val currentIndex = sortedList.indexOfFirst { it.id == inspirationId }
            if (currentIndex > 0) sortedList[currentIndex - 1] else null
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        if (inspirationId == 0) {
            focusRequester.requestFocus()
        }
    }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    var lastValue by remember { mutableStateOf(contentValue) }
    var isUndoRedoAction by remember { mutableStateOf(false) }

    LaunchedEffect(contentValue) {
        if (isUndoRedoAction) {
            isUndoRedoAction = false
            lastValue = contentValue
            return@LaunchedEffect
        }
        if (contentValue.text != lastValue.text) {
            if (undoStack.isEmpty() || undoStack.last().text != lastValue.text) {
                undoStack.add(lastValue)
                if (undoStack.size > 20) {
                    undoStack.removeAt(0)
                }
            }
            lastValue = contentValue
            redoStack.clear()
        }
    }

    val onSave = {
        scope.launch {
            if (titleValue.text.isNotBlank() || contentValue.text.isNotBlank() || currentId > 0) {
                val savedId = viewModel.saveInspiration(
                    title = titleValue.text,
                    content = contentValue.text,
                    tag = tag,
                    category = category,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    existingId = currentId,
                    sortOrder = sortOrder,
                    createdTimestamp = createdTimestamp
                )
                if (savedId > 0) {
                    currentId = savedId
                }
            }
        }
    }

    BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        onSave()
        onNavigateBack()
    }

    var showTagManagerDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    var textToolbarRect by remember { mutableStateOf<Rect?>(null) }
    var onCopyMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onPasteMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onCutMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onSelectAllMenu: (() -> Unit)? by remember { mutableStateOf(null) }

    val customTextToolbar = remember {
        CustomTextToolbar(
            onShowMenu = { rect, onCopy, onPaste, onCut, onSelectAll ->
                textToolbarRect = rect
                onCopyMenu = onCopy
                onPasteMenu = onPaste
                onCutMenu = onCut
                onSelectAllMenu = onSelectAll
            },
            onHideMenu = {
                textToolbarRect = null
            }
        )
    }

    LaunchedEffect(contentValue.selection, titleValue.selection) {
        if (contentValue.selection.length == 0 && titleValue.selection.length == 0) {
            textToolbarRect = null
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalTextToolbar provides customTextToolbar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentId == 0) "新灵感" else "编辑灵感",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (autoSaveStatus.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = autoSaveStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoSaveStatus == "保存中...") brandColor else Color.Gray
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSave()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        Text(
                            text = "${contentValue.text.length}字",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B685C),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = 8.dp)
                        )
                        IconButton(onClick = {
                            isPinned = !isPinned
                        }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = "置顶",
                                tint = if (isPinned) brandColor else Color(0xFF6B685C)
                            )
                        }
                        IconButton(onClick = {
                            if (contentValue.text.isBlank()) {
                                Toast.makeText(context, "正文内容为空，无法导出", Toast.LENGTH_SHORT).show()
                            } else {
                                showExportSheet = true
                            }
                        }) {
                            Icon(Icons.Default.SaveAlt, contentDescription = "导出文档", tint = Color(0xFF6B685C))
                        }
                        IconButton(onClick = {
                            isPreviewMode = !isPreviewMode
                        }) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = if (isPreviewMode) "编辑" else "预览 Markdown",
                                tint = if (isPreviewMode) brandColor else Color(0xFF6B685C)
                            )
                        }
                        IconButton(onClick = {
                            onSave()
                            onNavigateToList()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发布为新卡片", tint = brandColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color(0xFFFBFBFD)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                if (!isPreviewMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    ) {
                    TextField(
                        value = titleValue,
                        onValueChange = { titleValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .onFocusChanged { if (it.isFocused) focusedField = "title" },
                        placeholder = { Text("标题（可选）", color = Color(0xFF8E8E93), letterSpacing = (-0.3).sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.4).sp
                        ),
                        singleLine = true
                    )

                    TextField(
                        value = contentValue,
                        onValueChange = { newValue ->
                            var processedValue = newValue
                            val oldText = contentValue.text
                            val newText = newValue.text
                            val oldCursor = contentValue.selection.start
                            val newCursor = newValue.selection.start
                            
                            if (newText.length == oldText.length + 1 && newCursor == oldCursor + 1) {
                                val insertedChar = newText[oldCursor]
                                
                                val closingChars = setOf('\'', '"', ')', ']', '}', '>', '》', '」', '』', '】', '）', '’', '”', '〉')
                                if (oldCursor < oldText.length && oldText[oldCursor] == insertedChar && closingChars.contains(insertedChar)) {
                                    processedValue = TextFieldValue(
                                        text = oldText,
                                        selection = TextRange(newCursor)
                                    )
                                } else {
                                    val closingChar = when (insertedChar) {
                                        '\'' -> '\''
                                        '"' -> '"'
                                        '(' -> ')'
                                        '[' -> ']'
                                        '{' -> '}'
                                        '<' -> '>'
                                        '《' -> '》'
                                        '「' -> '」'
                                        '『' -> '』'
                                        '【' -> '】'
                                        '（' -> '）'
                                        '‘' -> '’'
                                        '“' -> '”'
                                        '〈' -> '〉'
                                        else -> null
                                    }
                                    
                                    if (closingChar != null) {
                                        val updatedText = newText.substring(0, newCursor) + closingChar + newText.substring(newCursor)
                                        processedValue = TextFieldValue(
                                            text = updatedText,
                                            selection = TextRange(newCursor)
                                        )
                                    }
                                }
                            } else if (newText.length == oldText.length - 1 && newCursor == oldCursor - 1) {
                                val deletedChar = oldText[newCursor]
                                val openingCharsMap = mapOf(
                                    '\'' to '\'', '"' to '"', '(' to ')', '[' to ']', '{' to '}', '<' to '>',
                                    '《' to '》', '「' to '」', '『' to '』', '【' to '】', '（' to '）', '‘' to '’',
                                    '“' to '”', '〈' to '〉'
                                )
                                if (openingCharsMap.containsKey(deletedChar) && newCursor < newText.length) {
                                    val expectedClosingChar = openingCharsMap[deletedChar]
                                    if (newText[newCursor] == expectedClosingChar) {
                                        val updatedText = newText.substring(0, newCursor) + newText.substring(newCursor + 1)
                                        processedValue = TextFieldValue(
                                            text = updatedText,
                                            selection = TextRange(newCursor)
                                        )
                                    }
                                }
                            }
                            
                            contentValue = processedValue
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) focusedField = "content" },
                        placeholder = { Text("写下你的想法...", color = Color(0xFF8E8E93)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF1D1D1F),
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        visualTransformation = MarkdownAndTagVisualTransformation()
                    )

                    // Markdown Toolbar Bottom
                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.95f))
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 撤销与重做移到最左侧
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    isUndoRedoAction = true
                                    val currentState = contentValue
                                    redoStack.add(currentState)
                                    val prevState = undoStack.removeAt(undoStack.lastIndex)
                                    lastValue = prevState
                                    contentValue = prevState
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "撤销",
                                tint = if (undoStack.isNotEmpty()) Color.Gray else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    isUndoRedoAction = true
                                    val currentState = contentValue
                                    undoStack.add(currentState)
                                    val nextState = redoStack.removeAt(redoStack.lastIndex)
                                    lastValue = nextState
                                    contentValue = nextState
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "重做",
                                tint = if (redoStack.isNotEmpty()) Color.Gray else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                        IconButton(onClick = {
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "**" + selected + "**" + after
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + 2 + selected.length + 2))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatBold, contentDescription = "加粗", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }
                        
                        IconButton(onClick = {
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "*" + selected + "*" + after
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + 1 + selected.length + 1))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "斜体", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "==" + selected + "==" + after
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + 2 + selected.length + 2))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.BorderColor, contentDescription = "高亮", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }
                        
                        IconButton(onClick = {
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            
                            val insertText = if (before.isEmpty() || before.last() == '\n') "- $selected" else "\n- $selected"
                            val newValue = before + insertText + after
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + insertText.length))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val insertText = if (before.isEmpty() || before.last() == '\n') "> $selected" else "\n> $selected"
                            val newValue = before + insertText + after
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + insertText.length))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "引用", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val currentText = contentValue.text
                            val min = contentValue.selection.min
                            val max = contentValue.selection.max
                            val insertTag = "#"
                            val newValue = currentText.substring(0, min) + insertTag + currentText.substring(max)
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + 1))
                        }, modifier = Modifier.size(36.dp)) {
                            Text("#", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }

                        IconButton(onClick = {
                            val currentText = contentValue.text
                            val min = contentValue.selection.min
                            val max = contentValue.selection.max
                            val selected = currentText.substring(min, max)
                            val newValue = currentText.substring(0, min) + "`" + selected + "`" + currentText.substring(max)
                            contentValue = TextFieldValue(newValue, selection = TextRange(min + 1 + selected.length + 1))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Code, contentDescription = "代码", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                        IconButton(onClick = { showTagManagerDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.LocalOffer, contentDescription = "管理标签", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }

                        if (showTagManagerDialog) {
                            var newTagInput by remember { mutableStateOf("") }
                            val currentTagsList = remember(tag) {
                                tag.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            }
                            
                            AlertDialog(
                                onDismissRequest = { showTagManagerDialog = false },
                                title = {
                                    Text(
                                        text = "管理笔记标签",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "当前标签（点击可移除）:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        if (currentTagsList.isEmpty()) {
                                            Text(
                                                text = "暂无标签",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                currentTagsList.forEach { t ->
                                                    SuggestionChip(
                                                        onClick = {
                                                            val updatedList = currentTagsList.toMutableList().apply { remove(t) }
                                                            tag = updatedList.joinToString(",")
                                                            
                                                            val tagPattern = "#$t"
                                                            var updatedText = contentValue.text
                                                            if (updatedText.contains(tagPattern)) {
                                                                updatedText = updatedText.replace(tagPattern, "").replace("  ", " ").trim()
                                                                contentValue = TextFieldValue(
                                                                    text = updatedText,
                                                                    selection = TextRange(updatedText.length)
                                                                )
                                                            }
                                                        },
                                                        label = { Text(t) },
                                                        icon = {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "删除标签",
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        },
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            containerColor = brandColor.copy(alpha = 0.12f),
                                                            labelColor = brandColor
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                        
                                        Text(
                                            text = "新增标签:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newTagInput,
                                                onValueChange = { newTagInput = it },
                                                placeholder = { Text("新标签名称", fontSize = 14.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = brandColor,
                                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                                )
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    val trimmed = newTagInput.trim()
                                                    if (trimmed.isNotEmpty() && !currentTagsList.contains(trimmed)) {
                                                        val updatedList = currentTagsList.toMutableList().apply { add(trimmed) }
                                                        tag = updatedList.joinToString(",")
                                                        
                                                        val currentText = contentValue.text
                                                        val tagString = "#$trimmed"
                                                        val appendix = if (currentText.isEmpty()) {
                                                            tagString
                                                        } else if (currentText.endsWith("\n") || currentText.endsWith(" ")) {
                                                            tagString
                                                        } else {
                                                            " $tagString"
                                                        }
                                                        contentValue = TextFieldValue(
                                                            text = currentText + appendix,
                                                            selection = TextRange(currentText.length + appendix.length)
                                                        )
                                                        newTagInput = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("添加", color = Color.White)
                                            }
                                        }
                                        
                                        val otherTags = allTags.filter { !currentTagsList.contains(it) }
                                        if (otherTags.isNotEmpty()) {
                                            Text(
                                                text = "推荐已有标签（点击可快速添加）:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                otherTags.forEach { t ->
                                                    SuggestionChip(
                                                        onClick = {
                                                            val updatedList = currentTagsList.toMutableList().apply { add(t) }
                                                            tag = updatedList.joinToString(",")
                                                            
                                                            val currentText = contentValue.text
                                                            val tagString = "#$t"
                                                            val appendix = if (currentText.isEmpty()) {
                                                                tagString
                                                            } else if (currentText.endsWith("\n") || currentText.endsWith(" ")) {
                                                                tagString
                                                            } else {
                                                                " $tagString"
                                                            }
                                                            contentValue = TextFieldValue(
                                                                text = currentText + appendix,
                                                                selection = TextRange(currentText.length + appendix.length)
                                                            )
                                                        },
                                                        label = { Text(t) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = { showTagManagerDialog = false },
                                        colors = ButtonDefaults.textButtonColors(contentColor = brandColor)
                                    ) {
                                        Text("确定", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (titleValue.text.isNotEmpty()) {
                        Text(
                            text = titleValue.text,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (tag.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (contentValue.text.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "无正文预览",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        MarkdownRenderer(markdown = contentValue.text)
                    }

                    if (previousInspiration != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PreviousInspirationCard(
                            inspiration = previousInspiration!!,
                            onClick = { onNavigateToEdit(previousInspiration!!.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (!isPreviewMode) {
                QuickPhraseCapsuleWidget(
                    phrases = quickPhrases,
                    brandColor = brandColor,
                    onPhraseClick = { phrase ->
                        if (phrase.id == "empty") {
                            Toast.makeText(context, "暂无快捷词组，可在【我的-快捷词组设置】中新增", Toast.LENGTH_SHORT).show()
                        } else {
                            val phraseText = phrase.content
                            val text = contentValue.text
                            val selection = contentValue.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val newValue = before + phraseText + after
                            val newSelection = TextRange(min + phraseText.length)
                            contentValue = TextFieldValue(newValue, selection = newSelection)
                            viewModel.incrementQuickPhraseUsage(phrase.id)
                        }
                    },
                    onPhraseUpdate = { id, label, content ->
                        viewModel.updateQuickPhrase(id, label, content)
                    },
                    onPhraseDelete = { id ->
                        viewModel.deleteQuickPhrase(id)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(top = 60.dp)
                )
            }
        }
        if (textToolbarRect != null) {
            val currentFieldValue = if (focusedField == "content") contentValue else titleValue
            CustomToolbarPopup(
                rect = textToolbarRect!!,
                onCopy = onCopyMenu,
                onPaste = onPasteMenu,
                onCut = onCutMenu,
                onSelectAll = onSelectAllMenu,
                onDismiss = { textToolbarRect = null },
                contentValue = currentFieldValue,
                onValueChange = { if (focusedField == "content") contentValue = it else titleValue = it }
            )
        }

        if (showExportSheet) {
            ExportBottomSheet(
                onDismissRequest = { showExportSheet = false },
                context = context,
                title = titleValue.text,
                content = contentValue.text,
                onResult = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
}

class MarkdownAndTagVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val builder = AnnotatedString.Builder(originalText)
        val syntaxColor = Color(0xFFE79B59) // 规定 Markdown 语法符号统一使用 #E79B59 染色

        // 1. 标题语法 (# 前缀)
        val headingRegex = Regex("(?m)^(#{1,6})(\\s+)(.*)$")
        headingRegex.findAll(originalText).forEach { match ->
            val hashesGroup = match.groups[1]
            val titleTextGroup = match.groups[3]
            if (hashesGroup != null) {
                // # 语法字符渲染为 syntaxColor (#E79B59)
                builder.addStyle(
                    style = SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold),
                    start = hashesGroup.range.first,
                    end = hashesGroup.range.last + 1
                )
                // 标题内容渲染为对应级别色值 (H1: #8C7CD4, H2: #8F84BE, H3: #9C99AE, H4: #ABADD3)
                val level = hashesGroup.value.length
                val titleColor = when (level) {
                    1 -> Color(0xFF8C7CD4)
                    2 -> Color(0xFF8F84BE)
                    3 -> Color(0xFF9C99AE)
                    4 -> Color(0xFFABADD3)
                    else -> Color(0xFFABADD3)
                }
                if (titleTextGroup != null && titleTextGroup.value.isNotEmpty()) {
                    builder.addStyle(
                        style = SpanStyle(color = titleColor, fontWeight = FontWeight.Bold),
                        start = titleTextGroup.range.first,
                        end = titleTextGroup.range.last + 1
                    )
                }
            }
        }

        // 2. 加粗: **bold** 或 __bold__
        val boldRegex = Regex("(\\*\\*)([^*\\n]+)(\\*\\*)|(__)([^_\\n]+)(__)")
        boldRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
            // 语法字符 ** 或 __ 染为 syntaxColor
            val g1 = match.groups[1]
            val g3 = match.groups[3]
            val g4 = match.groups[4]
            val g6 = match.groups[6]
            if (g1 != null && g3 != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), g1.range.first, g1.range.last + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), g3.range.first, g3.range.last + 1)
            }
            if (g4 != null && g6 != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), g4.range.first, g4.range.last + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), g6.range.first, g6.range.last + 1)
            }
        }

        // 3. 斜体: *italic* 或 _italic_
        val italicRegex = Regex("(?<!\\*)(\\*|_)(([^*_\\n]+))(\\*|_)(?!\\*)")
        italicRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = match.range.first,
                end = match.range.last + 1
            )
            val startIdx = match.range.first
            val endIdx = match.range.last
            builder.addStyle(SpanStyle(color = syntaxColor), startIdx, startIdx + 1)
            builder.addStyle(SpanStyle(color = syntaxColor), endIdx, endIdx + 1)
        }

        // 4. 高亮: ==highlight==
        val highlightRegex = Regex("(==)([^=\\n]+)(==)")
        highlightRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    background = Color(0xFFFFF2A8),
                    color = Color(0xFF1D1D1F),
                    fontWeight = FontWeight.SemiBold
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            val g1 = match.groups[1]
            val g3 = match.groups[3]
            if (g1 != null && g3 != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), g1.range.first, g1.range.last + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), g3.range.first, g3.range.last + 1)
            }
        }

        // 5. 行内代码: `code`
        val codeRegex = Regex("(`)([^`\\n]+)(`)")
        codeRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0xFFF2F2F7),
                    color = Color(0xFFD73A49)
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            val g1 = match.groups[1]
            val g3 = match.groups[3]
            if (g1 != null && g3 != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), g1.range.first, g1.range.last + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), g3.range.first, g3.range.last + 1)
            }
        }

        // 6. 删除线: ~~strikethrough~~
        val strikeRegex = Regex("(~~)([^~\\n]+)(~~)")
        strikeRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                start = match.range.first,
                end = match.range.last + 1
            )
            val g1 = match.groups[1]
            val g3 = match.groups[3]
            if (g1 != null && g3 != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), g1.range.first, g1.range.last + 1)
                builder.addStyle(SpanStyle(color = syntaxColor), g3.range.first, g3.range.last + 1)
            }
        }

        // 7. 列表语法 (- / * / + / 1.)
        val listRegex = Regex("(?m)^(\\s*[-*+]|\\s*\\d+\\.)(\\s+)")
        listRegex.findAll(originalText).forEach { match ->
            val listGroup = match.groups[1]
            if (listGroup != null) {
                builder.addStyle(
                    style = SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold),
                    start = listGroup.range.first,
                    end = listGroup.range.last + 1
                )
            }
        }

        // 8. 引用语法 (> 前缀)
        val quoteRegex = Regex("(?m)^(>+)(\\s*)")
        quoteRegex.findAll(originalText).forEach { match ->
            val quoteGroup = match.groups[1]
            if (quoteGroup != null) {
                builder.addStyle(
                    style = SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold),
                    start = quoteGroup.range.first,
                    end = quoteGroup.range.last + 1
                )
            }
        }

        // 9. 话题标签: #tag
        val tagRegex = Regex("(?<!#)#[^#\\s]+")
        tagRegex.findAll(originalText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    background = Color(0xFFE8F0FE),
                    color = Color(0xFF1967D2),
                    fontWeight = FontWeight.Bold
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            // # 符号染为 syntaxColor
            builder.addStyle(
                style = SpanStyle(color = syntaxColor),
                start = match.range.first,
                end = match.range.first + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
