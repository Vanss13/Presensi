package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Organization
import com.example.data.model.WorkspaceConfig
import com.example.service.NotificationService
import com.example.service.ReportExportService
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReportPeriod(val label: String, val daysBack: Long?) {
    TODAY("Hari Ini", 0L),
    WEEK("7 Hari", 7L),
    MONTH("Bulan Ini", 30L),
    YEAR("Tahun Ini", 365L),
    ALL("Semua", null)
}

@Composable
fun ReportsExportScreen(
    organization: Organization,
    config: WorkspaceConfig,
    attendances: List<AttendanceRecord>,
    onBack: (() -> Unit)? = null,
    onFeedback: (String) -> Unit
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selectedUnit by remember { mutableStateOf("SEMUA") }
    var searchName by remember { mutableStateOf("") }

    val filtered = remember(attendances, selectedPeriod, selectedUnit, searchName) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = fmt.format(Date())
        val cutoffMillis = selectedPeriod.daysBack?.let { d ->
            if (d == 0L) null else System.currentTimeMillis() - d * 86_400_000L
        }

        attendances.filter { rec ->
            val matchPeriod = when {
                selectedPeriod == ReportPeriod.TODAY -> rec.dateKey == todayStr
                cutoffMillis != null -> {
                    val recDate = runCatching { fmt.parse(rec.dateKey)?.time }.getOrNull()
                    recDate == null || recDate >= cutoffMillis
                }
                else -> true
            }
            val matchUnit = selectedUnit == "SEMUA" || rec.unitName == selectedUnit
            val matchName = searchName.isBlank() ||
                rec.memberName.contains(searchName, ignoreCase = true) ||
                rec.memberCode.contains(searchName, ignoreCase = true)
            matchPeriod && matchUnit && matchName
        }
    }

    val hadirCount = filtered.count { it.status == "HADIR" }
    val lateCount = filtered.count { it.status == "TERLAMBAT" }
    val totalOvertime = filtered.sumOf { it.overtimeMinutes }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
                Column {
                    Text("Laporan & Export Kehadiran", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Filter periode, kelas/departemen, lalu unduh PDF atau Excel (CSV).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportPeriod.entries) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) },
                        modifier = Modifier.testTag("report_period_${period.name}")
                    )
                }
            }
        }

        if (config.units.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedUnit == "SEMUA",
                            onClick = { selectedUnit = "SEMUA" },
                            label = { Text("Semua Unit") }
                        )
                    }
                    items(config.units) { u ->
                        FilterChip(
                            selected = selectedUnit == u.name,
                            onClick = { selectedUnit = u.name },
                            label = { Text(u.name) }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchName,
                onValueChange = { searchName = it },
                label = { Text("Cari nama atau kode anggota...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total: ${filtered.size} baris", style = MaterialTheme.typography.titleSmall)
                        Text("Hadir: $hadirCount • Telat: $lateCount • Lembur: ${totalOvertime}m", style = MaterialTheme.typography.bodySmall, color = HadirinBlue)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                ReportExportService.exportAttendancePdf(
                                    context = context,
                                    organization = organization,
                                    filterLabel = "${selectedPeriod.label} / $selectedUnit",
                                    records = filtered
                                ).onSuccess { file ->
                                    ReportExportService.shareReportFile(context, file, "application/pdf")
                                    onFeedback("Laporan PDF siap dibagikan (${file.name}).")
                                }.onFailure {
                                    onFeedback("Gagal membuat PDF: ${it.localizedMessage}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HadirinNavy, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF")
                        }
                        Button(
                            onClick = {
                                ReportExportService.exportAttendanceCsv(
                                    context = context,
                                    organization = organization,
                                    records = filtered
                                ).onSuccess { file ->
                                    ReportExportService.shareReportFile(context, file, "text/csv")
                                    onFeedback("File CSV Excel siap dibagikan (${file.name}).")
                                }.onFailure {
                                    onFeedback("Gagal membuat CSV: ${it.localizedMessage}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HadirinSuccess, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val summary = buildString {
                                appendLine("*REKAP PRESENSI ${organization.name}*")
                                appendLine("Periode: ${selectedPeriod.label} (${selectedUnit})")
                                appendLine("Total Data: ${filtered.size} | Tepat Waktu: $hadirCount | Terlambat: $lateCount | Total Lembur: $totalOvertime mnt")
                            }
                            NotificationService.openWhatsAppMessage(context, organization.phone, summary)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_whatsapp_report_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Ringkasan ke WhatsApp")
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Description,
                    title = "Data Laporan Kosong",
                    description = "Tidak ada catatan presensi yang sesuai dengan filter periode atau unit."
                )
            }
        } else {
            items(filtered, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.memberName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${rec.dateKey} • ${rec.unitName ?: rec.memberCode} • ${rec.checkInTimeText}-${rec.checkOutTimeText ?: "--:--"}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(label = rec.status, statusKey = rec.status)
                    }
                }
            }
        }
    }
}
