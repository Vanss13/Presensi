package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.AuditLog
import com.example.data.model.OrgRole
import com.example.data.model.Organization
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.service.NotificationService
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinDanger
import com.example.ui.theme.HadirinNavy

@Composable
fun ProfileSettingsScreen(
    userProfile: UserProfile?,
    activeOrg: Organization,
    activeRole: OrgRole,
    memberships: List<UserMembership>,
    auditLogs: List<AuditLog>,
    isBusy: Boolean,
    onUpdateProfile: (displayName: String, phone: String?) -> Unit,
    onSwitchOrganization: (orgId: String) -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(userProfile?.displayName) { mutableStateOf(userProfile?.displayName ?: "") }
    var phone by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone ?: "") }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationService.showLocalNotification(
                context,
                "Pengingat HADIRIN Aktif",
                "Notifikasi pengingat masuk dan pulang siap dikirim ke perangkat Anda."
            )
            onFeedback("Notifikasi uji coba dikirim.")
        } else {
            onFeedback("Izin notifikasi ditolak di pengaturan perangkat.")
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!userProfile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile?.photoUrl,
                                contentDescription = "Foto Profil",
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile?.displayName ?: "Pengguna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(userProfile?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(label = activeRole.label, statusKey = "ACTIVE")
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                    )
                    Button(
                        onClick = { onUpdateProfile(displayName, phone) },
                        enabled = !isBusy && displayName.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Profil")
                    }
                }
            }
        }

        // Organization Switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Organisasi Saya (${memberships.size})", style = MaterialTheme.typography.titleMedium)
                    memberships.forEach { m ->
                        val isActive = m.organizationId == activeOrg.id
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.organizationName, style = MaterialTheme.typography.titleSmall)
                                    Text("${OrgRole.fromRaw(m.role).label} • ${m.memberCode}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (!isActive) {
                                    OutlinedButton(onClick = { onSwitchOrganization(m.organizationId) }) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Buka")
                                    }
                                } else {
                                    StatusBadge(label = "AKTIF", statusKey = "ACTIVE")
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenOnboarding,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_or_join_org_button")
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daftarkan atau Gabung Organisasi Lain")
                    }
                    if (userProfile?.superAdmin == true) {
                        Button(
                            onClick = onOpenSuperAdmin,
                            colors = ButtonDefaults.buttonColors(containerColor = HadirinNavy, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_super_admin")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka Konsol Super Admin SaaS")
                        }
                    }
                }
            }
        }

        // Notification Settings
        item {
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            NotificationService.showLocalNotification(
                                context,
                                "Pengingat Presensi HADIRIN",
                                "Jangan lupa lakukan presensi di ${activeOrg.name} sesuai jadwal Anda."
                            )
                            onFeedback("Notifikasi uji coba dikirim.")
                        } else {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        NotificationService.showLocalNotification(
                            context,
                            "Pengingat Presensi HADIRIN",
                            "Jangan lupa lakukan presensi di ${activeOrg.name} sesuai jadwal Anda."
                        )
                        onFeedback("Notifikasi uji coba dikirim.")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("test_notification_button")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aktifkan & Uji Notifikasi Pengingat")
            }
        }

        // Audit Log (Staff)
        if ((activeRole == OrgRole.ORG_ADMIN || activeRole == OrgRole.SUPERVISOR) && auditLogs.isNotEmpty()) {
            item {
                Text("Audit Log Aktivitas (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(auditLogs.take(10), key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${log.action} • ${log.actorName}", style = MaterialTheme.typography.labelLarge)
                        Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, HadirinDanger),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = HadirinDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar Akun Google", color = HadirinDanger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
