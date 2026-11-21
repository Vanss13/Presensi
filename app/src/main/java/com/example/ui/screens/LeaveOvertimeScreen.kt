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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ApprovalStatus
import com.example.data.model.LeaveOvertimeRequest
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.RequestType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinDanger
import com.example.ui.theme.HadirinSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaveOvertimeScreen(
    organization: Organization,
    member: OrgMember?,
    role: OrgRole,
    requests: List<LeaveOvertimeRequest>,
    isBusy: Boolean,
    onSubmitRequest: (
        type: RequestType,
        startDate: String,
        endDate: String,
        durationHours: Long,
        reason: String
    ) -> Unit,
    onReviewRequest: (
        request: LeaveOvertimeRequest,
        decision: ApprovalStatus,
        note: String
    ) -> Unit
) {
    val isStaff = role == OrgRole.ORG_ADMIN || role == OrgRole.SUPERVISOR
    val isEdu = OrganizationType.fromRaw(organization.type) == OrganizationType.EDU
    var selectedTab by remember { mutableIntStateOf(if (isStaff) 1 else 0) }

    val availableTypes = remember(isEdu) {
        if (isEdu) listOf(RequestType.IZIN, RequestType.SAKIT, RequestType.KOREKSI)
        else listOf(RequestType.CUTI, RequestType.SAKIT, RequestType.IZIN, RequestType.LEMBUR, RequestType.KOREKSI)
    }

    val defaultDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var reqType by remember(availableTypes) { mutableStateOf(availableTypes.first()) }
    var startDate by remember { mutableStateOf(defaultDate) }
    var endDate by remember { mutableStateOf(defaultDate) }
    var hoursText by remember { mutableStateOf("8") }
    var reasonText by remember { mutableStateOf("") }

    var reviewTarget by remember { mutableStateOf<Pair<LeaveOvertimeRequest, ApprovalStatus>?>(null) }
    var reviewNote by remember { mutableStateOf("") }

    if (reviewTarget != null) {
        val (targetReq, decision) = reviewTarget!!
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = { Text("${decision.label} Pengajuan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${targetReq.memberName} • ${RequestType.fromRaw(targetReq.type).label} (${targetReq.startDate})")
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Catatan Supervisor / HRD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewRequest(targetReq, decision, reviewNote)
                        reviewNote = ""
                        reviewTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (decision == ApprovalStatus.APPROVED) HadirinSuccess else HadirinDanger,
                        contentColor = Color.White
                    )
                ) {
                    Text("Konfirmasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) {
                    Text("Batal")
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
                Text(
                    text = if (isEdu) "Izin & Koreksi Kehadiran" else "Cuti, Izin & Lembur",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (isStaff) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("tab_submit_leave")
                        ) {
                            Text("Ajukan")
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("tab_approval_queue")
                        ) {
                            Text("Persetujuan (${requests.count { it.approvalStatus == ApprovalStatus.PENDING.name }})")
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Formulir Pengajuan", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableTypes) { item ->
                                FilterChip(
                                    selected = reqType == item,
                                    onClick = { reqType = item },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("leave_type_${item.name}")
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Tanggal Mulai") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("Tanggal Selesai") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (reqType == RequestType.LEMBUR || reqType == RequestType.KOREKSI) {
                            OutlinedTextField(
                                value = hoursText,
                                onValueChange = { hoursText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Durasi (Jam)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Keterangan / Alasan Lengkap") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_leave_reason")
                        )
                        Button(
                            onClick = {
                                onSubmitRequest(
                                    reqType,
                                    startDate,
                                    endDate,
                                    hoursText.toLongOrNull() ?: 8L,
                                    reasonText
                                )
                                reasonText = ""
                            },
                            enabled = !isBusy && member != null && reasonText.trim().length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_leave_button")
                        ) {
                            Text("Kirim Pengajuan ${reqType.label}")
                        }
                    }
                }
            }
        }

        if (requests.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.EventNote,
                    title = "Belum Ada Pengajuan",
                    description = "Pengajuan cuti, izin, sakit, atau lembur akan tampil di daftar ini."
                )
            }
        } else {
            items(requests, key = { it.id }) { req ->
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
                                Text(
                                    text = "${req.memberName} • ${RequestType.fromRaw(req.type).label}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${req.startDate} s/d ${req.endDate} (${req.durationHours} jam)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                label = ApprovalStatus.fromRaw(req.approvalStatus).label,
                                statusKey = req.approvalStatus
                            )
                        }
                        Text(req.reason, style = MaterialTheme.typography.bodyMedium)
                        if (!req.reviewNote.isNullOrBlank()) {
                            Text(
                                text = "Catatan (${req.reviewedByName ?: "Supervisor"}): ${req.reviewNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = HadirinBlue
                            )
                        }
                        if (isStaff && req.approvalStatus == ApprovalStatus.PENDING.name) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { reviewTarget = req to ApprovalStatus.REJECTED },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_request_${req.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = HadirinDanger)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tolak", color = HadirinDanger)
                                }
                                Button(
                                    onClick = { reviewTarget = req to ApprovalStatus.APPROVED },
                                    colors = ButtonDefaults.buttonColors(containerColor = HadirinSuccess, contentColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_request_${req.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Setujui")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
