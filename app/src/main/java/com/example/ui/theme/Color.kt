package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

fun getNoteStripeColor(id: Any): Color {
    val colors = listOf(
        Color(0xFFC4C3E3), // 优雅紫
        Color(0xFFFFB3A7), // 蜜桃粉
        Color(0xFFFCDD9D), // 暖杏黄
        Color(0xFF9FBAF1), // 静谧蓝
        Color(0xFF8FD1B5), // 薄荷绿
        Color(0xFFE08E79)  // 珊瑚红
    )
    val index = (id.hashCode() and Int.MAX_VALUE) % colors.size
    return colors[index]
}

