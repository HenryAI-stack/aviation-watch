package com.henryai.aviationwatch.ui.directto

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import com.henryai.aviationwatch.ui.theme.AviationColors

/**
 * Course deviation indicator: two dots each side, full scale at the outer dot.
 * [deflection] in [-1, 1], negative = needle left (course is to the left).
 */
@Composable
fun CdiIndicator(deflection: Float, modifier: Modifier = Modifier) {
    val scale = MaterialTheme.colorScheme.onSurface
    val needle = AviationColors.Magenta
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val halfWidth = size.width * 0.42f
        val dotRadius = 3.dp.toPx()
        for (i in listOf(-2, -1, 1, 2)) {
            drawCircle(
                color = scale,
                radius = dotRadius,
                center = Offset(cx + halfWidth * i / 2f, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        // Aircraft reference mark in the centre.
        drawLine(
            color = scale,
            start = Offset(cx, cy - size.height * 0.3f),
            end = Offset(cx, cy + size.height * 0.3f),
            strokeWidth = 2.dp.toPx(),
        )
        val x = cx + halfWidth * deflection.coerceIn(-1f, 1f)
        drawLine(
            color = needle,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Bearing pointer relative to the current track: straight up = target dead ahead,
 * rotated right = turn right. Shows only the ring while the track is unknown.
 */
@Composable
fun BearingPointer(relativeBearing: Double?, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.outline
    val arrow = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(color = ring, radius = r - 2.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        if (relativeBearing != null) {
            rotate(degrees = relativeBearing.toFloat(), pivot = c) {
                val path = Path().apply {
                    moveTo(c.x, c.y - r * 0.8f)
                    lineTo(c.x + r * 0.4f, c.y + r * 0.5f)
                    lineTo(c.x, c.y + r * 0.25f)
                    lineTo(c.x - r * 0.4f, c.y + r * 0.5f)
                    close()
                }
                drawPath(path, color = arrow)
            }
        }
    }
}
