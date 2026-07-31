package com.example.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.QuickPhrase
import kotlin.math.abs
import kotlin.math.cos

import androidx.compose.material.icons.filled.LocalFireDepartment

// 辅助数据结构：表达无限循环滑动空间中的虚拟胶囊
private data class VirtualCapsule(
    val phrase: QuickPhrase,
    val rawY: Float // 相对视口中心的垂直像素偏移
)

@Composable
fun QuickPhraseCapsuleWidget(
    phrases: List<QuickPhrase>,
    brandColor: Color,
    onPhraseClick: (QuickPhrase) -> Unit,
    onPhraseUpdate: ((id: String, newLabel: String, newContent: String) -> Unit)? = null,
    onPhraseDelete: ((id: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val effectivePhrases = remember(phrases) {
        if (phrases.isEmpty()) listOf(QuickPhrase("empty", "暂无词组", "")) else phrases
    }

    // 默认折叠完全隐藏在极窄边缘
    var isExpanded by remember { mutableStateOf(false) }
    // 逆时针无缝无限循环滑动的逻辑偏移量
    var scrollOffsetPx by remember { mutableStateOf(0f) }

    // 是否吸附在屏幕右侧边框
    var isRightSide by remember { mutableStateOf(false) }

    // 长按在线快捷编辑对话框状态
    var editingPhrase by remember { mutableStateOf<QuickPhrase?>(null) }
    var editLabelInput by remember { mutableStateOf("") }
    var editContentInput by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    // 基于“常用频率”（usageCount）的交错中心化排序：最高频词组排在中间黄金区域，次高频分置上下两侧
    val frequencyCenteredPhrases = remember(effectivePhrases) {
        if (effectivePhrases.isEmpty()) emptyList()
        else {
            val sortedByUsage = effectivePhrases.sortedByDescending { it.usageCount }
            val result = arrayOfNulls<QuickPhrase>(effectivePhrases.size)
            val mid = effectivePhrases.size / 2
            var left = mid - 1
            var right = mid
            sortedByUsage.forEachIndexed { index, phrase ->
                if (index % 2 == 0) {
                    if (right < effectivePhrases.size) {
                        result[right] = phrase
                        right++
                    } else if (left >= 0) {
                        result[left] = phrase
                        left--
                    }
                } else {
                    if (left >= 0) {
                        result[left] = phrase
                        left--
                    } else if (right < effectivePhrases.size) {
                        result[right] = phrase
                        right++
                    }
                }
            }
            result.filterNotNull()
        }
    }

    val density = LocalDensity.current
    val defaultOffsetYPx = with(density) { (-130).dp.toPx() }
    
    // 安全边界（防止拖拽出屏幕上端或下端）
    val minOffsetYPx = with(density) { (-260).dp.toPx() }
    val maxOffsetYPx = with(density) { (160).dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetXPx by remember { mutableStateOf(0f) }
    var targetOffsetXPx by remember { mutableStateOf(0f) }

    var dragOffsetYPx by remember { mutableStateOf(defaultOffsetYPx) }
    var targetOffsetYPx by remember { mutableStateOf(defaultOffsetYPx) }

    // 拖拽松手时的 X/Y 轴磁吸平滑动画
    val animatedOffsetXPx by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetXPx else targetOffsetXPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "magnetSnapXAnimation"
    )

    val animatedOffsetYPx by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetYPx else targetOffsetYPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "magnetSnapYAnimation"
    )

    // 尺寸配置（精简匹配小屏幕设备）
    val expandedWidth = 156.dp
    val hubHeight = 48.dp
    val capsuleHeight = 32.dp
    val itemSpacing = 8.dp

    val capsuleHeightPx = with(density) { capsuleHeight.toPx() }
    val itemSpacingPx = with(density) { itemSpacing.toPx() }
    val itemTotalStepPx = capsuleHeightPx + itemSpacingPx

    // 当词组发生变化时重置滚动偏移
    LaunchedEffect(frequencyCenteredPhrases) {
        scrollOffsetPx = 0f
    }

    // 胶囊位置计算：当词组数量较少时（< 8个），不进行无限循环复制，确保每个词组在屏幕上仅显示一次；词组较多时启用无缝无限循环
    val virtualCapsules = remember(scrollOffsetPx, frequencyCenteredPhrases) {
        if (frequencyCenteredPhrases.isEmpty()) emptyList()
        else {
            val list = mutableListOf<VirtualCapsule>()
            if (frequencyCenteredPhrases.size < 8) {
                val midIndex = (frequencyCenteredPhrases.size - 1) / 2f
                frequencyCenteredPhrases.forEachIndexed { index, phrase ->
                    val y = (index - midIndex) * itemTotalStepPx + scrollOffsetPx
                    if (y in -330f..330f) {
                        list.add(VirtualCapsule(phrase, y))
                    }
                }
            } else {
                val cyclePx = frequencyCenteredPhrases.size * itemTotalStepPx
                frequencyCenteredPhrases.forEachIndexed { index, phrase ->
                    val baseY = index * itemTotalStepPx + scrollOffsetPx
                    var normY = ((baseY % cyclePx) + cyclePx) % cyclePx
                    if (normY > cyclePx / 2f) {
                        normY -= cyclePx
                    }
                    var y = normY
                    while (y >= -330f) {
                        y -= cyclePx
                    }
                    y += cyclePx
                    while (y <= 330f) {
                        list.add(VirtualCapsule(phrase, y))
                        y += cyclePx
                    }
                }
            }
            list.sortBy { it.rawY }
            list
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentWidthPx = with(density) { constraints.maxWidth.toDp().toPx() }
        val hubWidth = if (isExpanded) 16.dp else 10.dp
        val hubWidthPx = with(density) { hubWidth.toPx() }
        val wheelWidthPx = with(density) { 140.dp.toPx() }

        // 1. 全屏透明阻断/感知层：当挂件处于展开状态时，点击屏幕任意空白处自动归位收回挂件
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isExpanded = false
                        }
                    }
            )
        }

        // 2. 可左右 & 上下自由拖拽磁吸的挂件主体
        Box(
            modifier = Modifier
                .align(if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart)
                .offset(
                    x = if (isRightSide) {
                        with(density) { (animatedOffsetXPx - (parentWidthPx - hubWidthPx)).toDp() }
                    } else {
                        with(density) { animatedOffsetXPx.toDp() }
                    },
                    y = with(density) { animatedOffsetYPx.toDp() }
                )
        ) {
            if (isRightSide) {
                // 靠右模式：手柄定位在最右侧，胶囊向屏幕内侧(左)平滑展开
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 展开后的弧形无缝循环胶囊视口 (靠右模式，向左展开)
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                    ) {
                        CapsuleWheelContent(
                            isRightSide = true,
                            virtualCapsules = virtualCapsules,
                            brandColor = brandColor,
                            itemTotalStepPx = itemTotalStepPx,
                            capsuleHeight = capsuleHeight,
                            density = density,
                            onScroll = { delta ->
                                if (frequencyCenteredPhrases.size < 8) {
                                    val maxDrag = (frequencyCenteredPhrases.size * itemTotalStepPx) / 2f + 40f
                                    scrollOffsetPx = (scrollOffsetPx + delta).coerceIn(-maxDrag, maxDrag)
                                } else {
                                    scrollOffsetPx += delta
                                }
                            },
                            onPhraseClick = { phrase ->
                                if (phrase.id != "empty") {
                                    onPhraseClick(phrase)
                                }
                                isExpanded = false
                            },
                            onPhraseLongClick = { phrase ->
                                if (phrase.id != "empty") {
                                    editingPhrase = phrase
                                    editLabelInput = phrase.label
                                    editContentInput = phrase.content
                                    showEditDialog = true
                                }
                            }
                        )
                    }

                    // 枢轴手柄
                    HandleBox(
                        isRightSide = true,
                        isExpanded = isExpanded,
                        hubHeight = hubHeight,
                        brandColor = brandColor,
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Y轴磁吸
                            val clampedY = dragOffsetYPx.coerceIn(minOffsetYPx, maxOffsetYPx)
                            val snapNodesY = listOf(minOffsetYPx, defaultOffsetYPx, maxOffsetYPx)
                            val closestSnapY = snapNodesY.minByOrNull { abs(it - clampedY) } ?: clampedY
                            targetOffsetYPx = closestSnapY
                            dragOffsetYPx = closestSnapY

                            // X轴左右边界磁吸计算
                            if (dragOffsetXPx > parentWidthPx / 2f) {
                                isRightSide = true
                                targetOffsetXPx = parentWidthPx - hubWidthPx
                                dragOffsetXPx = parentWidthPx - hubWidthPx
                            } else {
                                isRightSide = false
                                targetOffsetXPx = 0f
                                dragOffsetXPx = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            targetOffsetYPx = dragOffsetYPx.coerceIn(minOffsetYPx, maxOffsetYPx)
                            targetOffsetXPx = if (isRightSide) parentWidthPx - hubWidthPx else 0f
                        },
                        onDrag = { dragAmountX, dragAmountY ->
                            dragOffsetXPx += dragAmountX
                            dragOffsetYPx += dragAmountY
                        },
                        onClick = { isExpanded = !isExpanded }
                    )
                }
            } else {
                // 靠左模式：手柄在左，胶囊向右平滑展开
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HandleBox(
                        isRightSide = false,
                        isExpanded = isExpanded,
                        hubHeight = hubHeight,
                        brandColor = brandColor,
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            val clampedY = dragOffsetYPx.coerceIn(minOffsetYPx, maxOffsetYPx)
                            val snapNodesY = listOf(minOffsetYPx, defaultOffsetYPx, maxOffsetYPx)
                            val closestSnapY = snapNodesY.minByOrNull { abs(it - clampedY) } ?: clampedY
                            targetOffsetYPx = closestSnapY
                            dragOffsetYPx = closestSnapY

                            if (dragOffsetXPx > parentWidthPx / 2f) {
                                isRightSide = true
                                targetOffsetXPx = parentWidthPx - hubWidthPx
                                dragOffsetXPx = parentWidthPx - hubWidthPx
                            } else {
                                isRightSide = false
                                targetOffsetXPx = 0f
                                dragOffsetXPx = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            targetOffsetYPx = dragOffsetYPx.coerceIn(minOffsetYPx, maxOffsetYPx)
                            targetOffsetXPx = if (isRightSide) parentWidthPx - hubWidthPx else 0f
                        },
                        onDrag = { dragAmountX, dragAmountY ->
                            dragOffsetXPx += dragAmountX
                            dragOffsetYPx += dragAmountY
                        },
                        onClick = { isExpanded = !isExpanded }
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                    ) {
                        CapsuleWheelContent(
                            isRightSide = false,
                            virtualCapsules = virtualCapsules,
                            brandColor = brandColor,
                            itemTotalStepPx = itemTotalStepPx,
                            capsuleHeight = capsuleHeight,
                            density = density,
                            onScroll = { delta ->
                                if (frequencyCenteredPhrases.size < 8) {
                                    val maxDrag = (frequencyCenteredPhrases.size * itemTotalStepPx) / 2f + 40f
                                    scrollOffsetPx = (scrollOffsetPx + delta).coerceIn(-maxDrag, maxDrag)
                                } else {
                                    scrollOffsetPx += delta
                                }
                            },
                            onPhraseClick = { phrase ->
                                onPhraseClick(phrase)
                                isExpanded = false
                            },
                            onPhraseLongClick = { phrase ->
                                editingPhrase = phrase
                                editLabelInput = phrase.label
                                editContentInput = phrase.content
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 快捷在线编辑浮动弹窗
    if (showEditDialog && editingPhrase != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = brandColor
                )
            },
            title = {
                Text(
                    text = "编辑快捷词组",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1D1D1F)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editLabelInput,
                        onValueChange = { editLabelInput = it },
                        label = { Text("胶囊标签（如：转折过渡）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editContentInput,
                        onValueChange = { editContentInput = it },
                        label = { Text("点击自动上屏的短句/词组内容") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLabel = editLabelInput.trim()
                        val newContent = editContentInput
                        if (newLabel.isNotEmpty() && newContent.isNotEmpty()) {
                            onPhraseUpdate?.invoke(editingPhrase!!.id, newLabel, newContent)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPhraseDelete != null) {
                        TextButton(
                            onClick = {
                                onPhraseDelete.invoke(editingPhrase!!.id)
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "删除", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("删除")
                        }
                    }
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("取消", color = Color(0xFF8E8E93))
                    }
                }
            }
        )
    }
}

// 提取展开后的轮盘视口为独立 Composable，复用左右靠侧渲染
@Composable
private fun CapsuleWheelContent(
    isRightSide: Boolean,
    virtualCapsules: List<VirtualCapsule>,
    brandColor: Color,
    itemTotalStepPx: Float,
    capsuleHeight: Dp,
    density: androidx.compose.ui.unit.Density,
    onScroll: (Float) -> Unit,
    onPhraseClick: (QuickPhrase) -> Unit,
    onPhraseLongClick: (QuickPhrase) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(140.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    onScroll(dragAmount)
                }
            },
        contentAlignment = if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // 绘制逆时针弧形分支连线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = if (isRightSide) size.width else 0f
            val startY = size.height / 2f

            virtualCapsules.forEach { capsule ->
                val yCenter = startY + capsule.rawY
                val dy = capsule.rawY
                val rad = (dy / 180f).coerceIn(-1.57f, 1.57f)
                // 靠右侧时弧线向左拱出（带负号）
                val archVal = (cos(rad) * 24.dp.toPx()).coerceAtLeast(2.dp.toPx())
                val archX = if (isRightSide) -archVal else archVal

                val path = Path().apply {
                    moveTo(startX, startY)
                    val controlX = startX + archX * 0.7f
                    val endX = if (isRightSide) startX - 14.dp.toPx() + archX else startX + 14.dp.toPx() + archX
                    cubicTo(
                        controlX, startY,
                        controlX, yCenter,
                        endX, yCenter
                    )
                }

                val absY = abs(dy)
                val lineAlpha = if (absY <= 1.25f * itemTotalStepPx) {
                    0.5f
                } else {
                    (0.5f - ((absY - 1.25f * itemTotalStepPx) / (2.2f * itemTotalStepPx)).coerceIn(0f, 1f) * 0.35f)
                }

                drawPath(
                    path = path,
                    color = brandColor.copy(alpha = lineAlpha),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // 渲染弧形排列的胶囊列表
        Box(modifier = Modifier.fillMaxSize()) {
            virtualCapsules.forEach { capsule ->
                val rawY = capsule.rawY
                val absY = abs(rawY)

                val rad = (rawY / 180f).coerceIn(-1.57f, 1.57f)
                val archDpVal = (cos(rad) * 24.dp.value).coerceAtLeast(0f).dp
                val offsetX = if (isRightSide) -(14.dp + archDpVal) else (14.dp + archDpVal)

                val itemAlpha = if (absY <= 1.25f * itemTotalStepPx) {
                    1.0f
                } else {
                    1.0f - ((absY - 1.25f * itemTotalStepPx) / (2.2f * itemTotalStepPx)).coerceIn(0f, 1f) * 0.65f
                }

                val itemScale = if (absY <= 1.25f * itemTotalStepPx) {
                    1.0f
                } else {
                    1.0f - ((absY - 1.25f * itemTotalStepPx) / (2.2f * itemTotalStepPx)).coerceIn(0f, 1f) * 0.12f
                }

                Box(
                    modifier = Modifier
                        .align(if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart)
                        .offset(x = offsetX, y = with(density) { rawY.toDp() })
                        .graphicsLayer {
                            this.alpha = itemAlpha
                            this.scaleX = itemScale
                            this.scaleY = itemScale
                        }
                        .wrapContentWidth()
                        .height(capsuleHeight)
                        .shadow(
                            elevation = if (absY <= 1.25f * itemTotalStepPx) 3.dp else 1.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = brandColor.copy(alpha = 0.3f * itemAlpha)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = itemAlpha),
                                    Color(0xFFFAFAFD).copy(alpha = itemAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = if (absY <= 1.25f * itemTotalStepPx) 1.2.dp else 0.8.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    brandColor.copy(alpha = 0.6f * itemAlpha),
                                    brandColor.copy(alpha = 0.25f * itemAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .pointerInput(capsule.phrase) {
                            detectTapGestures(
                                onTap = { onPhraseClick(capsule.phrase) },
                                onLongPress = { onPhraseLongClick(capsule.phrase) }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (capsule.phrase.usageCount > 0) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "常用",
                                tint = brandColor.copy(alpha = if (itemAlpha < 0.6f) 0.6f else 0.95f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = capsule.phrase.label,
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            softWrap = false,
                            fontWeight = if (absY <= 1.25f * itemTotalStepPx) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF2C2C2E).copy(alpha = if (itemAlpha < 0.6f) 0.7f else 1.0f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleBox(
    isRightSide: Boolean,
    isExpanded: Boolean,
    hubHeight: Dp,
    brandColor: Color,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val shape = if (isRightSide) {
        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    }

    Box(
        modifier = Modifier
            .width(if (isExpanded) 16.dp else 10.dp)
            .height(hubHeight)
            .graphicsLayer {
                this.alpha = if (isExpanded) 1.0f else 0.5f
            }
            .shadow(
                elevation = if (isExpanded) 4.dp else 1.dp,
                shape = shape
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isExpanded) {
                        listOf(brandColor.copy(alpha = 0.95f), brandColor.copy(alpha = 0.85f))
                    } else {
                        listOf(brandColor.copy(alpha = 0.75f), brandColor.copy(alpha = 0.5f))
                    }
                ),
                shape = shape
            )
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.7f),
                shape = shape
            )
            .pointerInput(isRightSide) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val arrowIcon = if (!isRightSide) {
            if (isExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight
        } else {
            if (isExpanded) Icons.Default.ChevronRight else Icons.Default.ChevronLeft
        }
        Icon(
            imageVector = arrowIcon,
            contentDescription = if (isExpanded) "收起快捷词组" else "展开快捷词组",
            tint = Color.White,
            modifier = Modifier.size(if (isExpanded) 14.dp else 10.dp)
        )
    }
}




