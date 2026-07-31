package com.example.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun YouJiLogo(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF1B7679)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.08f
        val notebookWidth = size.width * 0.62f
        val notebookHeight = size.height * 0.72f
        val notebookLeft = size.width * 0.08f
        val notebookTop = size.height * 0.16f

        // 1. 绘制笔记本的外边框
        drawRoundRect(
            color = tint,
            topLeft = Offset(notebookLeft, notebookTop),
            size = Size(notebookWidth, notebookHeight),
            cornerRadius = CornerRadius(size.width * 0.08f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. 绘制笔记本内部的三条横线
        drawLine(
            color = tint,
            start = Offset(notebookLeft + notebookWidth * 0.22f, notebookTop + notebookHeight * 0.28f),
            end = Offset(notebookLeft + notebookWidth * 0.58f, notebookTop + notebookHeight * 0.28f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(notebookLeft + notebookWidth * 0.22f, notebookTop + notebookHeight * 0.5f),
            end = Offset(notebookLeft + notebookWidth * 0.75f, notebookTop + notebookHeight * 0.5f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(notebookLeft + notebookWidth * 0.22f, notebookTop + notebookHeight * 0.72f),
            end = Offset(notebookLeft + notebookWidth * 0.62f, notebookTop + notebookHeight * 0.72f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )

        // 3. 绘制羽毛笔
        // 羽毛路径 (右侧斜向上)
        val featherPath = Path().apply {
            moveTo(size.width * 0.92f, size.height * 0.12f)
            quadraticTo(size.width * 0.88f, size.height * 0.38f, size.width * 0.66f, size.height * 0.68f)
            lineTo(size.width * 0.56f, size.height * 0.74f)
            quadraticTo(size.width * 0.62f, size.height * 0.44f, size.width * 0.92f, size.height * 0.12f)
        }
        drawPath(
            path = featherPath,
            color = tint
        )

        // 绘制羽毛的主干/脊线 (白色分割线，增加质感)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.92f, size.height * 0.12f),
            end = Offset(size.width * 0.56f, size.height * 0.74f),
            strokeWidth = strokeWidth * 0.25f,
            cap = StrokeCap.Round
        )

        // 绘制伸出底部的笔尖
        drawLine(
            color = tint,
            start = Offset(size.width * 0.56f, size.height * 0.74f),
            end = Offset(size.width * 0.44f, size.height * 0.92f),
            strokeWidth = strokeWidth * 0.55f,
            cap = StrokeCap.Round
        )
    }
}
