package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.BillingTransaction
import com.example.data.model.MemberSubType
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.OrgUnit
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.WorkSchedule
import com.example.data.model.WorkspaceConfig
import com.example.service.LocationService
import com.example.service.QrCodeService
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OrgManagementScreen(
    organization: Organization,
    role: OrgRole,
    config: WorkspaceConfig,
    members: List<OrgMember>,
    plans: List<SubscriptionPlan>,
    transactions: List<BillingTransaction>,
    initialTab: Int = 0,
    isBusy: Boolean,
    onAddRosterMember: (
        fullName: String,
        memberCode: String,
        memberRole: OrgRole,
        subType: MemberSubType,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        email: String?,
        phone: String?
    ) -> Unit,
    onUpdateMemberAssignment: (
        member: OrgMember,
        newRole: OrgRole,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        parentUid: String?
    ) -> Unit,
    onAddUnit: (kind: String, name: String, code: String, supervisor: String) -> Unit,
    onAddSchedule: (name: String, start: String, end: String, tolerance: Long, workDays: String) -> Unit,
    onAddLocation: (name: String, lat: Double, lon: Double, radius: Long) -> Unit,
    onRotateDynamicQr: (locationName: String) -> Unit,
    onUpdatePolicies: (requireGeofence: Boolean, requireDynamicQr: Boolean, requireSelfie: Boolean) -> Unit,
    onUpgradePlan: (plan: SubscriptionPlan, paymentMethod: String) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdmin = role == OrgRole.ORG_ADMIN
    val isEdu = OrganizationType.fromRaw(organization.type) == OrganizationType.EDU

    val tabLabels = remember(isAdmin) {
        if (isAdmin) listOf("Anggota", "Unit/Jadwal", "Lokasi/QR", "Paket")
        else listOf("Anggota", "Unit/Jadwal", "Lokasi/QR")
    }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, tabLabels.lastIndex)) }

    // Member dialogs state
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<OrgMember?>(null) }
    var memberSearch by remember { mutableStateOf("") }

    // Unit & schedule inline states
    var unitName by remember { mutableStateOf("") }
    var unitCode by remember { mutableStateOf("") }
    var unitSupervisor by remember { mutableStateOf("") }

    var schName by remember { mutableStateOf("") }
    var schStart by remember { mutableStateOf(if (isEdu) "07:00" else "08:00") }
    var schEnd by remember { mutableStateOf(if (isEdu) "14:00" else "17:00") }
    var schTolerance by remember { mutableStateOf("15") }

    // Location & QR state
    var locName by remember { mutableStateOf("") }
    var locLat by remember { mutableStateOf("-6.200000") }
    var locLon by remember { mutableStateOf("106.816666") }
    var locRadius by remember { mutableStateOf("150") }
    var autoRotateQr by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LaunchedEffect(autoRotateQr, organization.id) {
        if (autoRotateQr) {
            while (autoRotateQr) {
                delay(60_000L)
                val locLabel = config.locations.firstOrNull()?.name ?: organization.name
                onRotateDynamicQr(locLabel)
            }
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            scope.launch {
                LocationService.fetchCurrentLocation(context).onSuccess { c ->
                    locLat = c.latitude.toString()
                    locLon = c.longitude.toString()
                    onFeedback("Koordinat lokasi saat ini diambil.")
                }.onFailure {
                    onFeedback(it.localizedMessage ?: "Gagal mengambil GPS.")
                }
            }
        }
    }

    // Policy toggles
    var reqGeofence by remember(organization.requireGeofence) { mutableStateOf(organization.requireGeofence) }
    var reqQr by remember(organization.requireDynamicQr) { mutableStateOf(organization.requireDynamicQr) }
    var reqSelfie by remember(organization.requireSelfie) { mutableStateOf(organization.requireSelfie) }

    // Plan purchase confirmation
    var upgradeTargetPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var paymentMethod by remember { mutableStateOf("Virtual Account BCA") }

    if (showAddMemberDialog) {
        AddRosterMemberDialog(
            isEdu = isEdu,
            units = config.units,
            schedules = config.schedules,
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, code, memRole, subType, unit, sch, email, phone ->
                onAddRosterMember(name, code, memRole, subType, unit, sch, email, phone)
                showAddMemberDialog = false
            }
        )
    }

    if (editingMember != null) {
        EditMemberAssignmentDialog(
            member = editingMember!!,
            isAdmin = isAdmin,
            isEdu = isEdu,
            units = config.units,
            schedules = config.schedules,
            onDismiss = { editingMember = null },
            onConfirm = { newRole, unit, sch, parentUid ->
                onUpdateMemberAssignment(editingMember!!, newRole, unit, sch, parentUid)
                editingMember = null
            }
        )
    }

    if (upgradeTargetPlan != null) {
        val plan = upgradeTargetPlan!!
        AlertDialog(
            onDismissRequest = { upgradeTargetPlan = null },
            title = { Text("Aktifkan Paket ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Biaya Langganan: ${formatRupiah(plan.monthlyPriceIdr)}/bulan")
                    Text("Kapasitas Maksimal: ${plan.maxUsers} anggota")
                    Text("Pilih Metode Pembayaran:", style = MaterialTheme.typography.labelLarge)
                    listOf("Virtual Account BCA", "QRIS Otomatis", "Transfer Bank Mandiri").forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpgradePlan(plan, paymentMethod)
                        upgradeTargetPlan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White)
                ) {
                    Text("Konfirmasi & Bayar")
                }
            },
            dismissButton = {
                TextButton(onClick = { upgradeTargetPlan = null }) { Text("Batal") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Manajemen Organisasi", style = MaterialTheme.typography.headlineMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabLabels.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, tabLabels.size),
                            modifier = Modifier.testTag("manage_tab_$idx")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = memberSearch,
                            onValueChange = { memberSearch = it },
                            label = { Text("Cari nama atau kode...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAdmin) {
                            Button(
                                onClick = { showAddMemberDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("add_member_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Anggota")
                            }
                        }
                    }
                }
                val filteredMembers = members.filter {
                    memberSearch.isBlank() ||
                        it.fullName.contains(memberSearch, ignoreCase = true) ||
                        it.memberCode.contains(memberSearch, ignoreCase = true)
                }
                if (filteredMembers.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.PersonAdd,
                            title = "Belum Ada Anggota Cocok",
                            description = "Tambahkan anggota baru atau bagikan kode gabung ${organization.joinCode}."
                        )
                    }
                } else {
                    items(filteredMembers, key = { it.id }) { mem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mem.fullName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${mem.memberCode} • ${OrgRole.fromRaw(mem.role).label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HadirinBlue
                                    )
                                    Text(
                                        text = "${mem.unitName ?: "Belum ada unit"} • ${mem.scheduleName ?: "Jadwal standar"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!mem.parentUid.isNullOrBlank()) {
                                        Text(
                                            text = "Wali terhubung: ${mem.parentUid}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = HadirinSuccess
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { editingMember = mem },
                                    modifier = Modifier.testTag("edit_member_${mem.id}")
                                ) {
                                    Text("Atur")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isEdu) "Tambah Kelas / Jurusan" else "Tambah Departemen / Divisi", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = unitName,
                                onValueChange = { unitName = it },
                                label = { Text(if (isEdu) "Nama Kelas (Contoh: XI-IPA-2)" else "Nama Departemen (Contoh: Keuangan)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_unit_name")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = unitCode,
                                    onValueChange = { unitCode = it },
                                    label = { Text("Kode") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = unitSupervisor,
                                    onValueChange = { unitSupervisor = it },
                                    label = { Text(if (isEdu) "Wali Kelas" else "Kepala Divisi") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                            Button(
                                onClick = {
                                    onAddUnit(if (isEdu) "CLASS" else "DEPARTMENT", unitName, unitCode, unitSupervisor)
                                    unitName = ""
                                    unitCode = ""
                                    unitSupervisor = ""
                                },
                                enabled = !isBusy && unitName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_unit_submit")
                            ) {
                                Icon(Icons.Default.Apartment, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Unit")
                            }
                        }
                    }
                }

                items(config.units, key = { it.id }) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${u.name} (${u.code})", style = MaterialTheme.typography.titleSmall)
                                Text("Penanggung jawab: ${u.supervisorName.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            StatusBadge(label = u.kind, statusKey = "INFO")
                        }
                    }
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isEdu) "Tambah Jadwal Sekolah" else "Tambah Shift Kerja", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = schName,
                                onValueChange = { schName = it },
                                label = { Text("Nama Jadwal / Shift") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_schedule_name")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = schStart,
                                    onValueChange = { schStart = it },
                                    label = { Text("Masuk (HH:mm)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = schEnd,
                                    onValueChange = { schEnd = it },
                                    label = { Text("Pulang (HH:mm)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = schTolerance,
                                    onValueChange = { schTolerance = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tol (mnt)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                            Button(
                                onClick = {
                                    onAddSchedule(schName, schStart, schEnd, schTolerance.toLongOrNull() ?: 15L, "Senin - Jumat")
                                    schName = ""
                                },
                                enabled = !isBusy && schName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinNavy, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_schedule_submit")
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Jadwal")
                            }
                        }
                    }
                }

                items(config.schedules, key = { it.id }) { s ->
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
                            Column {
                                Text(s.name, style = MaterialTheme.typography.titleSmall)
                                Text("${s.startTime} - ${s.endTime} • Toleransi ${s.lateToleranceMinutes} mnt", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(s.workDaysLabel, style = MaterialTheme.typography.labelSmall, color = HadirinBlue)
                        }
                    }
                }
            }

            2 -> {
                // Dynamic QR Display & Rotation
                item {
                    val qrBitmap = remember(config.activeQrToken) {
                        QrCodeService.generateQrBitmap(config.activeQrToken, 440)
                    }
                    val remainingSec = ((config.qrExpiresAtMillis - nowMillis) / 1000L).coerceAtLeast(0L)
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = HadirinNavy)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("QR Code Presensi Dinamis", style = MaterialTheme.typography.titleMedium)
                                }
                                StatusBadge(
                                    label = if (remainingSec > 0L) "Aktif ${remainingSec}d" else "Kedaluwarsa",
                                    statusKey = if (remainingSec > 0L) "ACTIVE" else "EXPIRED"
                                )
                            }
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code Dinamis Organisasi",
                                    modifier = Modifier.size(210.dp)
                                )
                            }
                            Text(
                                text = config.activeQrToken.ifBlank { "Belum ada token QR" },
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Perbarui otomatis tiap 60 detik", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = autoRotateQr, onCheckedChange = { autoRotateQr = it })
                            }
                            Button(
                                onClick = {
                                    val locLabel = config.locations.firstOrNull()?.name ?: organization.name
                                    onRotateDynamicQr(locLabel)
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("rotate_qr_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Perbarui QR Sekarang")
                            }
                        }
                    }
                }

                // Attendance verification policy (Org Admin)
                if (isAdmin) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Aturan Metode Verifikasi", style = MaterialTheme.typography.titleMedium)
                                PolicyRow("Wajib GPS Geofencing", reqGeofence) { reqGeofence = it }
                                PolicyRow("Aktifkan Validasi QR Dinamis", reqQr) { reqQr = it }
                                PolicyRow("Aktifkan Bukti Selfie Kehadiran", reqSelfie) { reqSelfie = it }
                                Button(
                                    onClick = { onUpdatePolicies(reqGeofence, reqQr, reqSelfie) },
                                    enabled = !isBusy,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_policies_button")
                                ) {
                                    Text("Simpan Kebijakan Presensi")
                                }
                            }
                        }
                    }
                }

                // Add GPS Geofence Location
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tambah Lokasi GPS Geofencing", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = locName,
                                onValueChange = { locName = it },
                                label = { Text("Nama Titik Lokasi (Contoh: Kampus Utama)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_location_name")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = locLat,
                                    onValueChange = { locLat = it },
                                    label = { Text("Latitude") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = locLon,
                                    onValueChange = { locLon = it },
                                    label = { Text("Longitude") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = locRadius,
                                    onValueChange = { locRadius = it.filter { c -> c.isDigit() } },
                                    label = { Text("Radius (m)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            scope.launch {
                                                LocationService.fetchCurrentLocation(context).onSuccess { c ->
                                                    locLat = c.latitude.toString()
                                                    locLon = c.longitude.toString()
                                                    onFeedback("Koordinat GPS diperbarui.")
                                                }
                                            }
                                        } else {
                                            gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    },
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ambil GPS")
                                }
                            }
                            Button(
                                onClick = {
                                    val lat = locLat.toDoubleOrNull() ?: -6.2
                                    val lon = locLon.toDoubleOrNull() ?: 106.816666
                                    val rad = locRadius.toLongOrNull() ?: 150L
                                    onAddLocation(locName, lat, lon, rad)
                                    locName = ""
                                },
                                enabled = !isBusy && locName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_location_submit")
                            ) {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Titik Lokasi")
                            }
                        }
                    }
                }

                items(config.locations, key = { it.id }) { loc ->
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
                                Text(loc.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${loc.latitude}, ${loc.longitude} • Radius ${loc.radiusMeters} m",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(label = if (loc.active) "AKTIF" else "NONAKTIF", statusKey = if (loc.active) "ACTIVE" else "INACTIVE")
                        }
                    }
                }
            }

            3 -> {
                // Subscription Plans & Invoices (Admin)
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = HadirinNavy)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Langganan Organisasi Saat Ini", style = MaterialTheme.typography.labelMedium, color = Color(0xFF93C5FD))
                            Text(
                                text = "Paket ${organization.subscriptionPlanId} (${organization.subscriptionStatus})",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Penggunaan Anggota: ${members.size} / ${organization.memberLimit} pengguna",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                items(plans, key = { it.id }) { plan ->
                    val isCurrent = plan.id == organization.subscriptionPlanId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) HadirinBlue else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (plan.monthlyPriceIdr == 0L) "Gratis" else "${formatRupiah(plan.monthlyPriceIdr)}/bln",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HadirinBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("Maksimal ${plan.maxUsers} pengguna aktif", style = MaterialTheme.typography.labelMedium)
                            plan.features.forEach { f ->
                                Text("• $f", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { upgradeTargetPlan = plan },
                                enabled = !isBusy && !isCurrent,
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("select_plan_${plan.id}")
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCurrent) "Paket Aktif" else "Pilih Paket ${plan.name}")
                            }
                        }
                    }
                }

                item {
                    Text("Riwayat Invoice & Pembayaran", style = MaterialTheme.typography.titleMedium)
                }
                items(transactions, key = { it.id }) { tx ->
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
                            Column {
                                Text("${tx.invoiceNumber} • ${tx.planName}", style = MaterialTheme.typography.titleSmall)
                                Text("${tx.paymentMethod} • ${formatRupiah(tx.amountIdr)}", style = MaterialTheme.typography.bodySmall)
                            }
                            StatusBadge(label = tx.status, statusKey = tx.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AddRosterMemberDialog(
    isEdu: Boolean,
    units: List<OrgUnit>,
    schedules: List<WorkSchedule>,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        memberCode: String,
        memberRole: OrgRole,
        subType: MemberSubType,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        email: String?,
        phone: String?
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var memberCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(OrgRole.MEMBER) }
    var selectedSubType by remember { mutableStateOf(if (isEdu) MemberSubType.STUDENT else MemberSubType.EMPLOYEE) }
    var selectedUnit by remember { mutableStateOf(units.firstOrNull()) }
    var selectedSchedule by remember { mutableStateOf(schedules.firstOrNull()) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Anggota Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memberCode,
                    onValueChange = { memberCode = it },
                    label = { Text(if (isEdu) "NIS / NIP" else "NIK / ID Karyawan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Peran:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(OrgRole.MEMBER, OrgRole.SUPERVISOR, OrgRole.PARENT)) { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = {
                                selectedRole = r
                                selectedSubType = when {
                                    r == OrgRole.PARENT -> MemberSubType.PARENT
                                    r == OrgRole.SUPERVISOR && isEdu -> MemberSubType.TEACHER
                                    isEdu -> MemberSubType.STUDENT
                                    else -> MemberSubType.EMPLOYEE
                                }
                            },
                            label = { Text(r.label) }
                        )
                    }
                }
                if (units.isNotEmpty()) {
                    Text("Unit / Kelas:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(units) { u ->
                            FilterChip(
                                selected = selectedUnit?.id == u.id,
                                onClick = { selectedUnit = u },
                                label = { Text(u.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(fullName, memberCode, selectedRole, selectedSubType, selectedUnit, selectedSchedule, email, phone)
                },
                enabled = fullName.trim().isNotEmpty()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun EditMemberAssignmentDialog(
    member: OrgMember,
    isAdmin: Boolean,
    isEdu: Boolean,
    units: List<OrgUnit>,
    schedules: List<WorkSchedule>,
    onDismiss: () -> Unit,
    onConfirm: (OrgRole, OrgUnit?, WorkSchedule?, String?) -> Unit
) {
    var selectedRole by remember { mutableStateOf(OrgRole.fromRaw(member.role)) }
    var selectedUnit by remember { mutableStateOf(units.firstOrNull { it.id == member.unitId } ?: units.firstOrNull()) }
    var selectedSchedule by remember { mutableStateOf(schedules.firstOrNull { it.id == member.scheduleId } ?: schedules.firstOrNull()) }
    var parentUid by remember { mutableStateOf(member.parentUid ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur ${member.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    Text("Peran Akses:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(OrgRole.entries) { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r.label) }
                            )
                        }
                    }
                }
                if (units.isNotEmpty()) {
                    Text("Unit / Kelas:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(units) { u ->
                            FilterChip(
                                selected = selectedUnit?.id == u.id,
                                onClick = { selectedUnit = u },
                                label = { Text(u.name) }
                            )
                        }
                    }
                }
                if (schedules.isNotEmpty()) {
                    Text("Jadwal / Shift:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(schedules) { s ->
                            FilterChip(
                                selected = selectedSchedule?.id == s.id,
                                onClick = { selectedSchedule = s },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                if (isEdu) {
                    OutlinedTextField(
                        value = parentUid,
                        onValueChange = { parentUid = it },
                        label = { Text("ID Wali Murid (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedRole, selectedUnit, selectedSchedule, parentUid.ifBlank { null })
                }
            ) {
                Text("Perbarui")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
