package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.ApprovalStatus
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.BillingTransaction
import com.example.data.model.LeaveOvertimeRequest
import com.example.data.model.MemberSubType
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.OrgUnit
import com.example.data.model.Organization
import com.example.data.model.RequestType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.WorkSchedule
import com.example.data.model.WorkspaceConfig
import com.example.data.repository.WorkspaceRepository
import com.example.data.repository.unwrapFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val repository: WorkspaceRepository,
    private val organizationId: String,
    private val currentUserId: String,
    private val activeRole: OrgRole
) : ViewModel() {

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val organization: StateFlow<UiState<Organization?>> = repository.observeOrganization(organizationId)
        .map<Organization?, UiState<Organization?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).unwrapFirestoreMessage("Gagal memuat organisasi.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val workspaceConfig: StateFlow<WorkspaceConfig> = repository.observeWorkspaceConfig(organizationId)
        .map { it ?: WorkspaceConfig(organizationId = organizationId) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(WorkspaceConfig(organizationId = organizationId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WorkspaceConfig(organizationId = organizationId))

    val currentMember: StateFlow<OrgMember?> = repository.observeCurrentMember(organizationId, currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val members: StateFlow<List<OrgMember>> = repository.observeMembers(organizationId, activeRole, currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val attendances: StateFlow<List<AttendanceRecord>> = repository.observeAttendances(organizationId, activeRole, currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val requests: StateFlow<List<LeaveOvertimeRequest>> = repository.observeRequests(organizationId, activeRole, currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(organizationId, activeRole, currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = if (activeRole == OrgRole.ORG_ADMIN || activeRole == OrgRole.SUPERVISOR) {
        repository.observeAuditLogs(organizationId, currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val transactions: StateFlow<List<BillingTransaction>> = if (activeRole == OrgRole.ORG_ADMIN) {
        repository.observeOrgTransactions(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun performCheckIn(
        org: Organization,
        member: OrgMember,
        schedule: WorkSchedule?,
        method: AttendanceMethod,
        latitude: Double?,
        longitude: Double?,
        distanceMeters: Long,
        withinGeofence: Boolean,
        qrTokenUsed: String?,
        selfiePath: String?,
        faceConsentGranted: Boolean,
        onSuccessRecord: (AttendanceRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.checkIn(
                org = org,
                member = member,
                schedule = schedule,
                method = method,
                latitude = latitude,
                longitude = longitude,
                distanceMeters = distanceMeters,
                withinGeofence = withinGeofence,
                qrTokenUsed = qrTokenUsed,
                selfiePath = selfiePath,
                faceConsentGranted = faceConsentGranted
            )
            _isActionLoading.value = false
            res.onSuccess { rec ->
                val statusMsg = if (rec.lateMinutes > 0) "Terlambat ${rec.lateMinutes} mnt" else "Tepat waktu"
                _actionMessage.value = "Presensi masuk berhasil (${rec.checkInTimeText} • $statusMsg)"
                onSuccessRecord(rec)
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Presensi masuk gagal.")
            }
        }
    }

    fun performCheckOut(
        org: Organization,
        member: OrgMember,
        todayRecord: AttendanceRecord,
        schedule: WorkSchedule?,
        method: AttendanceMethod,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.checkOut(org, member, todayRecord, schedule, method)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Presensi pulang berhasil dicatat."
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Presensi pulang gagal.")
            }
        }
    }

    fun submitRequest(
        org: Organization,
        member: OrgMember,
        type: RequestType,
        startDate: String,
        endDate: String,
        durationHours: Long,
        reason: String
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.submitRequest(org, member, type, startDate, endDate, durationHours, reason)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Pengajuan ${type.label} terkirim untuk verifikasi."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal mengirim pengajuan.")
            }
        }
    }

    fun reviewRequest(
        org: Organization,
        reviewerName: String,
        request: LeaveOvertimeRequest,
        decision: ApprovalStatus,
        note: String
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.reviewRequest(org, reviewerName, request, decision, note)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Pengajuan ${request.memberName} ${decision.label.lowercase()}."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal memproses persetujuan.")
            }
        }
    }

    fun addUnit(kind: String, name: String, code: String, supervisor: String) {
        val cfg = workspaceConfig.value
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.addUnit(organizationId, cfg, kind, name, code, supervisor)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "$name berhasil ditambahkan."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal menambah unit.")
            }
        }
    }

    fun addSchedule(name: String, start: String, end: String, tolerance: Long, workDays: String) {
        val cfg = workspaceConfig.value
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.addSchedule(organizationId, cfg, name, start, end, tolerance, workDays)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Jadwal '$name' disimpan."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal menyimpan jadwal.")
            }
        }
    }

    fun addLocation(name: String, lat: Double, lon: Double, radius: Long) {
        val cfg = workspaceConfig.value
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.addLocation(organizationId, cfg, name, lat, lon, radius)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Lokasi '$name' (radius ${radius}m) disimpan."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal menyimpan lokasi.")
            }
        }
    }

    fun rotateDynamicQr(locationName: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.rotateDynamicQr(organizationId, locationName)
            _isActionLoading.value = false
            res.onSuccess { token ->
                _actionMessage.value = "QR Dinamis diperbarui ($token)."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal memperbarui QR Code.")
            }
        }
    }

    fun updatePolicies(requireGeofence: Boolean, requireDynamicQr: Boolean, requireSelfie: Boolean) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.updateOrgAttendancePolicies(organizationId, requireGeofence, requireDynamicQr, requireSelfie)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Aturan verifikasi presensi disimpan."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal menyimpan aturan.")
            }
        }
    }

    fun addRosterMember(
        org: Organization,
        fullName: String,
        memberCode: String,
        role: OrgRole,
        subType: MemberSubType,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        email: String?,
        phone: String?
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.addRosterMember(org, fullName, memberCode, role, subType, unit, schedule, email, phone)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Anggota '$fullName' ditambahkan."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal menambahkan anggota.")
            }
        }
    }

    fun updateMemberAssignment(
        org: Organization,
        member: OrgMember,
        newRole: OrgRole,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        parentUid: String?
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.updateMemberAssignment(org, member, newRole, unit, schedule, parentUid)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Penugasan ${member.fullName} diperbarui."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal memperbarui anggota.")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(organizationId, notificationId)
        }
    }

    fun upgradePlan(org: Organization, actorName: String, plan: SubscriptionPlan, paymentMethod: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.upgradeOrganizationPlan(org, actorName, plan, paymentMethod)
            _isActionLoading.value = false
            res.onSuccess { invoice ->
                _actionMessage.value = "Paket ${plan.name} aktif! Invoice: $invoice"
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal memproses langganan.")
            }
        }
    }

    fun seedDemoData(org: Organization) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = repository.seedDemoWorkspaceData(org, currentUserId)
            _isActionLoading.value = false
            res.onSuccess {
                _actionMessage.value = "Data uji coba organisasi berhasil dimuat."
            }.onFailure {
                _actionMessage.value = it.unwrapFirestoreMessage("Gagal memuat data uji coba.")
            }
        }
    }
}
