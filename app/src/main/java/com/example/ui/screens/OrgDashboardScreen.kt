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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApprovalStatus
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveOvertimeRequest
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.WorkspaceConfig
import com.example.ui.components.AttendanceBarChart
import com.example.ui.components.DailyChartPoint
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HadirinKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinDanger
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.HadirinWarning
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun OrgDashboardScreen(
    organization: Organization,
    config: WorkspaceConfig,
    members: List<OrgMember>,
    attendances: List<AttendanceRecord>,
    requests: List<LeaveOvertimeRequest>,
    isBusy: Boolean,
    onOpenQrManage: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApprovals: () -> Unit,
    onSeedDemoData: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val orgType = OrganizationType.fromRaw(organization.type)
    var selectedUnitFilter by remember { mutableStateOf("SEMUA") }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val eligibleMembers = remember(members, selectedUnitFilter) {
        members.filter { it.role != OrgRole.PARENT.name }
            .filter { selectedUnitFilter == "SEMUA" || it.unitName == selectedUnitFilter }
    }
    val todayRecords = remember(attendances, todayKey, selectedUnitFilter) {
        attendances.filter { it.dateKey == todayKey }
            .filter { selectedUnitFilter == "SEMUA" || it.unitName == selectedUnitFilter }
    }

    val hadirCount = todayRecords.count { it.status == "HADIR" }
    val lateCount = todayRecords.count { it.status == "TERLAMBAT" }
    val izinCount = todayRecords.count { it.status == "IZIN" || it.status == "SAKIT" }
    val totalMembers = eligibleMembers.size.coerceAtLeast(1)
    val presentUserIds = remember(todayRecords) { todayRecords.map { it.userId }.toSet() }
    val absentMembers = remember(eligibleMembers, presentUserIds) {
        eligibleMembers.filter { it.userId !in presentUserIds }
    }
    val pendingApprovals = requests.count { it.approvalStatus == ApprovalStatus.PENDING.name }
    val attendancePct = (((hadirCount + lateCount).toFloat() / totalMembers.toFloat()) * 100f)
        .roundToInt().coerceIn(0, 100)

    val chartPoints = remember(attendances, selectedUnitFilter) {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE", Locale("in", "ID"))
        val filtered = attendances.filter { selectedUnitFilter == "SEMUA" || it.unitName == selectedUnitFilter }
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = keyFmt.format(cal.time)
            val dayRows = filtered.filter { it.dateKey == key }
            DailyChartPoint(
                label = labelFmt.format(cal.time).take(3),
                hadir = dayRows.count { it.status == "HADIR" },
                terlambat = dayRows.count { it.status == "TERLAMBAT" },
                izin = dayRows.count { it.status == "IZIN" || it.status == "SAKIT" }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = HadirinNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = orgType.moduleName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF93C5FD)
                            )
                            Text(
                                text = organization.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusBadge(
                            label = "${organization.subscriptionPlanId} • ${organization.subscriptionStatus}",
                            statusKey = organization.subscriptionStatus
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kode Gabung: ${organization.joinCode}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = Color.White
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(organization.joinCode))
                                onFeedback("Kode gabung ${organization.joinCode} disalin.")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("copy_join_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Kode")
                        }
                    }
                }
            }
        }

        // Unit Filter Chips
        if (config.units.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedUnitFilter == "SEMUA",
                            onClick = { selectedUnitFilter = "SEMUA" },
                            label = { Text("Semua Unit") },
                            modifier = Modifier.testTag("dash_filter_all")
                        )
                    }
                    items(config.units) { unit ->
                        FilterChip(
                            selected = selectedUnitFilter == unit.name,
                            onClick = { selectedUnitFilter = unit.name },
                            label = { Text(unit.name) }
                        )
                    }
                }
            }
        }

        // 2-Column Bento KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HadirinKpiCard(
                        title = "Total Anggota",
                        value = eligibleMembers.size.toString(),
                        subtitle = "Batas kuota ${organization.memberLimit}",
                        icon = Icons.Default.Groups,
                        accentColor = HadirinNavy,
                        modifier = Modifier.weight(1f)
                    )
                    HadirinKpiCard(
                        title = "Hadir Hari Ini",
                        value = hadirCount.toString(),
                        subtitle = "Tepat waktu",
                        icon = Icons.Default.CheckCircle,
                        accentColor = HadirinSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HadirinKpiCard(
                        title = "Terlambat",
                        value = lateCount.toString(),
                        subtitle = "Melewati toleransi",
                        icon = Icons.Default.AccessTime,
                        accentColor = HadirinWarning,
                        modifier = Modifier.weight(1f)
                    )
                    HadirinKpiCard(
                        title = "Izin / Sakit",
                        value = izinCount.toString(),
                        subtitle = "$pendingApprovals menunggu ACC",
                        icon = Icons.Default.FactCheck,
                        accentColor = HadirinBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HadirinKpiCard(
                        title = "Belum Hadir",
                        value = absentMembers.size.toString(),
                        subtitle = "Belum presensi masuk",
                        icon = Icons.Default.EventBusy,
                        accentColor = HadirinDanger,
                        modifier = Modifier.weight(1f)
                    )
                    HadirinKpiCard(
                        title = "Persentase Hadir",
                        value = "$attendancePct%",
                        subtitle = "Tanggal $todayKey",
                        icon = Icons.Default.Percent,
                        accentColor = HadirinBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Action Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Button(
                        onClick = onOpenQrManage,
                        colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dash_open_qr_button")
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Dinamis")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onOpenReports,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dash_open_reports_button")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Laporan PDF/CSV")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onOpenApprovals,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dash_open_approvals_button")
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifikasi ($pendingApprovals)")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dash_open_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Semua Riwayat")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onSeedDemoData,
                        enabled = !isBusy,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("seed_demo_data_button")
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Isi Data Contoh")
                    }
                }
            }
        }

        // 7-Day Attendance Bar Chart
        item {
            AttendanceBarChart(points = chartPoints)
        }

        // Absent Members Today
        item {
            Text(
                text = "Anggota Belum Presensi Hari Ini (${absentMembers.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (absentMembers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Semua Anggota Sudah Presensi",
                    description = "Tidak ada anggota yang tercatat belum masuk pada hari ini."
                )
            }
        } else {
            items(absentMembers.take(6), key = { "absent_${it.id}" }) { mem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mem.fullName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${mem.memberCode} • ${mem.unitName ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(label = "BELUM HADIR", statusKey = "ALPHA")
                    }
                }
            }
        }

        // Recent Activity Feed
        item {
            Text("Aktivitas Presensi Terbaru", style = MaterialTheme.typography.titleMedium)
        }
        if (attendances.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    title = "Belum Ada Catatan Presensi",
                    description = "Gunakan tombol 'Isi Data Contoh' di atas atau lakukan presensi pertama."
                )
            }
        } else {
            items(attendances.take(8), key = { "recent_${it.id}" }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.memberName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${rec.dateKey} • Masuk ${rec.checkInTimeText} • ${rec.method}",
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
