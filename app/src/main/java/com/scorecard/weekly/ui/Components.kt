package com.scorecard.weekly.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scorecard.weekly.R
import com.scorecard.weekly.data.Area
import com.scorecard.weekly.data.MAX_TOTAL_SCORE
import com.scorecard.weekly.ui.theme.Negative
import com.scorecard.weekly.ui.theme.Positive

val Area.color: Color get() = Color(colorArgb)

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF5B5BD6), Color(0xFF9A5BD6)))

/** A white (or dark-grey) rounded card on the grey background. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Big page title with a small coloured overline. */
@Composable
fun ScreenHeader(
    title: String,
    overline: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    overline.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                )
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        trailing()
    }
}

/** Animated circular progress ring with anything in the middle. */
@Composable
fun ScoreRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val topLeft = Offset(sw / 2, sw / 2)
            val arcSize = Size(size.width - sw, size.height - sw)
            drawArc(
                color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(sw),
            )
            if (animated > 0f) {
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Animated rounded horizontal progress bar. */
@Composable
fun ScoreBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bar",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Emoji on a soft tinted rounded square. */
@Composable
fun IconBubble(emoji: String, color: Color, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.45f).sp, textAlign = TextAlign.Center)
    }
}

/** Round checkbox filled with the area colour when ticked. */
@Composable
fun CheckCircle(checked: Boolean, color: Color) {
    val background by animateColorAsState(if (checked) color else Color.Transparent, label = "check")
    val border = if (checked) color else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Small "▲ 3" / "▼ 2" pill comparing with the previous week. */
@Composable
fun DeltaChip(delta: Int) {
    val (color, text) = when {
        delta > 0 -> Positive to "▲ $delta"
        delta < 0 -> Negative to "▼ ${-delta}"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to "± 0"
    }
    Box(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** A short verdict for a week's total. */
fun verdict(total: Int, isEmpty: Boolean): String {
    if (isEmpty) return "Not scored yet"
    val f = total / MAX_TOTAL_SCORE.toFloat()
    return when {
        f >= 0.8f -> "Great week"
        f >= 0.6f -> "Solid week"
        f >= 0.4f -> "Mixed week"
        else -> "Tough week"
    }
}

@Composable
fun EmptyMessage(emoji: String, title: String, text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconBubble(emoji, MaterialTheme.colorScheme.primary, size = 72.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
            )
            Text(
                text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class ChartSeries(val color: Color, val values: List<Int>)

/** Smooth, animated line chart. With [fill] the area under the line gets a soft gradient. */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    maxValue: Int,
    gridStep: Int,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotCenter = MaterialTheme.colorScheme.surface
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Canvas(modifier) {
        val left = 28.dp.toPx()
        val right = 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 24.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val n = xLabels.size

        fun x(i: Int) = left + if (n <= 1) w / 2 else w * i / (n - 1)
        fun y(v: Int) = top + h * (1f - v / maxValue.toFloat())

        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        for (v in 0..maxValue step gridStep) {
            drawLine(gridColor, Offset(left, y(v)), Offset(left + w, y(v)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            val label = textMeasurer.measure(v.toString(), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 8.dp.toPx(), y(v) - label.size.height / 2))
        }
        val step = if (n > 7) 2 else 1
        xLabels.forEachIndexed { i, text ->
            if (i % step == 0 || i == n - 1) {
                val label = textMeasurer.measure(text, labelStyle)
                drawText(label, topLeft = Offset(x(i) - label.size.width / 2, top + h + 6.dp.toPx()))
            }
        }

        clipRect(right = left + w * reveal.value + 8.dp.toPx()) {
            series.forEach { s ->
                if (s.values.isEmpty()) return@forEach
                val points = s.values.mapIndexed { i, v -> Offset(x(i), y(v)) }
                val line = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        val mx = (p0.x + p1.x) / 2
                        cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
                    }
                }
                if (fill) {
                    val area = Path().apply {
                        addPath(line)
                        lineTo(points.last().x, top + h)
                        lineTo(points.first().x, top + h)
                        close()
                    }
                    drawPath(
                        area,
                        Brush.verticalGradient(listOf(s.color.copy(alpha = 0.28f), Color.Transparent), startY = top, endY = top + h),
                    )
                }
                drawPath(line, s.color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val last = points.last()
                drawCircle(s.color, radius = 6.dp.toPx(), center = last)
                drawCircle(dotCenter, radius = 2.5.dp.toPx(), center = last)
            }
        }
    }
}
