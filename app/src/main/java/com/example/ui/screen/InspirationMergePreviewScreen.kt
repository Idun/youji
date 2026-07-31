package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Inspiration
import com.example.ui.markdown.MarkdownRenderer
import com.example.ui.viewmodel.InspirationViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.ui.component.ExportBottomSheet
import com.example.ui.component.QuickPhraseCapsuleWidget
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState

data class MergeState(
    val title: String,
    val category: String,
    val content: String,
    val tag: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InspirationMergePreviewScreen(
    ids: String,
    viewModel: InspirationViewModel,
    onNavigateBack: () -> Unit
) {
    val inspirations by viewModel.allInspirations.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val quickPhrases by viewModel.quickPhrases.collectAsState()
    val themeColorLong by viewModel.themeColor.collectAsState()
    val brandColor = Color(themeColorLong)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve pre-selected notes from the incoming 'ids'
    val selectedInspirations = remember(ids, inspirations) {
        val idList = ids.split(",").mapNotNull { it.trim().toIntOrNull() }
        idList.mapNotNull { id -> inspirations.find { it.id == id } }
    }
    val isPureMergePreview = remember(ids) {
        ids.split(",").mapNotNull { it.trim().toIntOrNull() }.any { it > 0 }
    }

    // SharedPrefs for auto-save
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    androidx.activity.compose.BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        onNavigateBack()
    }
    val sharedPrefs = remember { context.getSharedPreferences("merge_draft_prefs", Context.MODE_PRIVATE) }

    // Editing states
    var jointContent by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var jointTitle by remember { mutableStateOf("") }
    var jointCategory by remember { mutableStateOf("") }
    var jointTag by remember { mutableStateOf("") }

    // Undo / Redo history stack
    val historyStack = remember { mutableStateListOf<MergeState>() }
    var historyIndex by remember { mutableStateOf(-1) }

    fun pushHistory(newState: MergeState) {
        if (historyIndex >= 0 && historyStack[historyIndex] == newState) return
        while (historyStack.size > historyIndex + 1) {
            historyStack.removeAt(historyStack.lastIndex)
        }
        historyStack.add(newState)
        if (historyStack.size > 50) {
            historyStack.removeAt(0)
        }
        historyIndex = historyStack.lastIndex
        
        // Auto-save to localStorage (SharedPreferences) ONLY when in free collage draft mode, NOT in pure merge preview mode!
        if (!isPureMergePreview && selectedInspirations.isEmpty()) {
            sharedPrefs.edit()
                .putString("draft_title", newState.title)
                .putString("draft_category", newState.category)
                .putString("draft_tag", newState.tag)
                .putString("draft_content", newState.content)
                .apply()
        }
    }

    fun updateState(
        title: String = jointTitle,
        category: String = jointCategory,
        tag: String = jointTag,
        content: androidx.compose.ui.text.input.TextFieldValue = jointContent
    ) {
        jointTitle = title
        jointCategory = category
        jointTag = tag
        jointContent = content
        pushHistory(MergeState(title, category, content.text, tag))
    }

    // Initialize content from pre-selected notes or draft if available
    LaunchedEffect(selectedInspirations) {
        if (selectedInspirations.isNotEmpty()) {
            val initialContent = selectedInspirations.map { it.content }.joinToString("\n\n")
            val firstNote = selectedInspirations.firstOrNull()
            val title = firstNote?.title?.takeIf { it.isNotBlank() }?.let { "${it}等接合" } ?: "新接合文档"
            val category = firstNote?.category ?: ""
            val initialTags = selectedInspirations.mapNotNull { it.tag.takeIf { t -> t.isNotBlank() } }.flatMap { it.split(",") }.map { it.trim() }.distinct().joinToString(",")
            
            jointTitle = title
            jointCategory = category
            jointTag = initialTags
            jointContent = androidx.compose.ui.text.input.TextFieldValue(initialContent)
            historyStack.clear()
            historyStack.add(MergeState(title, category, initialContent, initialTags))
            historyIndex = 0
        } else if (jointContent.text.isEmpty()) {
            // Check if there is a saved draft
            val savedTitle = sharedPrefs.getString("draft_title", null)
            val savedContent = sharedPrefs.getString("draft_content", null)
            val savedTag = sharedPrefs.getString("draft_tag", "") ?: ""
            val savedBlocksJson = sharedPrefs.getString("draft_blocks", null)
            if (!savedContent.isNullOrBlank()) {
                jointTitle = savedTitle ?: ""
                jointCategory = sharedPrefs.getString("draft_category", "") ?: ""
                jointTag = savedTag
                jointContent = androidx.compose.ui.text.input.TextFieldValue(savedContent)
                historyStack.clear()
                pushHistory(MergeState(jointTitle, jointCategory, savedContent, jointTag))
            } else if (savedBlocksJson != null) {
                jointTitle = savedTitle ?: ""
                jointCategory = sharedPrefs.getString("draft_category", "") ?: ""
                jointTag = savedTag
                val blocksStr = try {
                    val jsonArray = JSONArray(savedBlocksJson)
                    val blocks = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        blocks.add(obj.getString("text"))
                    }
                    blocks.joinToString("\n\n")
                } catch (e: Exception) {
                    ""
                }
                jointContent = androidx.compose.ui.text.input.TextFieldValue(blocksStr)
                historyStack.clear()
                pushHistory(MergeState(jointTitle, jointCategory, blocksStr, jointTag))
            } else {
                updateState(title = "", category = "", tag = "", content = androidx.compose.ui.text.input.TextFieldValue(""))
            }
        }
    }

    // Bottom sheet navigation & search states
    var showFullscreenPreview by remember(isPureMergePreview) { mutableStateOf(isPureMergePreview) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showNotesDrawer by remember { mutableStateOf(false) }
    var searchQueryInSheet by remember { mutableStateOf("") }
    var selectedGroupInSheet by remember { mutableStateOf<String?>(null) }

    // Draggable & Magnetic Docking FAB states
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(-240f) }
    var isDockedOnRight by remember { mutableStateOf(true) }
    var isFabFolded by remember { mutableStateOf(false) }
    var isFabDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isFabFolded, isFabDragging) {
        if (!isFabFolded && !isFabDragging) {
            kotlinx.coroutines.delay(3500)
            isFabFolded = true
        }
    }

    // Filter active (non-archived) notes
    val activeNotes = remember(inspirations) { inspirations.filter { !it.isArchived } }

    // Global dragging states
    var draggingNote by remember { mutableStateOf<Inspiration?>(null) }
    var dragStartScreenPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragCurrentScreenPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rootHeight by remember { mutableStateOf(0) }
    var rootWidth by remember { mutableStateOf(0) }
    var editAreaBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    val isHoveringEditArea = remember(draggingNote, dragCurrentScreenPos, editAreaBoundsInWindow, rootHeight) {
        if (draggingNote == null) false
        else if (!editAreaBoundsInWindow.isEmpty) {
            dragCurrentScreenPos.y <= editAreaBoundsInWindow.bottom && dragCurrentScreenPos.y >= editAreaBoundsInWindow.top - 120f
        } else {
            rootHeight > 0 && dragCurrentScreenPos.y < rootHeight * 0.75f
        }
    }

    val insertNoteToEditor: (Inspiration) -> Unit = { note ->
        val cursorPosition = jointContent.selection.end.coerceIn(0, jointContent.text.length)
        val currentText = jointContent.text
        val textBeforeCursor = currentText.substring(0, cursorPosition)
        val textAfterCursor = currentText.substring(cursorPosition)
        
        val separatorBefore = if (textBeforeCursor.isEmpty() || textBeforeCursor.endsWith("\n\n")) "" 
                              else if (textBeforeCursor.endsWith("\n")) "\n" 
                              else "\n\n"
        val separatorAfter = if (textAfterCursor.isEmpty() || textAfterCursor.startsWith("\n\n")) "" 
                             else if (textAfterCursor.startsWith("\n")) "\n" 
                             else "\n\n"
        
        val insertText = separatorBefore + note.content + separatorAfter
        val newContentText = textBeforeCursor + insertText + textAfterCursor
        val newCursorPosition = cursorPosition + insertText.length
        
        val newContent = androidx.compose.ui.text.input.TextFieldValue(
            text = newContentText,
            selection = androidx.compose.ui.text.TextRange(newCursorPosition)
        )
        updateState(content = newContent)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("已成功拼入笔记《${note.title.ifBlank { "未命名" }}》")
        }
    }

    val handleDragStart: (Inspiration, androidx.compose.ui.geometry.Offset) -> Unit = { note, initialPos ->
        draggingNote = note
        dragStartScreenPos = initialPos
        dragCurrentScreenPos = initialPos
        showNotesDrawer = false
    }

    val handleDrag: (androidx.compose.ui.geometry.Offset) -> Unit = { amount ->
        dragCurrentScreenPos = dragCurrentScreenPos + amount
    }

    val handleDragEnd: () -> Unit = {
        val note = draggingNote
        if (note != null) {
            insertNoteToEditor(note)
        }
        draggingNote = null
        dragStartScreenPos = androidx.compose.ui.geometry.Offset.Zero
        dragCurrentScreenPos = androidx.compose.ui.geometry.Offset.Zero
    }

    var textToolbarRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var onCopyMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onPasteMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onCutMenu: (() -> Unit)? by remember { mutableStateOf(null) }
    var onSelectAllMenu: (() -> Unit)? by remember { mutableStateOf(null) }

    LaunchedEffect(jointContent.selection) {
        if (jointContent.selection.length == 0) {
            textToolbarRect = null
        }
    }

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

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalTextToolbar provides customTextToolbar) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isPureMergePreview) "合并预览" else "拼文", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭并返回")
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(
                        enabled = historyIndex > 0,
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                val state = historyStack[historyIndex]
                                jointTitle = state.title
                                jointCategory = state.category
                                jointTag = state.tag
                                jointContent = androidx.compose.ui.text.input.TextFieldValue(state.content)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已撤销最近一次的操作")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销",
                            tint = if (historyIndex > 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // Redo Button
                    IconButton(
                        enabled = historyIndex < historyStack.lastIndex,
                        onClick = {
                            if (historyIndex < historyStack.lastIndex) {
                                historyIndex++
                                val state = historyStack[historyIndex]
                                jointTitle = state.title
                                jointCategory = state.category
                                jointTag = state.tag
                                jointContent = androidx.compose.ui.text.input.TextFieldValue(state.content)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已重做最近一次的操作")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "重做",
                            tint = if (historyIndex < historyStack.lastIndex) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Export Button
                    IconButton(
                        onClick = {
                            if (jointContent.text.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("正文内容为空，无法导出")
                                }
                            } else {
                                showExportSheet = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "导出拼文",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Preview Button (禁用纯多选合并预览模式下的关闭/开启，防止误入编辑界面)
                    IconButton(
                        enabled = !isPureMergePreview,
                        onClick = {
                            if (!isPureMergePreview) {
                                showFullscreenPreview = !showFullscreenPreview
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (showFullscreenPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showFullscreenPreview) "取消预览" else "预览拼文",
                            tint = if (!isPureMergePreview) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                        )
                    }

                    // Save Button
                    IconButton(
                        onClick = {
                            val finalContent = jointContent.text.trim()
                            if (finalContent.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("正文内容不能为空，请先输入或拖拽填入笔记")
                                }
                                return@IconButton
                            }
                            coroutineScope.launch {
                                val finalTitle = jointTitle.trim().ifBlank { "未命名接合文档" }
                                val newId = viewModel.saveInspiration(
                                    title = finalTitle,
                                    content = finalContent,
                                    tag = jointTag,
                                    category = jointCategory,
                                    isPinned = false,
                                    isArchived = false
                                )

                                // Archive original notes used in joint
                                if (selectedInspirations.isNotEmpty()) {
                                    viewModel.archiveInspirations(selectedInspirations, true)
                                }
                                
                                // Clear draft upon successful save
                                sharedPrefs.edit().clear().apply()

                                snackbarHostState.showSnackbar("文档保存成功")
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = Color(0xFFFBFBFD)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .onGloballyPositioned { coords ->
                    rootHeight = coords.size.height
                    rootWidth = coords.size.width
                }
        ) {
            // Animating background and border highlights during hovering/drag-over
            val editAreaBgColor by animateColorAsState(
                targetValue = if (isHoveringEditArea) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else Color.White,
                animationSpec = tween(250),
                label = "EditAreaBg"
            )
            val editAreaBorderColor by animateColorAsState(
                targetValue = if (isHoveringEditArea) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color(0xFFE5E5EA),
                animationSpec = tween(250),
                label = "EditAreaBorder"
            )


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 48.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                // Category Chip selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "目标分组: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    
                    var showCategoryDropdown by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            text = if (jointCategory.isBlank()) "未分类" else jointCategory,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showCategoryDropdown = true }
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("未分类") },
                                onClick = {
                                    jointCategory = ""
                                    showCategoryDropdown = false
                                }
                            )
                            allGroups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.name) },
                                    onClick = {
                                        jointCategory = g.name
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Title Input
                TextField(
                    value = jointTitle,
                    onValueChange = { jointTitle = it },
                    placeholder = { Text("输入接合后的新标题...", style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF8E8E93), letterSpacing = (-0.3).sp)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = (-0.4).sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    singleLine = true
                )

                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Content Input in unified editor body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            val size = coords.size
                            editAreaBoundsInWindow = androidx.compose.ui.geometry.Rect(
                                left = pos.x,
                                top = pos.y,
                                right = pos.x + size.width,
                                bottom = pos.y + size.height
                            )
                        }
                ) {
                    OutlinedTextField(
                        value = jointContent,
                        onValueChange = { newContent ->
                            jointContent = newContent
                            updateState(content = newContent)
                        },
                        placeholder = { Text("拖拽下方的笔记卡片到此处，或在此处自由编辑和合并笔记正文...", fontSize = 14.sp, color = Color(0xFF8E8E93)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF1D1D1F),
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        ),
                        visualTransformation = MarkdownAndTagVisualTransformation(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Flat Bottom Markdown Formatting Toolbar (Floating/Attached above soft keyboard via imePadding)
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 撤销与重做移到最左侧
                        IconButton(
                            onClick = {
                                if (historyIndex > 0) {
                                    historyIndex--
                                    val state = historyStack[historyIndex]
                                    jointTitle = state.title
                                    jointCategory = state.category
                                    jointTag = state.tag
                                    jointContent = TextFieldValue(state.content, selection = TextRange(state.content.length))
                                }
                            },
                            enabled = historyIndex > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "撤销",
                                tint = if (historyIndex > 0) Color.Gray else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (historyIndex < historyStack.lastIndex) {
                                    historyIndex++
                                    val state = historyStack[historyIndex]
                                    jointTitle = state.title
                                    jointCategory = state.category
                                    jointTag = state.tag
                                    jointContent = TextFieldValue(state.content, selection = TextRange(state.content.length))
                                }
                            },
                            enabled = historyIndex < historyStack.lastIndex,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "重做",
                                tint = if (historyIndex < historyStack.lastIndex) Color.Gray else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                        IconButton(onClick = {
                            val text = jointContent.text
                            val selection = jointContent.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "**" + selected + "**" + after
                            val newSelection = TextRange(min + 2 + selected.length + 2)
                            jointContent = TextFieldValue(newValue, selection = newSelection)
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatBold, contentDescription = "加粗", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = jointContent.text
                            val selection = jointContent.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "*" + selected + "*" + after
                            val newSelection = TextRange(min + 1 + selected.length + 1)
                            jointContent = TextFieldValue(newValue, selection = newSelection)
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "斜体", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = jointContent.text
                            val selection = jointContent.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val newValue = before + "==" + selected + "==" + after
                            val newSelection = TextRange(min + 2 + selected.length + 2)
                            jointContent = TextFieldValue(newValue, selection = newSelection)
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.BorderColor, contentDescription = "高亮", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = jointContent.text
                            val selection = jointContent.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val insertText = if (before.isEmpty() || before.last() == '\n') "- $selected" else "\n- $selected"
                            val newValue = before + insertText + after
                            val newSelection = TextRange(min + insertText.length)
                            jointContent = TextFieldValue(newValue, selection = newSelection)
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val text = jointContent.text
                            val selection = jointContent.selection
                            val min = selection.min
                            val max = selection.max
                            val before = text.substring(0, min)
                            val after = text.substring(max)
                            val selected = text.substring(min, max)
                            val insertText = if (before.isEmpty() || before.last() == '\n') "> $selected" else "\n> $selected"
                            val newValue = before + insertText + after
                            val newSelection = TextRange(min + insertText.length)
                            jointContent = TextFieldValue(newValue, selection = newSelection)
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "引用", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = {
                            val currentText = jointContent.text
                            val min = jointContent.selection.min
                            val max = jointContent.selection.max
                            val insertTag = "#"
                            val newValue = currentText.substring(0, min) + insertTag + currentText.substring(max)
                            jointContent = TextFieldValue(newValue, selection = TextRange(min + 1))
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Text("#", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }

                        Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                        IconButton(onClick = {
                            val currentText = jointContent.text
                            val min = jointContent.selection.min
                            val max = jointContent.selection.max
                            val selected = currentText.substring(min, max)
                            val newValue = currentText.substring(0, min) + "`" + selected + "`" + currentText.substring(max)
                            jointContent = TextFieldValue(newValue, selection = TextRange(min + 1 + selected.length + 1))
                            updateState(content = jointContent)
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Code, contentDescription = "代码", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        }

                        Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                        var showTagManagerDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showTagManagerDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.LocalOffer, contentDescription = "管理标签", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }

                        if (showTagManagerDialog) {
                            var newTagInput by remember { mutableStateOf("") }
                            val currentTagsList = remember(jointTag) {
                                jointTag.split(",").map { it.trim() }.filter { it.isNotEmpty() }
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
                                                            jointTag = updatedList.joinToString(",")
                                                            updateState(tag = jointTag)
                                                            
                                                            val tagPattern = "#$t"
                                                            var updatedText = jointContent.text
                                                            if (updatedText.contains(tagPattern)) {
                                                                updatedText = updatedText.replace(tagPattern, "").replace("  ", " ").trim()
                                                                jointContent = TextFieldValue(
                                                                    text = updatedText,
                                                                    selection = TextRange(updatedText.length)
                                                                )
                                                                updateState(content = jointContent)
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
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                            labelColor = MaterialTheme.colorScheme.primary
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
                                                textStyle = MaterialTheme.typography.bodyMedium
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    val trimmed = newTagInput.trim()
                                                    if (trimmed.isNotEmpty() && !currentTagsList.contains(trimmed)) {
                                                        val updatedList = currentTagsList.toMutableList().apply { add(trimmed) }
                                                        jointTag = updatedList.joinToString(",")
                                                        updateState(tag = jointTag)
                                                        
                                                        val currentText = jointContent.text
                                                        val tagString = "#$trimmed"
                                                        val appendix = if (currentText.isEmpty()) {
                                                            tagString
                                                        } else if (currentText.endsWith("\n") || currentText.endsWith(" ")) {
                                                            tagString
                                                        } else {
                                                            " $tagString"
                                                        }
                                                        jointContent = TextFieldValue(
                                                            text = currentText + appendix,
                                                            selection = TextRange(currentText.length + appendix.length)
                                                        )
                                                        updateState(content = jointContent)
                                                        newTagInput = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("添加", color = Color.White)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showTagManagerDialog = false }) {
                                        Text("完成")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 磁吸左右可自动折叠收起的悬浮球：收纳“分类与分组”灵感库
            val density = androidx.compose.ui.platform.LocalDensity.current
            val fabAnimatedOffsetX by animateFloatAsState(
                targetValue = if (isFabFolded) {
                    if (isDockedOnRight) with(density) { 32.dp.toPx() } else with(density) { (-32).dp.toPx() }
                } else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabFoldOffset"
            )

            val fabAlignment = if (isDockedOnRight) Alignment.CenterEnd else Alignment.CenterStart

            Surface(
                onClick = {
                    if (isFabFolded) {
                        isFabFolded = false
                    } else {
                        showNotesDrawer = true
                    }
                },
                shape = RoundedCornerShape(
                    topStart = if (isDockedOnRight) 24.dp else 0.dp,
                    bottomStart = if (isDockedOnRight) 24.dp else 0.dp,
                    topEnd = if (isDockedOnRight) 0.dp else 24.dp,
                    bottomEnd = if (isDockedOnRight) 0.dp else 24.dp
                ),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(fabAlignment)
                    .offset {
                        IntOffset(
                            (fabOffsetX + fabAnimatedOffsetX).roundToInt(),
                            fabOffsetY.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isFabDragging = true
                                isFabFolded = false
                            },
                            onDragEnd = {
                                isFabDragging = false
                                if (fabOffsetX < -rootWidth / 3f && isDockedOnRight) {
                                    isDockedOnRight = false
                                } else if (fabOffsetX > rootWidth / 3f && !isDockedOnRight) {
                                    isDockedOnRight = true
                                }
                                fabOffsetX = 0f
                            },
                            onDragCancel = {
                                isFabDragging = false
                                fabOffsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                fabOffsetX += dragAmount.x
                                fabOffsetY += dragAmount.y
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isDockedOnRight && isFabFolded) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = "分类与分组",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    if (!isFabFolded) {
                        Text(
                            text = "灵感库",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isDockedOnRight && isFabFolded) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            QuickPhraseCapsuleWidget(
                phrases = quickPhrases,
                brandColor = brandColor,
                onPhraseClick = { phrase ->
                    val phraseText = phrase.content
                    val text = jointContent.text
                    val selection = jointContent.selection
                    val min = selection.min
                    val max = selection.max
                    val before = text.substring(0, min)
                    val after = text.substring(max)
                    val newValue = before + phraseText + after
                    val newSelection = TextRange(min + phraseText.length)
                    jointContent = TextFieldValue(newValue, selection = newSelection)
                    updateState(content = jointContent)
                    viewModel.incrementQuickPhraseUsage(phrase.id)
                },
                onPhraseUpdate = { id, label, content ->
                    viewModel.updateQuickPhrase(id, label, content)
                },
                onPhraseDelete = { id ->
                    viewModel.deleteQuickPhrase(id)
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 80.dp)
            )

            // 导出选择弹框 ModalBottomSheet
            if (showExportSheet) {
                ExportBottomSheet(
                    onDismissRequest = { showExportSheet = false },
                    context = context,
                    title = jointTitle,
                    content = jointContent.text,
                    onResult = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )
            }

            // 抽屉式面板 ModalBottomSheet (无底栏残存痕迹)
            if (showNotesDrawer) {
                ModalBottomSheet(
                    onDismissRequest = { showNotesDrawer = false },
                    containerColor = Color(0xFFFBFBFD),
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.55f)
                            .padding(bottom = 24.dp)
                    ) {
                        // 搜索框
                        OutlinedTextField(
                            value = searchQueryInSheet,
                            onValueChange = { searchQueryInSheet = it },
                            placeholder = { Text("搜索灵感笔记...", fontSize = 14.sp, color = Color(0xFF8E8E93)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8E8E93)) },
                            trailingIcon = {
                                if (searchQueryInSheet.isNotEmpty()) {
                                    IconButton(onClick = { searchQueryInSheet = "" }) {
                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp), tint = Color(0xFF8E8E93))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE5E5EA),
                                focusedPlaceholderColor = Color(0xFF8E8E93),
                                unfocusedPlaceholderColor = Color(0xFF8E8E93)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (searchQueryInSheet.isNotEmpty()) {
                            // 搜索结果模式
                            val filteredNotesInSheet = activeNotes.filter {
                                it.title.contains(searchQueryInSheet, ignoreCase = true) ||
                                it.content.contains(searchQueryInSheet, ignoreCase = true)
                            }

                            if (filteredNotesInSheet.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("无相关匹配灵感", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                                    Text("搜寻结果 (${filteredNotesInSheet.size}条)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(filteredNotesInSheet) { note ->
                                            DraggableNoteCard(
                                                note = note,
                                                isDraggingGlobally = (draggingNote?.id == note.id),
                                                onDragStart = handleDragStart,
                                                onDrag = handleDrag,
                                                onDragEnd = handleDragEnd,
                                                onClick = { insertNoteToEditor(note) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 分组导航与卡片浏览模式
                            AnimatedContent(
                                targetState = selectedGroupInSheet,
                                transitionSpec = {
                                    if (targetState != null) {
                                        (fadeIn(animationSpec = tween(280)) + slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth / 3 }, animationSpec = tween(280))) togetherWith
                                        (fadeOut(animationSpec = tween(220)) + slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }, animationSpec = tween(220)))
                                    } else {
                                        (fadeIn(animationSpec = tween(280)) + slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }, animationSpec = tween(280))) togetherWith
                                        (fadeOut(animationSpec = tween(220)) + slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth / 3 }, animationSpec = tween(220)))
                                    }
                                },
                                label = "FolderNotesTransition"
                            ) { targetGroup ->
                                if (targetGroup == null) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                                        Text("分类与分组 (点击文件夹进入笔记卡片列表)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93), letterSpacing = 0.3.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        val folderItems = remember(activeNotes, allGroups) {
                                            val list = mutableListOf<FolderItemData>()
                                            list.add(FolderItemData("全部笔记", activeNotes.size, Icons.Default.AllInbox))
                                            val uncategorizedNotesCount = activeNotes.count { it.category.isBlank() }
                                            list.add(FolderItemData("无分类", uncategorizedNotesCount, Icons.Outlined.FolderOpen))
                                            allGroups.forEach { group ->
                                                val count = activeNotes.count { it.category == group.name }
                                                list.add(FolderItemData(group.name, count, Icons.Default.Folder))
                                            }
                                            list
                                        }

                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(bottom = 16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(folderItems) { folder ->
                                                FolderCard(
                                                    name = folder.name,
                                                    noteCount = folder.noteCount,
                                                    icon = folder.icon,
                                                    backgroundColor = Color.White,
                                                    onClick = { selectedGroupInSheet = folder.name }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val groupName = targetGroup
                                    val notesInGroup = remember(groupName, activeNotes) {
                                        when (groupName) {
                                            "全部笔记" -> activeNotes
                                            "无分类" -> activeNotes.filter { it.category.isBlank() }
                                            else -> activeNotes.filter { it.category == groupName }
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { selectedGroupInSheet = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "分组: $groupName (${notesInGroup.size}条)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (notesInGroup.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("该分组暂无笔记内容", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        } else {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(bottom = 16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(notesInGroup, key = { it.id }) { note ->
                                                    DraggableNoteCard(
                                                        note = note,
                                                        isDraggingGlobally = (draggingNote?.id == note.id),
                                                        onDragStart = handleDragStart,
                                                        onDrag = handleDrag,
                                                        onDragEnd = handleDragEnd,
                                                        onClick = { insertNoteToEditor(note) }
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
            }

            // Follow-touch floating note card preview (Thumbnail)
            if (draggingNote != null) {
                val floatingNote = draggingNote!!
                Popup(
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                        clippingEnabled = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (dragCurrentScreenPos.x - 85.dp.toPx()).roundToInt(),
                                    (dragCurrentScreenPos.y - 55.dp.toPx()).roundToInt()
                                )
                            }
                            .size(170.dp, 110.dp)
                            .graphicsLayer {
                                scaleX = 1.08f
                                scaleY = 1.08f
                                rotationZ = -3f // tactile diagonal tilt
                                alpha = 0.9f // premium translucent visual feedback
                                shadowElevation = 16.dp.toPx()
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                            }
                            .background(Color.White)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = floatingNote.title.ifBlank { "无标题" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = floatingNote.content,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. 全屏沉浸式拼文预览界面
            if (showFullscreenPreview) {
                BackHandler(enabled = true) {
                    showFullscreenPreview = false
                }
            }

            AnimatedVisibility(
                visible = showFullscreenPreview,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize().zIndex(100f)
            ) {
                val finalContent = jointContent.text.trim()
                val finalTitle = jointTitle.trim().ifBlank { "未命名接合文档" }
                val blockCount = if (finalContent.isBlank()) 0 else finalContent.split("\n\n").filter { it.isNotBlank() }.size
                val charCount = finalContent.length

                Surface(
                    color = Color(0xFFFCFAF6),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 48.dp)
                    ) {
                        // 1. 大气排版书卷大标题 (直接靠顶显示，无冗余顶部导航栏)
                        Text(
                            text = finalTitle,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50),
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. 信息栏
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "分类: ${jointCategory.ifBlank { "无分类" }}",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = "拼接: ${blockCount}个片段",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = "共 ${charCount} 字",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. 极简分隔线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. 正文内容展示
                        if (finalContent.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无拼文内容，请先在下方抽屉拖入或输入笔记",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            SelectionContainer {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    MarkdownRenderer(
                                        markdown = finalContent,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
        
        if (textToolbarRect != null) {
            CustomToolbarPopup(
                rect = textToolbarRect!!,
                onCopy = onCopyMenu,
                onPaste = onPasteMenu,
                onCut = onCutMenu,
                onSelectAll = onSelectAllMenu,
                onDismiss = { textToolbarRect = null },
                contentValue = jointContent,
                onValueChange = { 
                    jointContent = it
                    updateState(content = it)
                }
            )
        }
    }
}
}

data class FolderItemData(
    val name: String,
    val noteCount: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun FolderCard(
    name: String,
    noteCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$noteCount 篇内容",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DraggableNoteCard(
    note: Inspiration,
    isDraggingGlobally: Boolean,
    onDragStart: (Inspiration, androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit
) {
    var cardWindowOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val alphaValue = if (isDraggingGlobally) 0.3f else 1f

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(110.dp)
            .onGloballyPositioned { coordinates ->
                cardWindowOffset = coordinates.positionInWindow()
            }
            .pointerInput(note) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    var isDragging = false
                    var dragSum = androidx.compose.ui.geometry.Offset.Zero
                    val touchSlop = viewConfiguration.touchSlop

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.find { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (isDragging) {
                                change.consume()
                                onDragEnd()
                            } else {
                                onClick()
                            }
                            break
                        }

                        val dragAmount = change.positionChange()
                        dragSum += dragAmount

                        if (!isDragging && dragSum.getDistance() > touchSlop) {
                            isDragging = true
                            onDragStart(note, cardWindowOffset + change.position)
                        }

                        if (isDragging) {
                            change.consume()
                            onDrag(dragAmount)
                        }
                    }
                }
            }
            .graphicsLayer {
                alpha = alphaValue
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = if (isDraggingGlobally) 2.dp else 1.dp,
            color = if (isDraggingGlobally) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color(0xFFE5E2D9).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDraggingGlobally) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = note.title.ifBlank { "无标题" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "按住拖拽",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "填入正文",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "填入",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun exportAsPdf(context: android.content.Context, title: String, content: String): File? {
    return try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = Paint().apply {
            color = android.graphics.Color.rgb(30, 30, 30)
            textSize = 14f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(20, 20, 20)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        var y = 60f
        val docTitle = title.trim().ifBlank { "拼文文档" }
        canvas.drawText(docTitle, 40f, y, titlePaint)
        y += 40f
        
        val lines = content.split("\n")
        for (line in lines) {
            if (y > 800f) break
            if (line.isBlank()) {
                y += 18f
                continue
            }
            var start = 0
            while (start < line.length && y < 800f) {
                val count = paint.breakText(line, start, line.length, true, 515f, null)
                val chunk = line.substring(start, start + count)
                canvas.drawText(chunk, 40f, y, paint)
                y += 22f
                start += count
            }
        }
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "${docTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun exportAsImage(context: android.content.Context, title: String, content: String): File? {
    return try {
        val width = 800
        val padding = 40
        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(44, 44, 44)
            textSize = 28f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(30, 30, 30)
            textSize = 38f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val lines = content.split("\n")
        var totalHeight = padding * 2 + 70
        for (line in lines) {
            if (line.isBlank()) {
                totalHeight += 24
                continue
            }
            var start = 0
            while (start < line.length) {
                val count = linePaint.breakText(line, start, line.length, true, (width - padding * 2).toFloat(), null)
                totalHeight += 40
                start += count
            }
        }
        totalHeight = totalHeight.coerceAtLeast(400)
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(252, 250, 246))
        
        var y = (padding + 40).toFloat()
        val docTitle = title.trim().ifBlank { "拼文文档" }
        canvas.drawText(docTitle, padding.toFloat(), y, titlePaint)
        y += 50f
        
        for (line in lines) {
            if (line.isBlank()) {
                y += 24f
                continue
            }
            var start = 0
            while (start < line.length) {
                val count = linePaint.breakText(line, start, line.length, true, (width - padding * 2).toFloat(), null)
                val chunk = line.substring(start, start + count)
                canvas.drawText(chunk, padding.toFloat(), y, linePaint)
                y += 40f
                start += count
            }
        }
        
        val file = File(context.cacheDir, "${docTitle}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun exportAsMarkdown(context: android.content.Context, title: String, content: String): File? {
    return try {
        val docTitle = title.trim().ifBlank { "拼文文档" }
        val file = File(context.cacheDir, "${docTitle}_${System.currentTimeMillis()}.md")
        val mdContent = "# $docTitle\n\n$content"
        file.writeText(mdContent)
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun shareExportedFile(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出拼文"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
