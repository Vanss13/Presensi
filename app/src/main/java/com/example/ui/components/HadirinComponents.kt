package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinBlueSoft
import com.example.ui.theme.HadirinDanger
import com.example.ui.theme.HadirinDangerSoft
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinOutline
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.HadirinSuccessSoft
import com.example.ui.theme.HadirinWarning
import com.example.ui.theme.HadirinWarningSoft
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Long): String {
    if (amount <= 0L) return "Gratis"
    val nf = NumberFormat.getNumberInstance(Locale("in", "ID"))
    return "Rp ${nf.format(amount)}"
}

@Composable
fun HadirinKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = HadirinBlue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    statusKey: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (statusKey.uppercase()) {
        "HADIR", "APPROVED", "ACTIVE", "PAID" -> HadirinSuccessSoft to HadirinSuccess
        "TERLAMBAT", "PENDING", "TRIAL", "IZIN" -> HadirinWarningSoft to Color(0xFFB45309)
        "ALPHA", "REJECTED", "EXPIRED", "FAILED", "SAKIT" -> HadirinDangerSoft to HadirinDanger
        else -> HadirinBlueSoft to HadirinNavy
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HadirinBlueSoft
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = HadirinNavy,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class DailyChartPoint(
    val label: String,
    val hadir: Int,
    val terlambat: Int,
    val izin: Int
)

@Composable
fun AttendanceBarChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
) {
    val maxTotal = (points.maxOfOrNull { it.hadir + it.terlambat + it.izin } ?: 1).coerceAtLeast(1)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grafik Kehadiran 7 Hari",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = HadirinSuccess, label = "Hadir")
                    ChartLegendDot(color = HadirinWarning, label = "Telat")
                    ChartLegendDot(color = HadirinBlue, label = "Izin")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val count = points.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                    val chartHeight = size.height - 8.dp.toPx()

                    // Subtle horizontal grid lines
                    for (i in 0..3) {
                        val y = chartHeight * (i / 3f)
                        drawLine(
                            color = HadirinOutline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                        val total = pt.hadir + pt.terlambat + pt.izin
                        if (total == 0) {
                            drawRoundRect(
                                color = HadirinOutline.copy(alpha = 0.55f),
                                topLeft = Offset(left, chartHeight - 6.dp.toPx()),
                                size = Size(barWidth, 6.dp.toPx()),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        } else {
                            var currentBottom = chartHeight
                            val segments = listOf(
                                pt.hadir to HadirinSuccess,
                                pt.terlambat to HadirinWarning,
                                pt.izin to HadirinBlue
                            )
                            segments.forEach { (value, color) ->
                                if (value > 0) {
                                    val segH = (value.toFloat() / maxTotal.toFloat()) * chartHeight
                                    val top = (currentBottom - segH).coerceAtLeast(0f)
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, segH),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                    currentBottom = top
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { pt ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
