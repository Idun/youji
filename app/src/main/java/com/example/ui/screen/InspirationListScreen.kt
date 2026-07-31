package com.example.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Inspiration
import com.example.ui.viewmodel.InspirationViewModel
import com.example.ui.viewmodel.SortOrder
import com.example.ui.viewmodel.SortType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.domain.model.GroupInfo
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.FileProvider
import java.io.File

val colorOptions = listOf(
    "#F28B82", // Warm pastel red
    "#AECBFA", // Soft material blue
    "#CCFF90", // Vibrant light green
    "#FFF475", // Warm yellow
    "#FBBC04", // Bright amber
    "#D7AEFB", // Lavender
    "#FDCFE8", // Pastel pink
    "#E5E2D9"  // Warm grey
)

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF7F7F7F)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InspirationListScreen(
    viewModel: InspirationViewModel,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToMergePreview: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val inspirations by viewModel.inspirations.collectAsState()
    val currentThemeColorVal by viewModel.themeColor.collectAsState()
    val brandColor = remember(currentThemeColorVal) { Color(currentThemeColorVal) }
    val allTags by viewModel.allTags.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val tagSearchQuery by viewModel.tagSearchQuery.collectAsState()
    val tagTimeFilter by viewModel.tagTimeFilter.collectAsState()

    var singleNoteForTagAction by remember { mutableStateOf<Inspiration?>(null) }
    var singleNoteForGroupAction by remember { mutableStateOf<Inspiration?>(null) }

    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val screenCreatedTime = remember { System.currentTimeMillis() }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Int>() }
    
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchGroupDialog by remember { mutableStateOf(false) }
    
    var showJointDialog by remember { mutableStateOf(false) }
    var notesToJoint by remember { mutableStateOf<List<Inspiration>>(emptyList()) }
    
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val verticalBubbleShape = remember(density) {
        val cornerRadiusPx = with(density) { 22.dp.toPx() }
        val tailWidthPx = with(density) { 14.dp.toPx() }
        val tailHeightPx = with(density) { 12.dp.toPx() }

        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val mainH = h - tailHeightPx
            val cr = cornerRadiusPx.coerceAtMost(minOf(w, mainH) / 2f)

            moveTo(cr, 0f)
            lineTo(w - cr, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w - 2 * cr, 0f, w, 2 * cr),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(w, mainH - cr)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w - 2 * cr, mainH - 2 * cr, w, mainH),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 底部尾巴指向 FAB 按钮
            val tailCenterX = w / 2f
            val tailLeftX = tailCenterX - tailWidthPx / 2f
            val tailRightX = tailCenterX + tailWidthPx / 2f

            lineTo(tailRightX, mainH)
            lineTo(tailCenterX, h)
            lineTo(tailLeftX, mainH)

            lineTo(cr, mainH)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, mainH - 2 * cr, 2 * cr, mainH),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, cr)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, 2 * cr, 2 * cr),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
    }
    
    var isAllNotesExpanded by remember { mutableStateOf(true) }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    var isGridView by remember { mutableStateOf(false) }
    
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedItems.clear()
    }
    
    if (showNewGroupDialog) {
        var selectedColorHex by remember { mutableStateOf(colorOptions[0]) }
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("新建分组") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("分组名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("选择分组颜色", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(parseHexColor(hex), CircleShape)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newGroupName.isNotBlank()) {
                        viewModel.addGroup(newGroupName, selectedColorHex)
                        viewModel.setSelectedGroup(newGroupName.trim())
                    }
                    showNewGroupDialog = false
                    newGroupName = ""
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("取消") }
            }
        )
    }

    if (showBatchTagDialog) {
        var batchTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBatchTagDialog = false },
            title = { Text("批量打标签") },
            text = {
                OutlinedTextField(
                    value = batchTag,
                    onValueChange = { batchTag = it },
                    label = { Text("标签名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (batchTag.isNotBlank()) {
                        val selectedInspirations = inspirations.filter { it.id in selectedItems }
                        viewModel.tagInspirations(selectedInspirations, batchTag.trim())
                        selectionMode = false
                        selectedItems.clear()
                    }
                    showBatchTagDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchTagDialog = false }) { Text("取消") }
            }
        )
    }

    if (showBatchGroupDialog) {
        AlertDialog(
            onDismissRequest = { showBatchGroupDialog = false },
            title = { Text("批量移动到分组") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val selectedInspirations = inspirations.filter { it.id in selectedItems }
                                    viewModel.moveInspirationsToGroup(selectedInspirations, "全部笔记")
                                    selectionMode = false
                                    selectedItems.clear()
                                    showBatchGroupDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("无分类 (移出分组)", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(allGroups) { group ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val selectedInspirations = inspirations.filter { it.id in selectedItems }
                                    viewModel.moveInspirationsToGroup(selectedInspirations, group.name)
                                    selectionMode = false
                                    selectedItems.clear()
                                    showBatchGroupDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = parseHexColor(group.colorHex))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(group.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchGroupDialog = false }) { Text("取消") }
            }
        )
    }

    if (showJointDialog) {
        var orderedNotes by remember { mutableStateOf(notesToJoint) }
        var titleEditedByUser by remember { mutableStateOf(false) }
        var jointTitle by remember { 
            mutableStateOf(notesToJoint.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: "无标题灵感") 
        }
        
        LaunchedEffect(orderedNotes) {
            if (!titleEditedByUser) {
                jointTitle = orderedNotes.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: "无标题灵感"
            }
        }
        

        
        var jointCategory by remember { mutableStateOf(notesToJoint.firstOrNull()?.category ?: "") }
        
        fun moveUp(index: Int) {
            if (index > 0) {
                val list = orderedNotes.toMutableList()
                val temp = list[index]
                list[index] = list[index - 1]
                list[index - 1] = temp
                orderedNotes = list
            }
        }
        

        
        fun moveDown(index: Int) {
            if (index < orderedNotes.size - 1) {
                val list = orderedNotes.toMutableList()
                val temp = list[index]
                list[index] = list[index + 1]
                list[index + 1] = temp
                orderedNotes = list
            }
        }
        


        AlertDialog(
            onDismissRequest = { 
                showJointDialog = false
                notesToJoint = emptyList()
            },
            title = { Text("接合文档") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = jointTitle,
                        onValueChange = { 
                            jointTitle = it 
                            titleEditedByUser = true
                        },
                        label = { Text("接合笔记标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var showGroupDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showGroupDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "目标分组: ${if (jointCategory.isBlank()) "无分类" else jointCategory}")
                        }
                        DropdownMenu(
                            expanded = showGroupDropdown,
                            onDismissRequest = { showGroupDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无分类") },
                                onClick = {
                                    jointCategory = ""
                                    showGroupDropdown = false
                                }
                            )
                            allGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        jointCategory = group.name
                                        showGroupDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Text("编排卡片顺序 (可调整排序)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(4.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(orderedNotes) { idx, note ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (note.title.isNotBlank()) note.title else "无标题灵感",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (note.content.isNotBlank()) {
                                                Text(
                                                    text = note.content,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { moveUp(idx) },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "上移",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (idx > 0) MaterialTheme.colorScheme.primary else Color.Gray
                                                )
                                            }
                                            IconButton(
                                                onClick = { moveDown(idx) },
                                                enabled = idx < orderedNotes.size - 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "下移",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (idx < orderedNotes.size - 1) MaterialTheme.colorScheme.primary else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (orderedNotes.isNotEmpty()) {
                        scope.launch {
                            val combinedContent = buildString {
                                orderedNotes.forEachIndexed { idx, note ->
                                    if (idx > 0) {
                                        append("\n\n---\n\n")
                                    }
                                    append("## ")
                                    append(if (note.title.isNotBlank()) note.title else "无标题灵感")
                                    append("\n\n")
                                    if (note.tag.isNotBlank()) {
                                        append("标签: ")
                                        append(note.tag.split(",").joinToString(" ") { "#$it" })
                                        append("\n\n")
                                    }
                                    append(note.content)
                                }
                            }
                            val newId = viewModel.saveInspiration(
                                title = jointTitle,
                                content = combinedContent,
                                tag = "",
                                category = jointCategory,
                                isPinned = false,
                                isArchived = false
                            )
                            
                            // Archive original notes
                            viewModel.archiveInspirations(orderedNotes, true)
                            
                            showJointDialog = false
                            notesToJoint = emptyList()
                            selectionMode = false
                            selectedItems.clear()
                            
                            // Show Snackbar with Undo action
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "接合成功，原零散卡片已归档",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Long
                            )
                            
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                // Undo: delete new note, un-archive original notes
                                viewModel.deleteInspiration(
                                    com.example.domain.model.Inspiration(id = newId, title = "", content = "")
                                )
                                viewModel.archiveInspirations(orderedNotes, false)
                                snackbarHostState.showSnackbar("已撤销接合，原始卡片已恢复")
                            }
                        }
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJointDialog = false
                    notesToJoint = emptyList()
                }) { Text("取消") }
            }
        )
    }

    if (singleNoteForTagAction != null) {
        val note = singleNoteForTagAction!!
        var tagValue by remember(note.id) { mutableStateOf(note.tag) }
        AlertDialog(
            onDismissRequest = { singleNoteForTagAction = null },
            title = { Text("给笔记添加标签") },
            text = {
                OutlinedTextField(
                    value = tagValue,
                    onValueChange = { tagValue = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tagInspirations(listOf(note), tagValue.trim())
                    singleNoteForTagAction = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { singleNoteForTagAction = null }) { Text("取消") }
            }
        )
    }

    if (singleNoteForGroupAction != null) {
        val note = singleNoteForGroupAction!!
        AlertDialog(
            onDismissRequest = { singleNoteForGroupAction = null },
            title = { Text("移动笔记到分组") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveInspirationsToGroup(listOf(note), "全部笔记")
                                    singleNoteForGroupAction = null
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("无分类 (移出分组)", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(allGroups) { group ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveInspirationsToGroup(listOf(note), group.name)
                                    singleNoteForGroupAction = null
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = parseHexColor(group.colorHex))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(group.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { singleNoteForGroupAction = null }) { Text("取消") }
            }
        )
    }

    var showRenameGroupDialog by remember { mutableStateOf(false) }
    var renameGroupOldName by remember { mutableStateOf("") }
    var renameGroupNewInput by remember { mutableStateOf("") }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var deleteGroupTargetName by remember { mutableStateOf("") }
    var activeTabGroupMenuName by remember { mutableStateOf<String?>(null) }

    if (showRenameGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("重命名分组", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请输入“$renameGroupOldName”的新名称：", fontSize = 13.sp, color = Color(0xFF666666))
                    OutlinedTextField(
                        value = renameGroupNewInput,
                        onValueChange = { renameGroupNewInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameGroupNewInput.trim()
                        if (trimmed.isNotEmpty() && trimmed != renameGroupOldName) {
                            viewModel.renameGroup(renameGroupOldName, trimmed)
                            scope.launch {
                                snackbarHostState.showSnackbar("已将分组重命名为“$trimmed”")
                            }
                        }
                        showRenameGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameGroupDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("删除分组", fontWeight = FontWeight.Bold) },
            text = {
                Text("确定要删除分组“$deleteGroupTargetName”吗？该分组下的笔记将归为未分类。", fontSize = 13.sp, color = Color(0xFF666666))
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteGroupTargetName.isNotEmpty()) {
                            viewModel.deleteGroup(deleteGroupTargetName)
                            scope.launch {
                                snackbarHostState.showSnackbar("已删除分组“$deleteGroupTargetName”")
                            }
                        }
                        showDeleteGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectionMode) {
                    TopAppBar(
                        title = { Text("已选择 ${selectedItems.size} 项", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedItems.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "取消选择", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = brandColor)
                    )
                }
            },
            floatingActionButton = {
                if (!selectionMode) {
                    val fabRotation by animateFloatAsState(
                        targetValue = if (isFabMenuExpanded) 135f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "fabRotation"
                    )
                    FloatingActionButton(
                        onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                        containerColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.errorContainer else brandColor,
                        contentColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isFabMenuExpanded) "关闭菜单" else "展开菜单",
                            modifier = Modifier.graphicsLayer(rotationZ = fabRotation)
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFBFBFD))
                ) {
                // Header (Menu, Search, Sort)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!selectionMode) {
                        IconButton(onClick = { onNavigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (isSearchFocused) 0.dp else 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { isSearchFocused = true }
                                )
                            },
                        placeholder = { Text("搜索...", color = Color(0xFF6B685C)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = Color(0xFF6B685C)
                            )
                        },
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序", tint = Color(0xFF6B685C))
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("修改时间 (倒序)") },
                                        onClick = {
                                            viewModel.setSort(SortType.MODIFIED_TIME, SortOrder.DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("修改时间 (正序)") },
                                        onClick = {
                                            viewModel.setSort(SortType.MODIFIED_TIME, SortOrder.ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("创建时间 (倒序)") },
                                        onClick = {
                                            viewModel.setSort(SortType.CREATED_TIME, SortOrder.DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("创建时间 (正序)") },
                                        onClick = {
                                            viewModel.setSort(SortType.CREATED_TIME, SortOrder.ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("标题 (A-Z)") },
                                        onClick = {
                                            viewModel.setSort(SortType.TITLE_ALPHA, SortOrder.ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("标题 (Z-A)") },
                                        onClick = {
                                            viewModel.setSort(SortType.TITLE_ALPHA, SortOrder.DESC)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = brandColor,
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.submitSearch(searchQuery)
                                focusManager.clearFocus()
                                isSearchFocused = false
                            }
                        )
                    )
                    
                    if (isSearchFocused) {
                        TextButton(onClick = {
                            focusManager.clearFocus()
                            isSearchFocused = false
                        }) {
                            Text("取消", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Groups Tabs with drag reordering and color indicators
                if (!isSearchFocused) {
                    val tabs = listOf(
                        GroupInfo("全部笔记", colorHex = "#7F7F7F"),
                        GroupInfo("未分类", colorHex = "#9E9E9E")
                    ) + allGroups

                    val currentTabName = when {
                        showArchived -> "归档箱"
                        selectedGroup == "未分类" -> "未分类"
                        selectedGroup == "全部笔记" -> "全部笔记"
                        else -> selectedGroup
                    }
                    val selectedTabIndex = tabs.indexOfFirst { it.name == currentTabName }.coerceAtLeast(0)
                    
                    val lazyRowState = rememberLazyListState()
                    val reorderableRowState = rememberReorderableLazyListState(lazyRowState) { from, to ->
                        if (from.index < 2 || to.index < 2) return@rememberReorderableLazyListState
                        viewModel.reorderGroups(from.index - 2, to.index - 2)
                    }

                    LazyRow(
                        state = lazyRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                onClick = { isGridView = !isGridView },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.25f)),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        contentDescription = if (isGridView) "切换为列表视图" else "切换为网格视图",
                                        modifier = Modifier.size(16.dp),
                                        tint = brandColor
                                    )
                                }
                            }
                        }
                        itemsIndexed(tabs, key = { _, tab -> tab.name }) { index, tab ->
                            ReorderableItem(reorderableRowState, key = tab.name) { isDragging ->
                                val isSelected = selectedTabIndex == index
                                val scale by animateFloatAsState(if (isDragging) 1.05f else 1.0f)
                                val containerColor = if (isSelected) {
                                    brandColor
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val textColor = if (isSelected) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Surface(
                                    modifier = Modifier
                                        .scale(scale)
                                        .longPressDraggableHandle(
                                            enabled = index >= 2
                                        )
                                        .clickable {
                                            when (tab.name) {
                                                "未分类" -> {
                                                    viewModel.setShowArchived(false)
                                                    viewModel.setSelectedGroup("未分类")
                                                    viewModel.setSelectedTag("")
                                                }
                                                "全部笔记" -> {
                                                    viewModel.setShowArchived(false)
                                                    viewModel.setSelectedGroup("全部笔记")
                                                    viewModel.setSelectedTag("")
                                                }
                                                else -> {
                                                    viewModel.setShowArchived(false)
                                                    viewModel.setSelectedGroup(tab.name)
                                                    viewModel.setSelectedTag("")
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = containerColor,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) brandColor else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (index >= 2) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        color = parseHexColor(tab.colorHex),
                                                        shape = CircleShape
                                                    )
                                            )
                                        } else {
                                            val icon = when (tab.name) {
                                                "全部笔记" -> Icons.Default.Notes
                                                "未分类" -> Icons.Default.FolderOpen
                                                else -> null
                                            }
                                            if (icon != null) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Text(
                                            text = tab.name,
                                            color = textColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )

                                        if (index >= 2) {
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            activeTabGroupMenuName = tab.name
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "分组菜单",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = textColor.copy(alpha = 0.85f)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = activeTabGroupMenuName == tab.name,
                                                    onDismissRequest = { activeTabGroupMenuName = null }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("重命名", fontSize = 13.sp) },
                                                        onClick = {
                                                            activeTabGroupMenuName = null
                                                            renameGroupOldName = tab.name
                                                            renameGroupNewInput = tab.name
                                                            showRenameGroupDialog = true
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("删除", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            activeTabGroupMenuName = null
                                                            deleteGroupTargetName = tab.name
                                                            showDeleteGroupDialog = true
                                                        }
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

                // Main Content Area
                if (isSearchFocused && searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("历史搜索", color = Color(0xFF6B685C), fontSize = 14.sp)
                            IconButton(onClick = { viewModel.clearSearchHistory() }) {
                                Icon(Icons.Default.Delete, contentDescription = "清除历史", modifier = Modifier.size(20.dp), tint = Color(0xFF6B685C))
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { history ->
                                Surface(
                                    color = Color(0xFFE5E2D9),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .clickable {
                                            viewModel.setSearchQuery(history)
                                            viewModel.submitSearch(history)
                                            focusManager.clearFocus()
                                            isSearchFocused = false
                                        }
                                ) {
                                    Text(
                                        text = history,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 14.sp,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                        }
                    }
                } else if (isSearchFocused && searchQuery.isEmpty()) {
                    // Empty search state
                } else {
                    // Note List
                    if (isGridView) {
                        val gridState = rememberLazyGridState()
                        val reorderableGridState = rememberReorderableLazyGridState(gridState) { from, to ->
                            viewModel.reorderInspirations(from.index, to.index, inspirations)
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(inspirations, key = { it.id }) { inspiration ->
                                val isSelected = selectedItems.contains(inspiration.id)
                                ReorderableItem(reorderableGridState, key = inspiration.id) { isDragging ->
                                    val elevation = if (isDragging) 8.dp else 2.dp
                                    InspirationCard(
                                        inspiration = inspiration,
                                        searchQuery = searchQuery,
                                        onClick = {
                                            if (selectionMode) {
                                                if (selectedItems.contains(inspiration.id)) {
                                                    selectedItems.removeAll { it == inspiration.id }
                                                } else {
                                                    selectedItems.add(inspiration.id)
                                                }
                                                if (selectedItems.isEmpty()) selectionMode = false
                                            } else {
                                                onNavigateToEdit(inspiration.id)
                                            }
                                        },
                                        onMultiSelect = {
                                            selectionMode = true
                                            if (!selectedItems.contains(inspiration.id)) {
                                                selectedItems.add(inspiration.id)
                                            }
                                        },
                                        onTogglePin = { viewModel.togglePin(inspiration) },
                                        onToggleArchive = { viewModel.toggleArchive(inspiration) },
                                        onDelete = { viewModel.deleteInspiration(inspiration) },
                                        onGroupTo = { singleNoteForGroupAction = inspiration },
                                        onTagTo = { singleNoteForTagAction = inspiration },
                                        modifier = Modifier.longPressDraggableHandle(),
                                        elevation = elevation,
                                        selectionMode = selectionMode,
                                        isSelected = isSelected,
                                        brandColor = brandColor,
                                        onToggleVisibility = { viewModel.toggleContentVisibility(inspiration) }
                                    )
                                }
                            }
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                            viewModel.reorderInspirations(from.index, to.index, inspirations)
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(inspirations, key = { it.id }) { inspiration ->
                                val isSelected = selectedItems.contains(inspiration.id)
                                ReorderableItem(reorderableState, key = inspiration.id) { isDragging ->
                                    val elevation = if (isDragging) 8.dp else 4.dp
                                    InspirationCard(
                                        inspiration = inspiration,
                                        searchQuery = searchQuery,
                                        onClick = {
                                            if (selectionMode) {
                                                if (selectedItems.contains(inspiration.id)) {
                                                    selectedItems.removeAll { it == inspiration.id }
                                                } else {
                                                    selectedItems.add(inspiration.id)
                                                }
                                                if (selectedItems.isEmpty()) selectionMode = false
                                            } else {
                                                onNavigateToEdit(inspiration.id)
                                            }
                                        },
                                        onMultiSelect = {
                                            selectionMode = true
                                            if (!selectedItems.contains(inspiration.id)) {
                                                selectedItems.add(inspiration.id)
                                            }
                                        },
                                        onTogglePin = { viewModel.togglePin(inspiration) },
                                        onToggleArchive = { viewModel.toggleArchive(inspiration) },
                                        onDelete = { viewModel.deleteInspiration(inspiration) },
                                        onGroupTo = { singleNoteForGroupAction = inspiration },
                                        onTagTo = { singleNoteForTagAction = inspiration },
                                        modifier = Modifier.longPressDraggableHandle(),
                                        elevation = elevation,
                                        selectionMode = selectionMode,
                                        isSelected = isSelected,
                                        brandColor = brandColor,
                                        onToggleVisibility = { viewModel.toggleContentVisibility(inspiration) }
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // Floating Selection Toolbar
                AnimatedVisibility(
                    visible = selectionMode && selectedItems.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it + 100 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it + 100 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "(${selectedItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = brandColor,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 导出为Markdown
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selectedItems.isNotEmpty()) {
                                                val selectedInspirations = inspirations.filter { it.id in selectedItems }
                                                exportSelectedNotes(context, selectedInspirations)
                                            }
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SaveAlt,
                                        contentDescription = "导出",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("导出", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }

                                // 合并预览
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selectedItems.isNotEmpty()) {
                                                onNavigateToMergePreview(selectedItems.joinToString(","))
                                            }
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChromeReaderMode,
                                        contentDescription = "拼文",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("拼文", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }

                                // 接合文档
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selectedItems.isNotEmpty()) {
                                                notesToJoint = inspirations.filter { it.id in selectedItems }
                                                showJointDialog = true
                                            }
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "接合",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("接合", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }

                                // Batch Tag
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showBatchTagDialog = true }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = "标签",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("标签", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                // Batch Group
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showBatchGroupDialog = true }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "分组",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("分组", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                // Batch Archive
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val selectedInspirations = inspirations.filter { it.id in selectedItems }
                                            viewModel.archiveInspirations(selectedInspirations, !showArchived)
                                            selectionMode = false
                                            selectedItems.clear()
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = if (showArchived) "解档" else "归档",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(if (showArchived) "解档" else "归档", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                                }
                                
                                // Batch Delete
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val selectedInspirations = inspirations.filter { it.id in selectedItems }
                                            viewModel.deleteInspirations(selectedInspirations)
                                            selectionMode = false
                                            selectedItems.clear()
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("删除", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        // 透明抓取层：在气泡展开时，点击屏幕任意空白处可自动收回气泡
        if (isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        isFabMenuExpanded = false
                    }
            )
        }

        // 展开的悬浮快捷菜单 (竖排气泡框样式)
        AnimatedVisibility(
            visible = isFabMenuExpanded,
            enter = scaleIn(
                initialScale = 0.2f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = scaleOut(
                targetScale = 0.2f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f),
                animationSpec = androidx.compose.animation.core.tween(180)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 98.dp, end = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(210.dp)
                    .shadow(elevation = 12.dp, shape = verticalBubbleShape, clip = true, spotColor = brandColor.copy(alpha = 0.25f))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.88f),
                                Color.White.copy(alpha = 0.75f)
                            )
                        ),
                        shape = verticalBubbleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.5f),
                                brandColor.copy(alpha = 0.25f)
                            )
                        ),
                        shape = verticalBubbleShape
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. 新建分组 ("组")
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFF5AB693))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFF8FD1B5), Color(0xFF5AB693))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                isFabMenuExpanded = false
                                showNewGroupDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("组", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 2. Markdown ("M")
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFF618DE6))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFF9FBAF1), Color(0xFF618DE6))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                isFabMenuExpanded = false
                                onNavigateToEdit(0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 3. 拼文 ("拼")
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFFF09A59))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF3C09A), Color(0xFFF09A59))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                isFabMenuExpanded = false
                                onNavigateToMergePreview("0")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("拼", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        } // Close Box wrapping Scaffold
    }

fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color = Color(0xFFFFE082),
    highlightTextColor: Color = Color(0xFF1D1D1F)
): AnnotatedString {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var start = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            if (index > start) {
                append(text.substring(start, index))
            }
            val matchEnd = index + query.length
            pushStyle(
                SpanStyle(
                    background = highlightColor,
                    color = highlightTextColor,
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(index, matchEnd))
            pop()
            start = matchEnd
        }
    }
}

@Composable
fun InspirationCard(
    inspiration: Inspiration,
    categoryColorHex: String? = null,
    searchQuery: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMultiSelect: () -> Unit = {},
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onGroupTo: () -> Unit = {},
    onTagTo: () -> Unit = {},
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 4.dp,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    brandColor: Color = Color(0xFF1B7679),
    onToggleVisibility: () -> Unit = {}
) {
    val formattedDate = remember(inspiration.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(inspiration.timestamp))
    }

    val cardTags = remember(inspiration.tag) {
        inspiration.tag.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    val highlightedTitle = remember(inspiration.title, searchQuery, brandColor) {
        buildHighlightedText(
            text = inspiration.title,
            query = searchQuery,
            highlightColor = brandColor.copy(alpha = 0.28f),
            highlightTextColor = brandColor
        )
    }

    val highlightedContent = remember(inspiration.content, searchQuery, brandColor) {
        buildHighlightedText(
            text = inspiration.content,
            query = searchQuery,
            highlightColor = brandColor.copy(alpha = 0.28f),
            highlightTextColor = brandColor
        )
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 1.dp else elevation,
        animationSpec = tween(durationMillis = 250),
        label = "elevation"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) brandColor.copy(alpha = 0.5f) else Color(0xFFE5E5EA),
        animationSpec = tween(durationMillis = 250),
        label = "borderColor"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF0F6F3) else Color.White,
        animationSpec = tween(durationMillis = 250),
        label = "bgColor"
    )

    val stripeColor = remember(inspiration.id) {
        com.example.ui.theme.getNoteStripeColor(inspiration.id)
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    @OptIn(ExperimentalFoundationApi::class)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { currentOnClick() }
            .scale(animatedScale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedBgColor
        ),
        border = BorderStroke(0.5.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left stripe indicator for group color
            val hasContrastIssue = stripeColor == Color(0xFFFDF8E2) || stripeColor == Color(0xFFFDFDF5)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
                    .then(
                        if (hasContrastIssue) {
                            Modifier.border(width = 0.5.dp, color = Color(0xFFC7BEAF))
                        } else {
                            Modifier
                        }
                    )
            )

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = highlightedTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF333333)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (inspiration.isContentVisible) {
                        Text(
                            text = highlightedContent,
                            fontSize = 14.sp,
                            color = Color(0xFF6B685C),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "正文已被隐藏",
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            cardTags.forEach { singleTag ->
                                Surface(
                                    color = Color(0xFFE5E2D9),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "#$singleTag",
                                        fontSize = 10.sp,
                                        color = Color(0xFF555555),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            if (inspiration.category.isNotEmpty()) {
                                val badgeBgColor = if (categoryColorHex != null) parseHexColor(categoryColorHex).copy(alpha = 0.2f) else Color(0xFFD0E0D0)
                                val badgeTextColor = if (categoryColorHex != null) parseHexColor(categoryColorHex) else Color(0xFF333333)
                                Surface(
                                    color = badgeBgColor,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = inspiration.category,
                                        fontSize = 10.sp,
                                        color = badgeTextColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = { onTogglePin() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "置顶",
                            tint = if (inspiration.isPinned) Color(0xFFD66A44) else Color(0xFF999999),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleVisibility() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (inspiration.isContentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "切换内容可见性",
                            tint = if (inspiration.isContentVisible) brandColor else Color(0xFF999999),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 竖排三个点 (MoreVert) 挪到这里，与小眼睛、置顶垂直对齐
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多选项",
                                tint = Color(0xFF6B685C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (inspiration.isPinned) "取消置顶" else "置顶", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                onClick = {
                                    onTogglePin()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("多选", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                onClick = {
                                    showMenu = false
                                    onMultiSelect()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (inspiration.isArchived) "取消归档" else "归档", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                onClick = {
                                    onToggleArchive()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                }
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            DropdownMenuItem(
                                text = { Text("分组到...", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    onGroupTo()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("标签到...", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    onTagTo()
                                    showMenu = false
                                }
                            )
                        }
                    }

                    if (selectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerTreeGroupItem(
    name: String,
    colorHex: String,
    noteCount: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onMergePreviewClick: (() -> Unit)? = null,
    onJointClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onToggleExpand() },
                modifier = Modifier.size(20.dp)
            ) {
                if (noteCount > 0) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = parseHexColor(colorHex),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (noteCount > 0) {
                Text(
                    text = noteCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (onMergePreviewClick != null) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("合并预览") },
                                onClick = {
                                    showMenu = false
                                    onMergePreviewClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ChromeReaderMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            if (onJointClick != null) {
                                DropdownMenuItem(
                                    text = { Text("接合文档") },
                                    onClick = {
                                        showMenu = false
                                        onJointClick()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        

    }
}

@Composable
fun DrawerTreeNoteItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(start = 44.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (title.isNotBlank()) title else "无标题笔记",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun exportSelectedNotes(context: Context, notes: List<Inspiration>) {
    if (notes.isEmpty()) return
    try {
        val mergedContent = buildString {
            notes.forEachIndexed { index, note ->
                append("# ${note.title.ifBlank { "无标题" }}\n\n")
                if (note.category.isNotBlank()) {
                    append("> 分类: ${note.category}\n")
                }
                if (note.tag.isNotBlank()) {
                    val tags = note.tag.split(",").joinToString(" ") { "#$it" }
                    append("> 标签: $tags\n")
                }
                append("\n")
                append(note.content)
                append("\n\n")
                if (index < notes.size - 1) {
                    append("---\n\n")
                }
            }
        }
        

        
        val fileName = if (notes.size == 1) {
            val cleanTitle = notes.first().title.trim()
            val safeTitle = cleanTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            if (safeTitle.isBlank()) "untitled.md" else "${safeTitle}.md"
        } else {
            "merged_inspirations_${System.currentTimeMillis() / 1000}.md"
        }
        
        val cacheFile = File(context.cacheDir, fileName)
        cacheFile.writeText(mergedContent)
        
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "导出 Markdown 文件")
            putExtra(Intent.EXTRA_TEXT, mergedContent)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "导出为 Markdown 文件")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
