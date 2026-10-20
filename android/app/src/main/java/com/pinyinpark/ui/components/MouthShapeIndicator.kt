package com.pinyinpark.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 口型动画示意
 * 学习页打开「显示口型动画」后展示：模拟嘴巴开合的动画 + 发音要领提示
 */
@Composable
fun MouthShapeIndicator(
    pinyin: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mouthShape")
    val openness by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openness"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(88.dp)) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val lipHeight = 56.dp.toPx()

                // 嘴唇
                drawRoundRect(
                    color = Color(0xFFFF8A80),
                    topLeft = Offset(4.dp.toPx(), centerY - lipHeight / 2f),
                    size = Size(size.width - 8.dp.toPx(), lipHeight),
                    cornerRadius = CornerRadius(lipHeight / 2f)
                )

                // 口腔（随动画开合）
                val mouthHeight = 8.dp.toPx() + 30.dp.toPx() * openness
                drawOval(
                    color = Color(0xFF6D1B1B),
                    topLeft = Offset(centerX - 20.dp.toPx(), centerY - mouthHeight / 2f),
                    size = Size(40.dp.toPx(), mouthHeight)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👄 ${mouthShapeHint(pinyin)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3E2723)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "跟着读：$pinyin",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8D6E63)
                )
            }
        }
    }
}

/**
 * 发音要领（按声母/韵母的发音部位给出提示）
 */
private fun mouthShapeHint(pinyin: String): String = when (pinyin.lowercase()) {
    "b", "p", "m" -> "双唇紧闭，再突然放开"
    "f" -> "上齿轻咬下唇，吹气"
    "d", "t", "n", "l" -> "舌尖抵住上齿龈"
    "g", "k", "h" -> "舌根抬起，抵住软腭"
    "j", "q", "x" -> "舌面贴近硬腭"
    "zh", "ch", "sh", "r" -> "舌尖上翘，靠近硬腭前部"
    "z", "c", "s" -> "舌尖平放，靠近上齿背"
    "a" -> "嘴巴张大，舌面放平"
    "o" -> "嘴唇收圆，舌头后缩"
    "e" -> "嘴微张，舌后部抬起"
    "i" -> "嘴角向两边展开"
    "u" -> "嘴唇收成小圆孔"
    "ü" -> "嘴唇收圆，舌前部抬起"
    "一声" -> "声音又高又平"
    "二声" -> "声音由低往高升"
    "三声" -> "先降下去再升起来"
    "四声" -> "从高快速降下来"
    else -> "看老师的口型，跟着读"
}
