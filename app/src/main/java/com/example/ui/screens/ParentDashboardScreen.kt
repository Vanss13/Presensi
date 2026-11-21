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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.AttendanceRecord
import com.example.data.model.OrgMember
import com.example.data.model.Organization
import com.example.service.NotificationService
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentDashboardScreen(
    parentUid: String,
    organization: Organization,
    linkedChildren: List<OrgMember>,
    childAttendances: List<AttendanceRecord>,
    notifications: List<AppNotification>,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Monitoring Wali Murid", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Pantau jam masuk, jam pulang, dan keterlambatan anak di ${organization.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (linkedChildren.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Hubungkan Akun Orang Tua ke Siswa", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Berikan ID Wali berikut kepada Admin Sekolah agar akun Anda dihubungkan ke data siswa:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = parentUid,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(parentUid))
                                        onFeedback("ID Wali disalin ke clipboard.")
                                    },
                                    modifier = Modifier.testTag("copy_parent_uid_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin ID Wali")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                NotificationService.openWhatsAppMessage(
                                    context,
                                    organization.phone,
                                    "Halo Admin ${organization.name}, mohon hubungkan akun Wali Murid saya (ID: $parentUid) ke data anak saya."
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim ID Wali via WhatsApp Sekolah")
                        }
                    }
                }
            }
        } else {
            items(linkedChildren, key = { it.id }) { child ->
                val todayAtt = childAttendances.firstOrNull { it.dateKey == todayKey && it.userId == child.userId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(child.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${child.memberCode} • ${child.unitName ?: "Kelas"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                label = todayAtt?.status ?: "BELUM PRESENSI",
                                statusKey = todayAtt?.status ?: "PENDING"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Masuk: ${todayAtt?.checkInTimeText ?: "--:--"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFamily
                            )
                            Text(
                                text = "Pulang: ${todayAtt?.checkOutTimeText ?: "--:--"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFamily
                            )
                        }
                        if ((todayAtt?.lateMinutes ?: 0L) > 0L) {
                            Text(
                                text = "Catatan: Terlambat ${todayAtt?.lateMinutes} menit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Notifikasi & Riwayat Anak", style = MaterialTheme.typography.titleMedium)
        }

        if (childAttendances.isEmpty() && notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.ChildCare,
                    title = "Belum Ada Aktivitas Anak",
                    description = "Notifikasi masuk/pulang dan riwayat presensi siswa akan muncul otomatis di sini."
                )
            }
        } else {
            items(childAttendances.take(15), key = { it.id }) { rec ->
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
                            Text("${rec.memberName} (${rec.dateKey})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Masuk ${rec.checkInTimeText} • Pulang ${rec.checkOutTimeText ?: "-"} • ${rec.method}",
                                style = MaterialTheme.typography.bodySmall,
                                color = HadirinBlue
                            )
                        }
                        StatusBadge(label = rec.status, statusKey = rec.status)
                    }
                }
            }
        }
    }
}
