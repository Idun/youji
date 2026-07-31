package com.example.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.component.YouJiLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    brandColor: Color = Color(0xFF1B7679),
    onSplashFinished: () -> Unit
) {
    // 渐显动画
    val alphaAnim = rememberInfiniteTransition(label = "infinite").animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val entranceAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val entranceScale = remember { androidx.compose.animation.core.Animatable(0.8f) }

    LaunchedEffect(Unit) {
        // 入场动画
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        // 延时 1.8 秒后跳转
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAF6)), // 温暖干净的亮白（略带纸张暖色）
        contentAlignment = Alignment.Center
    ) {
        // 1. 中间 Logo 和项目名
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(y = (-40).dp)
                .alpha(entranceAlpha.value)
                .scale(entranceScale.value)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "由记",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                letterSpacing = 4.sp
            )
        }

        // 2. 底部加载部分
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = brandColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = "Loading...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF95A5A6),
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}
