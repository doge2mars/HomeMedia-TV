package org.moontechlab.selenetv.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val HmTeal = Color(0xFF00BFA5)
private val HmBlue = Color(0xFF2979FF)

/**
 * HomeMedia 加载动画：青蓝渐变圆弧旋转 + 中央白色播放三角轻呼吸，
 * 与 App 图标（青蓝渐变 + 白三角）同款式，替代原来的月牙动画。
 * 尺寸自适应：调用方只传 size，圆环粗细与三角按比例缩放。
 */
@Composable
fun HomeMediaSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    foreground: Color = Color.White,
) {
    val infinite = rememberInfiniteTransition(label = "hm_spinner")
    // 渐变弧 1.1s 一圈匀速旋转
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hm_rotation",
    )
    // 中央三角 0.9s 一次轻呼吸
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hm_pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val px = size.toPx()
        val stroke = px * 0.09f
        val inset = stroke / 2f
        val arcSize = Size(px - stroke, px - stroke)
        // 底环：淡白整圆，衬托旋转弧
        drawArc(
            color = foreground.copy(alpha = 0.14f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke),
        )
        // 旋转弧：青绿到蓝对角渐变，留 90° 缺口，圆头
        rotate(rotation) {
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(HmTeal, HmBlue),
                    start = Offset(0f, 0f),
                    end = Offset(px, px),
                ),
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // 中央白三角：比例与 App 图标三角一致（宽 0.30、高 0.35），随呼吸缩放
        val cx = px / 2f
        val cy = px / 2f
        val halfH = px * 0.175f * pulse
        val leftX = cx - px * 0.13f * pulse
        val apexX = cx + px * 0.17f * pulse
        val triangle = Path().apply {
            moveTo(leftX, cy - halfH)
            lineTo(leftX, cy + halfH)
            lineTo(apexX, cy)
            close()
        }
        drawPath(triangle, foreground)
    }
}
