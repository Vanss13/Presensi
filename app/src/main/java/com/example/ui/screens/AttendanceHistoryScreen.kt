package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AttendanceRecord
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.JetBrainsMonoFamily
import java.io.File

@Composable
fun AttendanceHistoryScreen(
    attendances: List<AttendanceRecord>
) {
    var statusFilter by remember { mutableStateOf("SEMUA") }
    var previewSelfieRecord by remember { mutableStateOf<AttendanceRecord?>(null) }

    val filtered = remember(attendances, statusFilter) {
        when (statusFilter) {
            "HADIR" -> attendances.filter { it.status == "HADIR" }
            "TERLAMBAT" -> attendances.filter { it.status == "TERLAMBAT" }
            "IZIN" -> attendances.filter { it.status == "IZIN" || it.status == "SAKIT" }
            else -> attendances
        }
    }

    if (previewSelfieRecord != null) {
        val rec = previewSelfieRecord!!
        AlertDialog(
            onDismissRequest = { previewSelfieRecord = null },
            title = { Text("Bukti Foto Selfie — ${rec.memberName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val file = rec.selfiePath?.let { File(it) }
                    if (file != null && file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Foto Selfie Kehadiran",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Foto selfie tersimpan di perangkat pengirim (${rec.deviceInfo}).")
                    }
                    Text("Tanggal: ${rec.dateKey} • Masuk: ${rec.checkInTimeText}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { previewSelfieRecord = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Riwayat Presensi", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("SEMUA" to "Semua", "HADIR" to "Hadir", "TERLAMBAT" to "Terlambat", "IZIN" to "Izin / Sakit")
                    items(chips) { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("history_filter_$key")
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    title = "Belum Ada Riwayat Presensi",
                    description = "Catatan presensi masuk dan pulang akan tampil secara real-time di sini."
                )
            }
        } else {
            items(filtered, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.memberName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${rec.dateKey} • ${rec.unitName ?: rec.memberCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(label = rec.status, statusKey = rec.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Masuk: ${rec.checkInTimeText}  •  Pulang: ${rec.checkOutTimeText ?: "--:--"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFamily
                            )
                            Text(
                                text = rec.method,
                                style = MaterialTheme.typography.labelMedium,
                                color = HadirinBlue
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Jarak ${rec.distanceMeters}m • ${if (rec.withinGeofence) "Dalam Area" else "Luar Area"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!rec.selfiePath.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { previewSelfieRecord = rec },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lihat Foto", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
