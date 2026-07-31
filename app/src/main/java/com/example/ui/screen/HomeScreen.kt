package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material3.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.example.domain.model.GroupInfo
import com.example.domain.model.Inspiration
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.ui.component.YouJiLogo
import com.example.ui.viewmodel.InspirationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

enum class RightDrawerType { THEME, ABOUT, QUICK_PHRASE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: InspirationViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToMergePreview: (String) -> Unit
) {
    val context = LocalContext.current
    val allInspirations by viewModel.allInspirations.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val currentThemeColorVal by viewModel.themeColor.collectAsState()
    val brandColor = remember(currentThemeColorVal) { Color(currentThemeColorVal) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 过滤出未归档的笔记
    val activeNotes = remember(allInspirations) {
        allInspirations.filter { !it.isArchived }
    }

    // 最近修改的5篇灵感笔记
    val recentNotes = remember(activeNotes) {
        activeNotes.sortedByDescending { it.modifiedTimestamp }.take(5)
    }

    val scope = rememberCoroutineScope()
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showRenameGroupDialog by remember { mutableStateOf(false) }
    var renameGroupOldName by remember { mutableStateOf("") }
    var renameGroupNewInput by remember { mutableStateOf("") }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var deleteGroupTargetName by remember { mutableStateOf("") }
    var activeGroupMenuName by remember { mutableStateOf<String?>(null) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Int>() }
    var showJointDialog by remember { mutableStateOf(false) }
    var notesToJoint by remember { mutableStateOf<List<Inspiration>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val density = LocalDensity.current
    val foldingFanShape = remember(density) {
        val cRPx = with(density) { 12.dp.toPx() }
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val xc = w / 2f
            val yc = h + h * 0.7f

            val rOuter = h * 1.55f
            val rInner = h * 0.75f
            val cR = cRPx

            val deltaO = (cR / rOuter) * (180f / Math.PI.toFloat())
            val deltaI = (cR / rInner) * (180f / Math.PI.toFloat())

            val halfAngleDeg = 44f
            val startAngle = 270f - halfAngleDeg
            val sweepAngle = halfAngleDeg * 2f

            val outerStartRad = Math.toRadians((startAngle + deltaO).toDouble())
            val outerEndRad = Math.toRadians((startAngle + sweepAngle - deltaO).toDouble())

            val innerStartRad = Math.toRadians((startAngle + deltaI).toDouble())
            val innerEndRad = Math.toRadians((startAngle + sweepAngle - deltaI).toDouble())

            // Corner 1: Left Top Corner (Outer Start)
            val origOuterStartX = xc + rOuter * kotlin.math.cos(Math.toRadians(startAngle.toDouble())).toFloat()
            val origOuterStartY = yc + rOuter * kotlin.math.sin(Math.toRadians(startAngle.toDouble())).toFloat()

            val origInnerStartX = xc + rInner * kotlin.math.cos(Math.toRadians(startAngle.toDouble())).toFloat()
            val origInnerStartY = yc + rInner * kotlin.math.sin(Math.toRadians(startAngle.toDouble())).toFloat()

            // Direction vector along left radial edge
            val leftDx = origOuterStartX - origInnerStartX
            val leftDy = origOuterStartY - origInnerStartY
            val leftLen = kotlin.math.hypot(leftDx, leftDy)
            val leftUx = if (leftLen > 0) leftDx / leftLen else 0f
            val leftUy = if (leftLen > 0) leftDy / leftLen else 0f

            // Direction vector along right radial edge
            val origOuterEndX = xc + rOuter * kotlin.math.cos(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()
            val origOuterEndY = yc + rOuter * kotlin.math.sin(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()

            val origInnerEndX = xc + rInner * kotlin.math.cos(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()
            val origInnerEndY = yc + rInner * kotlin.math.sin(Math.toRadians((startAngle + sweepAngle).toDouble())).toFloat()

            val rightDx = origInnerEndX - origOuterEndX
            val rightDy = origInnerEndY - origOuterEndY
            val rightLen = kotlin.math.hypot(rightDx, rightDy)
            val rightUx = if (rightLen > 0) rightDx / rightLen else 0f
            val rightUy = if (rightLen > 0) rightDy / rightLen else 0f

            // Start path from point on left radial edge before outer corner
            val ptLeftOuterIn = androidx.compose.ui.geometry.Offset(origOuterStartX - leftUx * cR, origOuterStartY - leftUy * cR)
            moveTo(ptLeftOuterIn.x, ptLeftOuterIn.y)

            // Corner 1 quadTo into outer arc
            val outerArcStartX = xc + rOuter * kotlin.math.cos(outerStartRad).toFloat()
            val outerArcStartY = yc + rOuter * kotlin.math.sin(outerStartRad).toFloat()
            quadraticBezierTo(origOuterStartX, origOuterStartY, outerArcStartX, outerArcStartY)

            // Outer Arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(xc - rOuter, yc - rOuter, xc + rOuter, yc + rOuter),
                startAngleDegrees = startAngle + deltaO,
                sweepAngleDegrees = sweepAngle - 2f * deltaO,
                forceMoveTo = false
            )

            // Corner 2 (Top Right Outer): quadTo onto right radial edge
            val ptRightOuterIn = androidx.compose.ui.geometry.Offset(origOuterEndX + rightUx * cR, origOuterEndY + rightUy * cR)
            quadraticBezierTo(origOuterEndX, origOuterEndY, ptRightOuterIn.x, ptRightOuterIn.y)

            // Right Radial Edge to Inner Corner
            val ptRightInnerIn = androidx.compose.ui.geometry.Offset(origInnerEndX - rightUx * cR, origInnerEndY - rightUy * cR)
            lineTo(ptRightInnerIn.x, ptRightInnerIn.y)

            // Corner 3 (Bottom Right Inner): quadTo into inner arc
            val innerArcEndX = xc + rInner * kotlin.math.cos(innerEndRad).toFloat()
            val innerArcEndY = yc + rInner * kotlin.math.sin(innerEndRad).toFloat()
            quadraticBezierTo(origInnerEndX, origInnerEndY, innerArcEndX, innerArcEndY)

            // Inner Arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(xc - rInner, yc - rInner, xc + rInner, yc + rInner),
                startAngleDegrees = startAngle + sweepAngle - deltaI,
                sweepAngleDegrees = -(sweepAngle - 2f * deltaI),
                forceMoveTo = false
            )

            // Corner 4 (Bottom Left Inner): quadTo onto left radial edge
            val ptLeftInnerIn = androidx.compose.ui.geometry.Offset(origInnerStartX + leftUx * cR, origInnerStartY + leftUy * cR)
            quadraticBezierTo(origInnerStartX, origInnerStartY, ptLeftInnerIn.x, ptLeftInnerIn.y)

            close()
        }
    }

    // 新建分组弹窗
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

    var currentTab by remember { mutableStateOf(0) } // 0: 灵感库, 1: 我的
    var rightDrawerType by remember { mutableStateOf<RightDrawerType?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFBFBFD),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = brandColor,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "由记",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "YouJi",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            
                            val todayStr = remember {
                                val sdf = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)
                                sdf.format(Date())
                            }
                            Text(
                                text = todayStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!selectionMode) {
                    val density = LocalDensity.current
                val cutoutShape = remember(density) {
                    val rTop = with(density) { 28.dp.toPx() }
                    val buttonR = with(density) { 28.dp.toPx() } // 56dp button -> 28dp radius
                    val gap = with(density) { 6.dp.toPx() }      // 6dp gap
                    val notchR = buttonR + gap                   // 34dp notch radius
                    val yCenter = with(density) { 0.dp.toPx() }  // center at top edge
                    val filletR = with(density) { 8.dp.toPx() }  // smooth rounded corners

                    androidx.compose.foundation.shape.GenericShape { size, _ ->
                        val w = size.width
                        val h = size.height
                        val x0 = w / 2f

                        moveTo(0f, rTop)
                        quadraticTo(0f, 0f, rTop, 0f)

                        lineTo(x0 - notchR - filletR, 0f)
                        quadraticTo(x0 - notchR, 0f, x0 - notchR, filletR)

                        // Concave notch arc dipping down into the bar
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = x0 - notchR,
                                top = yCenter - notchR,
                                right = x0 + notchR,
                                bottom = yCenter + notchR
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )

                        quadraticTo(x0 + notchR, 0f, x0 + notchR + filletR, 0f)
                        lineTo(w - rTop, 0f)
                        quadraticTo(w, 0f, w, rTop)

                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(90.dp)
                ) {
                    // 1. Bottom Bar Surface
                    AnimatedVisibility(
                        visible = !isFabMenuExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = cutoutShape,
                                    clip = false,
                                    spotColor = Color.Black.copy(alpha = 0.08f),
                                    ambientColor = Color.Black.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = Color(0xFFE5E5EA),
                                    shape = cutoutShape
                                ),
                            shape = cutoutShape,
                            color = Color.White.copy(alpha = 0.95f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Tab: Home / Inspiration Library
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            currentTab = 0
                                            isFabMenuExpanded = false
                                            if (selectionMode) {
                                                selectedItems.clear()
                                                selectionMode = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "主页",
                                            tint = if (currentTab == 0) brandColor else Color(0xFF8E8E93),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "灵感库",
                                            fontSize = 10.sp,
                                            color = if (currentTab == 0) brandColor else Color(0xFF8E8E93),
                                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }

                                // Spacer for Protruding FAB
                                Spacer(modifier = Modifier.width(80.dp))

                                // Right Tab: Me
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            currentTab = 1
                                            isFabMenuExpanded = false
                                            selectionMode = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "我的",
                                            tint = if (currentTab == 1) brandColor else Color(0xFF8E8E93),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "我的",
                                            fontSize = 10.sp,
                                            color = if (currentTab == 1) brandColor else Color(0xFF8E8E93),
                                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Protruding Round Floating Ball
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 10.dp)
                            .size(56.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                clip = false,
                                spotColor = brandColor.copy(alpha = 0.35f)
                            )
                            .background(if (isFabMenuExpanded) MaterialTheme.colorScheme.errorContainer else brandColor, CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable {
                                isFabMenuExpanded = !isFabMenuExpanded
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "菜单",
                            tint = if (isFabMenuExpanded) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFFBFBFD))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

            // 1. 顶部：最近灵感画廊 (Recent Inspirations Gallery)
            if (recentNotes.isNotEmpty()) {
                val displayRecentNotes = remember(recentNotes) { recentNotes.take(5) }
                val pagerState = rememberPagerState(pageCount = { displayRecentNotes.size })
                
                Text(
                    text = "最近灵感",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val note = displayRecentNotes[page]
                        val stripeColor = remember(note.id) {
                            com.example.ui.theme.getNoteStripeColor(note.id)
                        }

                        val cardBgColor = remember(note.id) {
                            val hex = if (note.category.isNotBlank()) {
                                allGroups.find { it.name == note.category }?.colorHex ?: "#E5E2D9"
                            } else {
                                "#E5E2D9"
                            }
                            parseHexColor(hex).copy(alpha = 0.2f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    onNavigateToEdit(note.id)
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(stripeColor)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (note.title.isNotBlank()) note.title else "无标题灵感",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D1D1F),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (note.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Text(
                                            text = note.content,
                                            fontSize = 12.sp,
                                            color = Color(0xFF6E6E73),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val dateStr = remember(note.modifiedTimestamp) {
                                            val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
                                            sdf.format(Date(note.modifiedTimestamp))
                                        }
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = Color(0xFF8E8E93)
                                        )

                                        if (note.tag.isNotBlank()) {
                                            val firstTag = note.tag.split(",").firstOrNull()?.trim() ?: ""
                                            if (firstTag.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "#$firstTag",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (displayRecentNotes.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 14.dp, end = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(displayRecentNotes.size) { index ->
                                val active = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 8.dp else 5.dp)
                                        .background(
                                            color = if (active) brandColor else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // 2. 下方上下布局 (Dashboard Column)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 上方：灵感库大卡片 (配色与下面一致，单色不带渐变)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                viewModel.setShowArchived(false)
                                viewModel.setSelectedGroup("全部笔记")
                                viewModel.setSelectedTag("")
                                onNavigateToList()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "灵感库",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1D1F),
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "汇聚随时闪现的创作与记录火花",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6E6E73)
                                )
                            }

                            // 简练现代的圆形计数展示
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFF7F7F9), CircleShape)
                                    .border(0.5.dp, Color(0xFFE5E5EA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${activeNotes.size}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1D1F)
                                    )
                                    Text(
                                        text = "总数",
                                        fontSize = 10.sp,
                                        color = Color(0xFF8E8E93),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 下方：原先的快捷操作跟分类计数
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 全部笔记快捷
                    val isAllExpanded = expandedCategory == "all"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategory = if (isAllExpanded) null else "all"
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, if (isAllExpanded) brandColor else Color(0xFFE5E5EA)),
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Notes, 
                                        contentDescription = null, 
                                        tint = if (isAllExpanded) brandColor else Color(0xFF48484A), 
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "全部笔记", 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (isAllExpanded) brandColor else Color(0xFF1D1D1F)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${activeNotes.size}", 
                                        fontSize = 12.sp, 
                                        color = Color(0xFF6E6E73), 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Icon(
                                        imageVector = if (isAllExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "展开收起",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isAllExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (activeNotes.isEmpty()) {
                                    Text(
                                        "暂无笔记",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888888),
                                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                } else {
                                    activeNotes.forEach { note ->
                                        val isSelected = selectedItems.contains(note.id)
                                        CompactInspirationItem(
                                            note = note,
                                            onClick = {
                                                if (selectionMode) {
                                                    if (selectedItems.contains(note.id)) {
                                                        selectedItems.removeAll { it == note.id }
                                                    } else {
                                                        selectedItems.add(note.id)
                                                    }
                                                    if (selectedItems.isEmpty()) selectionMode = false
                                                } else {
                                                    onNavigateToEdit(note.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!selectionMode) {
                                                    selectionMode = true
                                                    selectedItems.add(note.id)
                                                }
                                            },
                                            selectionMode = selectionMode,
                                            isSelected = isSelected,
                                            brandColor = brandColor,
                                            onToggleVisibility = { viewModel.toggleContentVisibility(note) },
                                            onMergePreview = {
                                                if (selectionMode) {
                                                    onNavigateToMergePreview(selectedItems.joinToString(","))
                                                } else {
                                                    onNavigateToMergePreview(note.id.toString())
                                                }
                                            },
                                            onJointDocs = {
                                                if (selectionMode) {
                                                    notesToJoint = allInspirations.filter { it.id in selectedItems }
                                                } else {
                                                    notesToJoint = listOf(note)
                                                }
                                                showJointDialog = true
                                            },
                                            onRestore = { viewModel.toggleArchive(note) },
                                            onDelete = { viewModel.deleteInspiration(note) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 未分类快捷
                    val uncategorizedNotes = remember(activeNotes, allGroups) {
                        activeNotes.filter { note ->
                            note.category.isBlank() || allGroups.none { it.name == note.category }
                        }
                    }
                    val isUncategorizedExpanded = expandedCategory == "uncategorized"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategory = if (isUncategorizedExpanded) null else "uncategorized"
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isUncategorizedExpanded) brandColor else Color(0xFFE2E2E2))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FolderOpen, 
                                        contentDescription = null, 
                                        tint = if (isUncategorizedExpanded) brandColor else Color(0xFF777777), 
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "未分类", 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (isUncategorizedExpanded) brandColor else Color(0xFF444444)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${uncategorizedNotes.size}", 
                                        fontSize = 12.sp, 
                                        color = Color(0xFF666666), 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Icon(
                                        imageVector = if (isUncategorizedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "展开收起",
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isUncategorizedExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uncategorizedNotes.isEmpty()) {
                                    Text(
                                        "暂无未分类笔记",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888888),
                                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                } else {
                                    uncategorizedNotes.forEach { note ->
                                        val isSelected = selectedItems.contains(note.id)
                                        CompactInspirationItem(
                                            note = note,
                                            onClick = {
                                                if (selectionMode) {
                                                    if (selectedItems.contains(note.id)) {
                                                        selectedItems.removeAll { it == note.id }
                                                    } else {
                                                        selectedItems.add(note.id)
                                                    }
                                                    if (selectedItems.isEmpty()) selectionMode = false
                                                } else {
                                                    onNavigateToEdit(note.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!selectionMode) {
                                                    selectionMode = true
                                                    selectedItems.add(note.id)
                                                }
                                            },
                                            selectionMode = selectionMode,
                                            isSelected = isSelected,
                                            brandColor = brandColor,
                                            onToggleVisibility = { viewModel.toggleContentVisibility(note) },
                                            onMergePreview = {
                                                if (selectionMode) {
                                                    onNavigateToMergePreview(selectedItems.joinToString(","))
                                                } else {
                                                    onNavigateToMergePreview(note.id.toString())
                                                }
                                            },
                                            onJointDocs = {
                                                if (selectionMode) {
                                                    notesToJoint = allInspirations.filter { it.id in selectedItems }
                                                } else {
                                                    notesToJoint = listOf(note)
                                                }
                                                showJointDialog = true
                                            },
                                            onRestore = { viewModel.toggleArchive(note) },
                                            onDelete = { viewModel.deleteInspiration(note) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 归档箱快捷
                    val archivedNotes = remember(allInspirations) {
                        allInspirations.filter { it.isArchived }
                    }
                    val isArchivedExpanded = expandedCategory == "archived"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategory = if (isArchivedExpanded) null else "archived"
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isArchivedExpanded) brandColor else Color(0xFFE2E2E2))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Archive, 
                                        contentDescription = null, 
                                        tint = if (isArchivedExpanded) brandColor else Color(0xFF777777), 
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "归档箱", 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (isArchivedExpanded) brandColor else Color(0xFF444444)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${archivedNotes.size}", 
                                        fontSize = 12.sp, 
                                        color = Color(0xFF666666), 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Icon(
                                        imageVector = if (isArchivedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "展开收起",
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isArchivedExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (archivedNotes.isEmpty()) {
                                    Text(
                                        "暂无归档笔记",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888888),
                                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                } else {
                                    archivedNotes.forEach { note ->
                                        val isSelected = selectedItems.contains(note.id)
                                        CompactInspirationItem(
                                            note = note,
                                            onClick = {
                                                if (selectionMode) {
                                                    if (selectedItems.contains(note.id)) {
                                                        selectedItems.removeAll { it == note.id }
                                                    } else {
                                                        selectedItems.add(note.id)
                                                    }
                                                    if (selectedItems.isEmpty()) selectionMode = false
                                                } else {
                                                    onNavigateToEdit(note.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!selectionMode) {
                                                    selectionMode = true
                                                    selectedItems.add(note.id)
                                                }
                                            },
                                            selectionMode = selectionMode,
                                            isSelected = isSelected,
                                            brandColor = brandColor,
                                            onToggleVisibility = { viewModel.toggleContentVisibility(note) },
                                            onMergePreview = {
                                                if (selectionMode) {
                                                    onNavigateToMergePreview(selectedItems.joinToString(","))
                                                } else {
                                                    onNavigateToMergePreview(note.id.toString())
                                                }
                                            },
                                            onJointDocs = {
                                                if (selectionMode) {
                                                    notesToJoint = allInspirations.filter { it.id in selectedItems }
                                                } else {
                                                    notesToJoint = listOf(note)
                                                }
                                                showJointDialog = true
                                            },
                                            onRestore = { viewModel.toggleArchive(note) },
                                            onDelete = { viewModel.deleteInspiration(note) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 分组与计数标题
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("分类分组", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
                    }

                    // 分组滚动列表
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allGroups.take(6).forEach { group ->
                            val groupNotes = remember(activeNotes, group.name) {
                                activeNotes.filter { it.category == group.name }
                            }
                            val isGroupExpanded = expandedCategory == "group:${group.name}"
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategory = if (isGroupExpanded) null else "group:${group.name}"
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (isGroupExpanded) brandColor else Color(0xFFE2E2E2))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(parseHexColor(group.colorHex), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = group.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF444444),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box {
                                                IconButton(
                                                    onClick = { activeGroupMenuName = group.name },
                                                    modifier = Modifier.size(24.dp).padding(end = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "分组菜单",
                                                        tint = Color(0xFF888888),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = activeGroupMenuName == group.name,
                                                    onDismissRequest = { activeGroupMenuName = null }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("重命名", fontSize = 13.sp) },
                                                        onClick = {
                                                            activeGroupMenuName = null
                                                            renameGroupOldName = group.name
                                                            renameGroupNewInput = group.name
                                                            showRenameGroupDialog = true
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("删除", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            activeGroupMenuName = null
                                                            deleteGroupTargetName = group.name
                                                            showDeleteGroupDialog = true
                                                        }
                                                    )
                                                }
                                            }
                                            Text(
                                                "${groupNotes.size}", 
                                                fontSize = 11.sp, 
                                                color = Color(0xFF666666),
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Icon(
                                                imageVector = if (isGroupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "展开收起",
                                                tint = Color(0xFF666666),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isGroupExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (groupNotes.isEmpty()) {
                                            Text(
                                                "暂无该分组笔记",
                                                fontSize = 12.sp,
                                                color = Color(0xFF888888),
                                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                            )
                                        } else {
                                            groupNotes.forEach { note ->
                                                val isSelected = selectedItems.contains(note.id)
                                                CompactInspirationItem(
                                                    note = note,
                                                    onClick = {
                                                        if (selectionMode) {
                                                            if (selectedItems.contains(note.id)) {
                                                                selectedItems.removeAll { it == note.id }
                                                            } else {
                                                                selectedItems.add(note.id)
                                                            }
                                                            if (selectedItems.isEmpty()) selectionMode = false
                                                        } else {
                                                            onNavigateToEdit(note.id)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!selectionMode) {
                                                            selectionMode = true
                                                            selectedItems.add(note.id)
                                                        }
                                                    },
                                                    selectionMode = selectionMode,
                                                    isSelected = isSelected,
                                                    brandColor = brandColor,
                                                    onToggleVisibility = { viewModel.toggleContentVisibility(note) },
                                                    onMergePreview = {
                                                        if (selectionMode) {
                                                            onNavigateToMergePreview(selectedItems.joinToString(","))
                                                        } else {
                                                            onNavigateToMergePreview(note.id.toString())
                                                        }
                                                    },
                                                    onJointDocs = {
                                                        if (selectionMode) {
                                                            notesToJoint = allInspirations.filter { it.id in selectedItems }
                                                        } else {
                                                            notesToJoint = listOf(note)
                                                        }
                                                        showJointDialog = true
                                                    },
                                                    onRestore = { viewModel.toggleArchive(note) },
                                                    onDelete = { viewModel.deleteInspiration(note) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 我的热门标签
                    if (allTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Label, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("热门标签", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTags.take(8).forEach { tag ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        viewModel.setShowArchived(false)
                                        viewModel.setSelectedTag(tag)
                                        onNavigateToList()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E2E2))
                                ) {
                                    Text(
                                        text = "#$tag",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = Color(0xFF555555),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    } else {
            Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF7F8FA))
                ) {
                    MyProfileScreen(
                        viewModel = viewModel,
                        brandColor = brandColor,
                        onOpenThemeDrawer = { rightDrawerType = RightDrawerType.THEME },
                        onOpenAboutDrawer = { rightDrawerType = RightDrawerType.ABOUT },
                        onOpenQuickPhraseDrawer = { rightDrawerType = RightDrawerType.QUICK_PHRASE }
                    )
                }
            }

            // 遮罩层，点击外部收起菜单
            AnimatedVisibility(
                visible = isFabMenuExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isFabMenuExpanded = false
                        }
                )
            }

            // 弹出悬浮扇形菜单
            AnimatedVisibility(
                visible = isFabMenuExpanded,
                enter = scaleIn(
                    initialScale = 0.5f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = scaleOut(
                    targetScale = 0.5f,
                    transformOrigin = TransformOrigin(0.5f, 1f)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 110.dp)
                        .shadow(elevation = 16.dp, shape = foldingFanShape, clip = true, spotColor = brandColor.copy(alpha = 0.25f))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.82f),
                                    Color.White.copy(alpha = 0.68f)
                                )
                            ),
                            shape = foldingFanShape
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.5f),
                                    brandColor.copy(alpha = 0.25f)
                                )
                            ),
                            shape = foldingFanShape
                        )
                ) {

                    // 1. 左侧按钮：新建分组，纯图标“组”
                    Box(
                        modifier = Modifier
                            .offset(x = 24.dp, y = 38.dp)
                            .size(52.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0xFF5AB693))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF8FD1B5), Color(0xFF5AB693))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                isFabMenuExpanded = false
                                showNewGroupDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "组",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. 中间按钮：markdown，纯图标"M"
                    Box(
                        modifier = Modifier
                            .offset(x = 84.dp, y = 22.dp)
                            .size(52.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0xFF618DE6))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF9FBAF1), Color(0xFF618DE6))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                isFabMenuExpanded = false
                                onNavigateToEdit(0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 3. 右侧按钮：拼文，纯图标“拼”
                    Box(
                        modifier = Modifier
                            .offset(x = 144.dp, y = 38.dp)
                            .size(52.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0xFFF09A59))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF3C09A), Color(0xFFF09A59))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                isFabMenuExpanded = false
                                onNavigateToMergePreview("0")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "拼",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        // Floating Selection Toolbar for HomeScreen
        AnimatedVisibility(
            visible = selectionMode && selectedItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it + 100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it + 100 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedItems.clear()
                            selectionMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已选择 ${selectedItems.size} 项",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row {
                        // 导出为Markdown
                        IconButton(onClick = {
                            if (selectedItems.isNotEmpty()) {
                                val selectedInspirations = allInspirations.filter { it.id in selectedItems }
                                exportSelectedNotes(context, selectedInspirations)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = "导出为Markdown",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 合并预览
                        IconButton(onClick = {
                            if (selectedItems.isNotEmpty()) {
                                onNavigateToMergePreview(selectedItems.joinToString(","))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ChromeReaderMode,
                                contentDescription = "合并预览",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 接合文档
                        IconButton(onClick = {
                            if (selectedItems.isNotEmpty()) {
                                notesToJoint = allInspirations.filter { it.id in selectedItems }
                                showJointDialog = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "接合文档",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
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
                            
                            // 归档原始笔记
                            viewModel.archiveInspirations(orderedNotes, true)
                            
                            showJointDialog = false
                            notesToJoint = emptyList()
                            selectionMode = false
                            selectedItems.clear()
                            
                            // 显示 Snackbar 并支持撤销
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "接合成功，原零散卡片已归档",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Long
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                viewModel.archiveInspirations(orderedNotes, false)
                                viewModel.deleteInspiration(
                                    com.example.domain.model.Inspiration(id = newId, title = "", content = "")
                                )
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
}

    // 右侧滑出抽屉 (System Settings Drawer)
    AnimatedVisibility(
        visible = rightDrawerType != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize().zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { rightDrawerType = null })
                }
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = false) {}, // 防止点击事件穿透到背景
                color = Color(0xFFFBFBF9),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp)
                ) {
                    // 顶部：标题和关闭按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (rightDrawerType) {
                                RightDrawerType.THEME -> "系统主题色"
                                RightDrawerType.ABOUT -> "关于由记"
                                RightDrawerType.QUICK_PHRASE -> "快捷词组设置"
                                null -> ""
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        IconButton(
                            onClick = { rightDrawerType = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF888888)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(24.dp))

                    if (rightDrawerType == RightDrawerType.THEME) {
                        // 主题色选择列表
                        val currentThemeColorVal by viewModel.themeColor.collectAsState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            com.example.ui.viewmodel.themeColors.forEach { option ->
                                val isSelected = option.colorValue == currentThemeColorVal
                                Surface(
                                    onClick = {
                                        viewModel.setThemeColor(option.colorValue)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(option.colorValue).copy(alpha = 0.08f) else Color.White,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color(option.colorValue) else Color(0xFFE0DFD5)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(Color(option.colorValue), CircleShape)
                                            )
                                            Text(
                                                text = option.name.replace("（默认）", ""),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(option.colorValue) else Color(0xFF333333)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选择",
                                                tint = Color(option.colorValue),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (rightDrawerType == RightDrawerType.ABOUT) {
                        // 关于内容展示
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brandColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "由记 YouJi",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandColor
                                    )
                                    Text(
                                        text = "随时随地，收集灵感",
                                        fontSize = 12.sp,
                                        color = brandColor.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Text(
                                text = "由记 是一款专业的离线优先灵感碎片收集和拼接整理工具。旨在帮助内容创作者、学者及日常记录者快速捕捉脑海中随时闪现的灵感火花。",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = Color(0xFF555555)
                            )

                            Text(
                                text = "核心功能包括：\n• 随手记录 Markdown 格式的灵感笔记\n• 直观的拖拽操作进行长文接合\n• 多维度文件夹分类与标签分组管理\n• 纯离线安全存储，保护您的数据隐私",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = Color(0xFF555555)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("版本", fontSize = 13.sp, color = Color(0xFF888888))
                                Text("v1.1.0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            }
                        }
                    } else if (rightDrawerType == RightDrawerType.QUICK_PHRASE) {
                        // 快捷词组侧边栏设置表单
                        val quickPhrases by viewModel.quickPhrases.collectAsState()
                        var showQuickPhraseDialog by remember { mutableStateOf(false) }
                        var editingPhrase by remember { mutableStateOf<com.example.domain.model.QuickPhrase?>(null) }
                        var phraseLabelInput by remember { mutableStateOf("") }
                        var phraseContentInput by remember { mutableStateOf("") }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brandColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "快捷词组与句子管理",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandColor
                                    )
                                    Text(
                                        text = "在编辑页面左侧可滚动调用的常用转折句与固定短语胶囊。",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (quickPhrases.isNotEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.restoreDefaultQuickPhrases() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("清空词组", fontSize = 12.sp, color = Color(0xFF8E8E93))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }
                                Button(
                                    onClick = {
                                        editingPhrase = null
                                        phraseLabelInput = ""
                                        phraseContentInput = ""
                                        showQuickPhraseDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("新增词组", fontSize = 12.sp)
                                }
                            }

                            if (quickPhrases.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无快捷词组，点击上方“新增词组”添加",
                                        fontSize = 13.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickPhrases.forEach { phrase ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    editingPhrase = phrase
                                                    phraseLabelInput = phrase.label
                                                    phraseContentInput = phrase.content
                                                    showQuickPhraseDialog = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = brandColor.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = phrase.label,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = brandColor,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = phrase.content.replace("\n", "↵"),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF3A3A3C),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteQuickPhrase(phrase.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "删除词组",
                                                        tint = Color(0xFF8E8E93),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showQuickPhraseDialog) {
                            AlertDialog(
                                onDismissRequest = { showQuickPhraseDialog = false },
                                title = {
                                    Text(
                                        text = if (editingPhrase == null) "新增快捷词组" else "编辑快捷词组",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = phraseLabelInput,
                                            onValueChange = { phraseLabelInput = it },
                                            label = { Text("胶囊标签 (如“转折”)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = phraseContentInput,
                                            onValueChange = { phraseContentInput = it },
                                            label = { Text("点击上屏的详细文本") },
                                            minLines = 2,
                                            maxLines = 4,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val label = phraseLabelInput.trim()
                                            val content = phraseContentInput
                                            if (label.isNotBlank() && content.isNotBlank()) {
                                                val target = editingPhrase
                                                if (target == null) {
                                                    viewModel.addQuickPhrase(label, content)
                                                } else {
                                                    viewModel.updateQuickPhrase(target.id, label, content)
                                                }
                                                showQuickPhraseDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                                    ) {
                                        Text("保存")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showQuickPhraseDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 右边缘往左滑动跳转手势区域 (仅在非选择模式，且没有滑出抽屉时生效)
        if (!selectionMode && rightDrawerType == null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        var dragAccumulator = 0f
                        detectDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (dragAccumulator < -80f) {
                                    onNavigateToList()
                                }
                            },
                            onDragCancel = { dragAccumulator = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount.x
                                if (dragAccumulator < -150f) {
                                    onNavigateToList()
                                    dragAccumulator = 0f
                                }
                            }
                        )
                    }
            )
        }
    }
}
}

data class HeatmapDay(
    val date: java.time.LocalDate,
    val dateStr: String,
    val chineseMonthDay: String,
    val count: Int
)

data class MonthLabelInfo(
    val monthName: String,
    val weekIndex: Int
)

@Composable
private fun ProfileStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF8E8E93)
        )
    }
}

@Composable
fun MyProfileScreen(
    viewModel: InspirationViewModel,
    brandColor: Color,
    onOpenThemeDrawer: () -> Unit,
    onOpenAboutDrawer: () -> Unit,
    onOpenQuickPhraseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val quickPhrases by viewModel.quickPhrases.collectAsState()

    // Export document launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { targetUri ->
            try {
                val jsonText = viewModel.exportAllDataJson()
                context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                    stream.write(jsonText.toByteArray(Charsets.UTF_8))
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("数据已成功导出保存为 JSON 文件！")
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("导出文件失败：${e.localizedMessage}")
                }
            }
        }
    }

    // Import document launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { targetUri ->
            try {
                val jsonText = context.contentResolver.openInputStream(targetUri)?.bufferedReader()?.use { it.readText() }
                if (!jsonText.isNullOrBlank()) {
                    coroutineScope.launch {
                        val success = viewModel.importDataJson(jsonText)
                        if (success) {
                            snackbarHostState.showSnackbar("数据恢复成功！已还原导出时的全部数据")
                            showImportDialog = false
                        } else {
                            snackbarHostState.showSnackbar("导入失败：JSON 内容解析错误")
                        }
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("读取导入文件失败：${e.localizedMessage}")
                }
            }
        }
    }

    val allInspirations by viewModel.allInspirations.collectAsState(initial = emptyList())
    val activeInspirations = remember(allInspirations) {
        allInspirations.filter { !it.isArchived }
    }

    // 统计数据
    val totalInspirations = activeInspirations.size
    val totalWords = remember(activeInspirations) { activeInspirations.sumOf { it.content.length } }
    val avgWords = if (totalInspirations > 0) totalWords / totalInspirations else 0
    val totalTags = remember(activeInspirations) {
        activeInspirations.flatMap { note ->
            note.tag.split(",", " ", "，", "#").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().size
    }
    val distinctDaysCount = remember(activeInspirations) {
        activeInspirations.map { note ->
            java.time.Instant.ofEpochMilli(note.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.distinct().size
    }

    // 热力图数据 (18周)
    var selectedHeatmapDay by remember { mutableStateOf<HeatmapDay?>(null) }

    val countMap = remember(activeInspirations) {
        val map = mutableMapOf<java.time.LocalDate, Int>()
        activeInspirations.forEach { note ->
            val d = java.time.Instant.ofEpochMilli(note.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            map[d] = (map[d] ?: 0) + 1
        }
        map
    }

    val heatmapWeeks = remember(countMap) {
        val today = java.time.LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val endDay = today.plusDays((7 - currentDayOfWeek).toLong())
        val startDay = endDay.minusWeeks(18).plusDays(1)

        val weeks = mutableListOf<List<HeatmapDay>>()
        var cur = startDay
        while (!cur.isAfter(endDay)) {
            val weekDays = mutableListOf<HeatmapDay>()
            for (i in 0..6) {
                val c = countMap[cur] ?: 0
                val dateStr = cur.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val zhStr = "${cur.monthValue}月${cur.dayOfMonth}日"
                weekDays.add(HeatmapDay(date = cur, dateStr = dateStr, chineseMonthDay = zhStr, count = c))
                cur = cur.plusDays(1)
            }
            weeks.add(weekDays)
        }
        weeks
    }

    // 热力图跨越的月份文字
    val monthLabels = remember(heatmapWeeks) {
        val labels = mutableListOf<String>()
        var lastMonth = -1
        val zhMonths = arrayOf("", "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
        heatmapWeeks.forEach { week ->
            val firstDayOfWeek = week.firstOrNull()?.date
            if (firstDayOfWeek != null && firstDayOfWeek.monthValue != lastMonth) {
                lastMonth = firstDayOfWeek.monthValue
                labels.add(zhMonths[lastMonth])
            }
        }
        labels
    }

    // 选中显示的年月文字 (如 2026年6月，默认当前年月)
    val displayMonthText = remember(selectedHeatmapDay) {
        val date = selectedHeatmapDay?.date ?: java.time.LocalDate.now()
        "${date.year}年${date.monthValue}月"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. 头像和昵称
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Color.White, CircleShape)
                    .border(0.5.dp, Color(0xFFE5E5EA), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.mipmap.ic_launcher_round),
                    contentDescription = "App Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "由记创作者",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "用文字拼接每一刻灵感",
                    fontSize = 12.sp,
                    color = Color(0xFF6E6E73)
                )
            }
        }

        // 2. 统计卡片 (5个概览数据项：灵感、字数、平均、标签、天数)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1) 灵感
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = "$totalInspirations",
                    label = "灵感",
                    valueColor = Color(0xFF3B82F6)
                )
                // 2) 字数
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = "$totalWords",
                    label = "字数",
                    valueColor = Color(0xFFF43F5E)
                )
                // 3) 平均
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = "$avgWords",
                    label = "平均",
                    valueColor = Color(0xFFF59E0B)
                )
                // 4) 标签
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = "$totalTags",
                    label = "标签",
                    valueColor = Color(0xFF8B5CF6)
                )
                // 5) 天数
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = "$distinctDaysCount",
                    label = "天数",
                    valueColor = Color(0xFFEF4444)
                )
            }
        }

        // 3. 年度极巧热力图（最新18周）
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF4F5F8),
            border = BorderStroke(0.5.dp, Color(0xFFE2E4E8)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // 标题与选定日期提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "年度极巧热力图 (最新18周)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1F)
                    )
                    Text(
                        text = displayMonthText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 热力图网格与底部月份标签
                val monthLabelInfos = remember(heatmapWeeks) {
                    val zhMonths = arrayOf("", "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
                    val rawCandidates = mutableListOf<MonthLabelInfo>()
                    
                    var lastMonth = -1
                    heatmapWeeks.forEachIndexed { weekIndex, week ->
                        val monthCounts = week.groupingBy { it.date.monthValue }.eachCount()
                        val dominantMonth = monthCounts.maxByOrNull { it.value }?.key ?: week.first().date.monthValue
                        
                        if (dominantMonth != lastMonth) {
                            lastMonth = dominantMonth
                            rawCandidates.add(MonthLabelInfo(zhMonths[dominantMonth], weekIndex))
                        }
                    }
                    
                    val labels = mutableListOf<MonthLabelInfo>()
                    var prevWeekIndex = -100
                    
                    rawCandidates.forEachIndexed { idx, candidate ->
                        val skipFirstTail = (idx == 0 && rawCandidates.size > 1 && (rawCandidates[1].weekIndex - candidate.weekIndex) < 3)
                        
                        if (!skipFirstTail) {
                            if (candidate.weekIndex - prevWeekIndex >= 3) {
                                labels.add(candidate)
                                prevWeekIndex = candidate.weekIndex
                            }
                        }
                    }
                    
                    labels
                }

                // 热力图横向/下拉滑动滚动网格与底部月份标签
                val heatmapScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(heatmapScrollState)
                ) {
                    val columnWidthDp = 18.dp
                    val columnSpacingDp = 4.dp
                    val totalContentWidthDp = (columnWidthDp + columnSpacingDp) * 18

                    Column(
                        modifier = Modifier.width(totalContentWidthDp)
                    ) {
                        // 热力图网格：18 列，7 行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(columnSpacingDp)
                        ) {
                            heatmapWeeks.forEach { week ->
                                Column(
                                    modifier = Modifier.width(columnWidthDp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    week.forEach { day ->
                                        val isSelected = selectedHeatmapDay?.date == day.date
                                        val blockBg = when {
                                            day.count == 0 -> Color(0xFFE1E5EA)
                                            day.count == 1 -> Color(0xFFFCA5A5)
                                            day.count == 2 -> Color(0xFFF87171)
                                            else -> Color(0xFFEF4444)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(blockBg)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(3.dp))
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable {
                                                    selectedHeatmapDay = if (isSelected) null else day
                                                }
                                        ) {
                                            if (isSelected) {
                                                Popup(
                                                    alignment = Alignment.TopCenter,
                                                    offset = IntOffset(0, -66),
                                                    onDismissRequest = { selectedHeatmapDay = null }
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(0.5.dp, Color(0xFFE2E4E8)),
                                                        shadowElevation = 3.dp
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "${day.count}篇",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFF43F5E)
                                                            )
                                                            Text(
                                                                text = day.chineseMonthDay,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF4B5563)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // 底部月份精准对齐文案展示 ("四月", "五月", "六月", "七月"...)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        ) {
                            monthLabelInfos.forEach { labelInfo ->
                                val xOffset = (columnWidthDp + columnSpacingDp) * labelInfo.weekIndex
                                Text(
                                    text = labelInfo.monthName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.offset(x = xOffset)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 功能选项列表
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "系统偏好设置",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E8E93),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            // 系统主题色卡片
            val currentThemeColorVal by viewModel.themeColor.collectAsState()
            val selectedOption = remember(currentThemeColorVal) {
                com.example.ui.viewmodel.themeColors.find { it.colorValue == currentThemeColorVal } ?: com.example.ui.viewmodel.themeColors[0]
            }

            Surface(
                onClick = onOpenThemeDrawer,
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(brandColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "系统主题色",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "个性化定制应用的主调色彩",
                                fontSize = 12.sp,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(selectedOption.colorValue), CircleShape)
                        )
                        Text(
                            text = selectedOption.name.replace("（默认）", ""),
                            fontSize = 13.sp,
                            color = Color(0xFF6E6E73),
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 自定义快捷词组与句子卡片（侧边栏收纳）
            Surface(
                onClick = onOpenQuickPhraseDrawer,
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(brandColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "快捷词组与上屏胶囊",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "配置编辑页侧边快速上屏的词句胶囊 (${quickPhrases.size}项)",
                                fontSize = 12.sp,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 导出数据 JSON 卡片
            Surface(
                onClick = {
                    exportJsonText = viewModel.exportAllDataJson()
                    showExportDialog = true
                },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(brandColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "导出数据 (JSON)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "导出全部笔记、分组与系统设置",
                                fontSize = 12.sp,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFC7C7CC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 导入数据 JSON 卡片
            Surface(
                onClick = {
                    importInputText = ""
                    showImportDialog = true
                },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(brandColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "导入数据 (JSON)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "选择文件或粘贴 JSON 恢复全部数据",
                                fontSize = 12.sp,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFC7C7CC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 关于由记卡片
            Surface(
                onClick = onOpenAboutDrawer,
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(brandColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "关于由记",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "了解由记的故事和核心功能",
                                fontSize = 12.sp,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v1.1.0",
                            fontSize = 13.sp,
                            color = Color(0xFF6E6E73),
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    }

    if (showExportDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }
        val fileName = "youji_backup_${dateFormat.format(Date())}.json"

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(text = "导出数据 (JSON)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "项目数据已打包完成。您可以直接复制 JSON 字符串，或点击导出为文件：",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        createDocumentLauncher.launch(fileName)
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Text("保存为 JSON 文件")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportJsonText))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已复制导出 JSON 到剪贴板")
                        }
                        showExportDialog = false
                    }
                ) {
                    Text("复制 JSON 文本")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(text = "导入数据恢复 (JSON)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "选择要导入的 JSON 备份文件，或在框中粘贴 JSON 字符串。导入将还原全部笔记、分组与设置：",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )

                    Button(
                        onClick = {
                            openDocumentLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor.copy(alpha = 0.12f), contentColor = brandColor)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择 JSON 文件导入")
                    }

                    Text(text = "或在下方直接粘贴 JSON 字符串：", fontSize = 12.sp, color = Color(0xFF6B7280))

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("在此粘贴 JSON 内容...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            coroutineScope.launch {
                                val success = viewModel.importDataJson(importInputText)
                                if (success) {
                                    snackbarHostState.showSnackbar("数据已成功恢复！")
                                    showImportDialog = false
                                } else {
                                    snackbarHostState.showSnackbar("恢复失败：JSON 内容解析错误")
                                }
                            }
                        }
                    },
                    enabled = importInputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Text("开始恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CompactInspirationItem(
    note: Inspiration,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    brandColor: Color = Color(0xFF1B7679),
    onToggleVisibility: () -> Unit = {},
    onMergePreview: () -> Unit = {},
    onJointDocs: () -> Unit = {},
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val stripeColor = remember(note.id) {
        com.example.ui.theme.getNoteStripeColor(note.id)
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    @OptIn(ExperimentalFoundationApi::class)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { currentOnClick() },
                onLongClick = { currentOnLongClick() }
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFF0F6F3) else Color(0xFFFDFDFD),
        border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) brandColor.copy(alpha = 0.45f) else Color(0xFFE0DFD5)),
        shadowElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = note.title.ifBlank { "无标题" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isContentVisible) {
                    if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = note.content.take(40).replace("\n", " "),
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "正文已被隐藏",
                        fontSize = 11.sp,
                        color = Color(0xFF999999),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1
                    )
                }
            }

            // 小眼睛图标
            IconButton(
                onClick = { onToggleVisibility() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (note.isContentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "切换可见性",
                    tint = if (note.isContentVisible) brandColor else Color(0xFFCCCCCC),
                    modifier = Modifier.size(16.dp)
                )
            }

            // 竖排三个点
            Box(modifier = Modifier.padding(end = 4.dp)) {
                var showItemMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    if (note.isArchived) {
                        DropdownMenuItem(
                            text = { Text("恢复", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Restore, null, modifier = Modifier.size(14.dp)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            onClick = {
                                onRestore?.invoke()
                                showItemMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("彻底删除", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(14.dp)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            onClick = {
                                onDelete?.invoke()
                                showItemMenu = false
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    DropdownMenuItem(
                        text = { Text("合并预览", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ChromeReaderMode, null, modifier = Modifier.size(14.dp)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        onClick = {
                            onMergePreview()
                            showItemMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("接合文档", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        onClick = {
                            onJointDocs()
                            showItemMenu = false
                        }
                    )
                }
            }

            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 8.dp).scale(0.8f)
                )
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }
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
