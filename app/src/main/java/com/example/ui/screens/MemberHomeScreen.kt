package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceRecord
import com.example.data.model.OrgMember
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.UserProfile
import com.example.data.model.WorkspaceConfig
import com.example.service.GpsCoordinate
import com.example.service.LocationService
import com.example.service.NotificationService
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinDanger
import com.example.ui.theme.HadirinDangerSoft
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.HadirinSuccessSoft
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemberHomeScreen(
    userProfile: UserProfile?,
    organization: Organization,
    member: OrgMember?,
    config: WorkspaceConfig,
    attendances: List<AttendanceRecord>,
    isBusy: Boolean,
    onCheckIn: (
        method: AttendanceMethod,
        latitude: Double?,
        longitude: Double?,
        distanceMeters: Long,
        withinGeofence: Boolean,
        qrTokenUsed: String?,
        selfiePath: String?,
        faceConsentGranted: Boolean
    ) -> Unit,
    onCheckOut: (todayRecord: AttendanceRecord, method: AttendanceMethod) -> Unit,
    onNotifyLocalMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val clockText = remember(nowMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID")).format(Date(nowMillis))
    }
    val todayKey = remember(nowMillis) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
    }

    val todayRecord = remember(attendances, todayKey, userProfile?.userId) {
        attendances.firstOrNull { it.dateKey == todayKey && it.userId == userProfile?.userId }
    }

    val assignedSchedule = remember(config.schedules, member?.scheduleId) {
        config.schedules.firstOrNull { it.id == member?.scheduleId } ?: config.schedules.firstOrNull()
    }
    val activeLocation = remember(config.locations) {
        config.locations.firstOrNull { it.active } ?: config.locations.firstOrNull()
    }

    var selectedMethod by remember { mutableStateOf(AttendanceMethod.GPS) }
    var currentGps by remember { mutableStateOf<GpsCoordinate?>(null) }
    var gpsStatusNote by remember { mutableStateOf("Tekan periksa GPS untuk mengukur jarak lokasi.") }
    var isLocating by remember { mutableStateOf(false) }

    var showQrDialog by remember { mutableStateOf(false) }
    var showSelfieDialog by remember { mutableStateOf(false) }
    var pendingActionIsCheckOut by remember { mutableStateOf(false) }

    val distanceMeters = remember(currentGps, activeLocation) {
        val gps = currentGps
        val loc = activeLocation
        if (gps != null && loc != null) {
            LocationService.haversineDistanceMeters(gps.latitude, gps.longitude, loc.latitude, loc.longitude)
        } else 0L
    }
    val withinGeofence = remember(currentGps, activeLocation, distanceMeters) {
        val loc = activeLocation ?: return@remember true
        if (currentGps == null) false else distanceMeters <= loc.radiusMeters
    }

    fun fetchGpsNow() {
        scope.launch {
            isLocating = true
            val res = LocationService.fetchCurrentLocation(context)
            isLocating = false
            res.onSuccess { coord ->
                currentGps = coord
                gpsStatusNote = if (activeLocation != null) {
                    val dist = LocationService.haversineDistanceMeters(coord.latitude, coord.longitude, activeLocation.latitude, activeLocation.longitude)
                    if (dist <= activeLocation.radiusMeters) {
                        "Valid di area ${activeLocation.name} (${dist} m / batas ${activeLocation.radiusMeters} m)"
                    } else {
                        "Di luar area ${activeLocation.name} (${dist} m / batas ${activeLocation.radiusMeters} m)"
                    }
                } else {
                    "Koordinat GPS terdeteksi (${String.format(Locale.US, "%.5f, %.5f", coord.latitude, coord.longitude)})"
                }
            }.onFailure { err ->
                gpsStatusNote = err.localizedMessage ?: "Gagal mengambil koordinat GPS."
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsNow()
        } else {
            gpsStatusNote = "Izin lokasi diperlukan untuk verifikasi GPS Geofencing."
        }
    }

    LaunchedEffect(Unit) {
        val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLoc) {
            fetchGpsNow()
        }
    }

    if (showQrDialog) {
        QrScannerDialog(
            onQrScanned = { scannedToken ->
                showQrDialog = false
                val now = System.currentTimeMillis()
                val validToken = config.activeQrToken
                if (validToken.isNotBlank() && scannedToken != validToken) {
                    onNotifyLocalMessage("Token QR tidak cocok dengan QR aktif organisasi.")
                    return@QrScannerDialog
                }
                if (config.qrExpiresAtMillis in 1 until now) {
                    onNotifyLocalMessage("QR Code sudah kedaluwarsa. Minta Admin memperbarui QR.")
                    return@QrScannerDialog
                }
                if (pendingActionIsCheckOut && todayRecord != null) {
                    onCheckOut(todayRecord, AttendanceMethod.QR)
                } else {
                    onCheckIn(
                        AttendanceMethod.QR,
                        currentGps?.latitude,
                        currentGps?.longitude,
                        distanceMeters,
                        withinGeofence || !organization.requireGeofence,
                        scannedToken,
                        null,
                        false
                    )
                    NotificationService.showLocalNotification(context, "Presensi QR Berhasil", "Tercatat masuk di ${organization.name} pukul $clockText.")
                }
            },
            onDismiss = { showQrDialog = false }
        )
    }

    if (showSelfieDialog) {
        SelfieCaptureDialog(
            onSelfieCaptured = { path, consent ->
                showSelfieDialog = false
                if (pendingActionIsCheckOut && todayRecord != null) {
                    onCheckOut(todayRecord, AttendanceMethod.SELFIE)
                } else {
                    onCheckIn(
                        AttendanceMethod.SELFIE,
                        currentGps?.latitude,
                        currentGps?.longitude,
                        distanceMeters,
                        withinGeofence || !organization.requireGeofence,
                        null,
                        path,
                        consent
                    )
                    NotificationService.showLocalNotification(context, "Selfie Presensi Berhasil", "Bukti foto tersimpan pada $clockText.")
                }
            },
            onDismiss = { showSelfieDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                // Hero Clock & Member Identity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(HadirinNavy, Color(0xFF1E40AF)))
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (!userProfile?.photoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = userProfile?.photoUrl,
                                            contentDescription = "Foto Profil",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = (member?.fullName ?: userProfile?.displayName ?: "H").take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = member?.fullName ?: userProfile?.displayName ?: "Pengguna",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${member?.memberCode ?: "ID"} • ${member?.unitName ?: OrganizationType.fromRaw(organization.type).moduleName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                StatusBadge(
                                    label = if (todayRecord == null) "BELUM MASUK" else if (todayRecord.checkOutTimeText == null) "SUDAH MASUK" else "SELESAI",
                                    statusKey = if (todayRecord == null) "PENDING" else "HADIR"
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = clockText,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Masuk: ${assignedSchedule?.startTime ?: "08:00"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pulang: ${assignedSchedule?.endTime ?: "17:00"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verification Method & GPS Geofence Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Metode Verifikasi Presensi", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethod == AttendanceMethod.GPS,
                            onClick = { selectedMethod = AttendanceMethod.GPS },
                            label = { Text("GPS") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("method_chip_gps")
                        )
                        FilterChip(
                            selected = selectedMethod == AttendanceMethod.QR,
                            onClick = { selectedMethod = AttendanceMethod.QR },
                            label = { Text("QR Code") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("method_chip_qr")
                        )
                        FilterChip(
                            selected = selectedMethod == AttendanceMethod.SELFIE,
                            onClick = { selectedMethod = AttendanceMethod.SELFIE },
                            label = { Text("Selfie") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("method_chip_selfie")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentGps != null && withinGeofence) HadirinSuccessSoft else if (currentGps != null) HadirinDangerSoft else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gpsStatusNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentGps != null && withinGeofence) HadirinSuccess else if (currentGps != null) HadirinDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (currentGps != null) {
                                    Text(
                                        text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", currentGps!!.latitude, currentGps!!.longitude)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasLoc) fetchGpsNow()
                                    else locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                },
                                enabled = !isLocating,
                                modifier = Modifier.testTag("refresh_gps_button")
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Perbarui GPS", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary One-Handed Check-In / Check-Out Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (member == null) {
                            onNotifyLocalMessage("Data keanggotaan belum dimuat.")
                            return@Button
                        }
                        when (selectedMethod) {
                            AttendanceMethod.GPS -> {
                                val effectiveMethod = AttendanceMethod.GPS
                                if (organization.requireGeofence && currentGps == null) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                    onNotifyLocalMessage("Aktifkan izin lokasi untuk presensi GPS Geofencing.")
                                    return@Button
                                }
                                if (organization.requireGeofence && activeLocation != null && !withinGeofence) {
                                    onNotifyLocalMessage("Anda berada di luar radius presensi (${distanceMeters}m > ${activeLocation.radiusMeters}m).")
                                    return@Button
                                }
                                onCheckIn(
                                    effectiveMethod,
                                    currentGps?.latitude,
                                    currentGps?.longitude,
                                    distanceMeters,
                                    withinGeofence,
                                    null,
                                    null,
                                    false
                                )
                                NotificationService.showLocalNotification(context, "Presensi Masuk Tercatat", "Anda masuk pukul $clockText di ${organization.name}.")
                            }
                            AttendanceMethod.QR -> {
                                pendingActionIsCheckOut = false
                                showQrDialog = true
                            }
                            AttendanceMethod.SELFIE -> {
                                pendingActionIsCheckOut = false
                                showSelfieDialog = true
                            }
                        }
                    },
                    enabled = !isBusy && todayRecord == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HadirinSuccess,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("check_in_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PRESENSI MASUK", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val rec = todayRecord ?: return@Button
                        when (selectedMethod) {
                            AttendanceMethod.GPS -> {
                                onCheckOut(rec, AttendanceMethod.GPS)
                                NotificationService.showLocalNotification(context, "Presensi Pulang Tercatat", "Anda pulang pukul $clockText.")
                            }
                            AttendanceMethod.QR -> {
                                pendingActionIsCheckOut = true
                                showQrDialog = true
                            }
                            AttendanceMethod.SELFIE -> {
                                pendingActionIsCheckOut = true
                                showSelfieDialog = true
                            }
                        }
                    },
                    enabled = !isBusy && todayRecord != null && todayRecord.checkOutTimeText == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HadirinNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("check_out_button")
                ) {
                    Text("PRESENSI PULANG", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Today's Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ringkasan Presensi Hari Ini", style = MaterialTheme.typography.titleSmall)
                    if (todayRecord == null) {
                        Text(
                            text = "Anda belum melakukan presensi masuk hari ini. Pilih metode GPS, QR Dinamis, atau Selfie di atas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Jam Masuk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(todayRecord.checkInTimeText, style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMonoFamily)
                            }
                            Column {
                                Text("Jam Pulang", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(todayRecord.checkOutTimeText ?: "--:--", style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMonoFamily)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                StatusBadge(label = todayRecord.status, statusKey = todayRecord.status)
                            }
                        }
                        if (todayRecord.lateMinutes > 0L) {
                            Text(
                                text = "Keterlambatan tercatat: ${todayRecord.lateMinutes} menit",
                                style = MaterialTheme.typography.bodySmall,
                                color = HadirinDanger
                            )
                        }
                        if (todayRecord.workMinutes > 0L) {
                            Text(
                                text = "Total durasi: ${todayRecord.workMinutes / 60}j ${todayRecord.workMinutes % 60}m (Lembur ${todayRecord.overtimeMinutes} mnt)",
                                style = MaterialTheme.typography.bodySmall,
                                color = HadirinBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
