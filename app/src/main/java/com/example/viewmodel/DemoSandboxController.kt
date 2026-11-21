package com.example.viewmodel

import android.os.Build
import com.example.data.model.AppNotification
import com.example.data.model.ApprovalStatus
import com.example.data.model.AttendanceLocation
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AuditLog
import com.example.data.model.BillingTransaction
import com.example.data.model.DefaultSubscriptionCatalog
import com.example.data.model.LeaveOvertimeRequest
import com.example.data.model.MemberSubType
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.OrgUnit
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.RequestType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.SubscriptionPlanTier
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.model.WorkSchedule
import com.example.data.model.WorkspaceConfig
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DemoOrgBundle(
    val organization: Organization,
    val config: WorkspaceConfig,
    val members: List<OrgMember>,
    val attendances: List<AttendanceRecord>,
    val requests: List<LeaveOvertimeRequest>,
    val notifications: List<AppNotification>,
    val auditLogs: List<AuditLog>
)

class DemoSandboxController {
    private val demoUid = "demo_halimawan13"
    private val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L))
    private val twoDaysAgoKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 2 * 86_400_000L))

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = demoUid,
            userId = demoUid,
            email = "halimawan13@gmail.com",
            displayName = "Halimawan (Mode Uji Coba)",
            phone = "081234567890",
            activeOrganizationId = "demo_work_1",
            superAdmin = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _activeRole = MutableStateFlow(OrgRole.ORG_ADMIN)
    val activeRole: StateFlow<OrgRole> = _activeRole.asStateFlow()

    private val _superAdminOpen = MutableStateFlow(false)
    val superAdminOpen: StateFlow<Boolean> = _superAdminOpen.asStateFlow()

    private val _onboardingOpen = MutableStateFlow(false)
    val onboardingOpen: StateFlow<Boolean> = _onboardingOpen.asStateFlow()

    private val _plans = MutableStateFlow(DefaultSubscriptionCatalog.plans)
    val plans: StateFlow<List<SubscriptionPlan>> = _plans.asStateFlow()

    private val _transactions = MutableStateFlow(
        listOf(
            BillingTransaction(
                id = "demo_tx_1",
                organizationId = "demo_work_1",
                organizationName = "PT Karya Nusantara",
                userId = demoUid,
                staffUids = listOf(demoUid),
                planId = "BUSINESS",
                planName = "Business",
                amountIdr = 399000L,
                status = "PAID",
                invoiceNumber = "INV-202611-8821A",
                paymentMethod = "Virtual Account BCA"
            ),
            BillingTransaction(
                id = "demo_tx_2",
                organizationId = "demo_edu_1",
                organizationName = "SMA Negeri 1 Nusantara",
                userId = demoUid,
                staffUids = listOf(demoUid),
                planId = "STARTER",
                planName = "Starter",
                amountIdr = 149000L,
                status = "PAID",
                invoiceNumber = "INV-202611-9014K",
                paymentMethod = "QRIS Otomatis"
            )
        )
    )
    val transactions: StateFlow<List<BillingTransaction>> = _transactions.asStateFlow()

    private val _bundles = MutableStateFlow(createInitialBundles())
    val bundles: StateFlow<Map<String, DemoOrgBundle>> = _bundles.asStateFlow()

    private val _activeOrgId = MutableStateFlow("demo_work_1")
    val activeOrgId: StateFlow<String> = _activeOrgId.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun switchRole(role: OrgRole) {
        _activeRole.value = role
        _feedback.value = "Beralih tampilan ke peran ${role.label}."
    }

    fun switchOrganization(orgId: String) {
        if (_bundles.value.containsKey(orgId)) {
            _activeOrgId.value = orgId
            _userProfile.value = _userProfile.value.copy(activeOrganizationId = orgId)
            _superAdminOpen.value = false
            _onboardingOpen.value = false
            _feedback.value = "Berpindah ke ${_bundles.value[orgId]?.organization?.name}."
        }
    }

    fun setSuperAdminOpen(open: Boolean) {
        _superAdminOpen.value = open
    }

    fun setOnboardingOpen(open: Boolean) {
        _onboardingOpen.value = open
    }

    fun getMemberships(): List<UserMembership> = _bundles.value.values.map { b ->
        UserMembership(
            id = "${demoUid}_${b.organization.id}",
            userId = demoUid,
            organizationId = b.organization.id,
            organizationName = b.organization.name,
            organizationType = b.organization.type,
            role = _activeRole.value.name,
            memberCode = if (b.organization.type == OrganizationType.EDU.name) "ADM-EDU" else "ADM-WRK"
        )
    }

    fun updateProfile(displayName: String, phone: String?) {
        val clean = displayName.trim()
        if (clean.isEmpty()) return
        _userProfile.value = _userProfile.value.copy(
            displayName = clean,
            phone = phone?.trim()?.takeIf { it.isNotBlank() }
        )
        _feedback.value = "Profil uji coba diperbarui."
    }

    fun createOrganization(
        name: String,
        type: OrganizationType,
        phone: String,
        address: String,
        adminName: String
    ) {
        val cleanName = name.trim()
        if (cleanName.length < 2) return
        val orgId = "demo_${UUID.randomUUID().toString().take(8)}"
        val prefix = if (type == OrganizationType.EDU) "EDU" else "WRK"
        val joinCode = "$prefix${UUID.randomUUID().toString().take(5).uppercase()}"
        val unit = if (type == OrganizationType.EDU) {
            OrgUnit("u_1", "CLASS", "Kelas X-A", "XA", adminName)
        } else {
            OrgUnit("u_1", "DEPARTMENT", "Operasional", "OPS", adminName)
        }
        val schedule = if (type == OrganizationType.EDU) {
            WorkSchedule("sch_1", "Jadwal Pagi", "07:00", "14:00", 15L, "Senin - Jumat")
        } else {
            WorkSchedule("sch_1", "Shift Pagi", "08:00", "17:00", 15L, "Senin - Jumat")
        }
        val org = Organization(
            id = orgId,
            name = cleanName,
            type = type.name,
            joinCode = joinCode,
            address = address.trim(),
            phone = phone.trim(),
            ownerUid = demoUid,
            staffUids = listOf(demoUid),
            subscriptionPlanId = SubscriptionPlanTier.FREE.id,
            subscriptionStatus = "TRIAL",
            memberLimit = 20L,
            requireGeofence = true
        )
        val config = WorkspaceConfig(
            id = "config",
            organizationId = orgId,
            units = listOf(unit),
            schedules = listOf(schedule),
            locations = listOf(
                AttendanceLocation("loc_1", "Lokasi Utama", -6.200000, 106.816666, 250L, true)
            ),
            activeQrToken = "HDR-${orgId.takeLast(4).uppercase()}-DEMO",
            qrExpiresAtMillis = System.currentTimeMillis() + 5 * 60_000L,
            qrLocationName = "Lokasi Utama"
        )
        val adminMember = OrgMember(
            id = demoUid,
            organizationId = orgId,
            userId = demoUid,
            staffUids = listOf(demoUid),
            fullName = adminName.ifBlank { _userProfile.value.displayName },
            email = _userProfile.value.email,
            phone = phone,
            memberCode = "ADM-001",
            role = OrgRole.ORG_ADMIN.name,
            subType = if (type == OrganizationType.EDU) MemberSubType.TEACHER.name else MemberSubType.EMPLOYEE.name,
            unitId = unit.id,
            unitName = unit.name,
            scheduleId = schedule.id,
            scheduleName = schedule.name
        )
        _bundles.value = _bundles.value + (orgId to DemoOrgBundle(
            organization = org,
            config = config,
            members = listOf(adminMember),
            attendances = emptyList(),
            requests = emptyList(),
            notifications = emptyList(),
            auditLogs = listOf(
                AuditLog(
                    id = "log_${UUID.randomUUID().toString().take(6)}",
                    organizationId = orgId,
                    actorUid = demoUid,
                    actorName = adminMember.fullName,
                    action = "ORGANISASI_DIBUAT",
                    details = "Organisasi demo $cleanName dibuat."
                )
            )
        ))
        _activeOrgId.value = orgId
        _activeRole.value = OrgRole.ORG_ADMIN
        _onboardingOpen.value = false
        _superAdminOpen.value = false
        _feedback.value = "Organisasi demo '$cleanName' berhasil dibuat!"
    }

    fun joinOrganization(joinCode: String, fullName: String, memberCode: String, joinAsParent: Boolean) {
        val code = joinCode.trim().uppercase()
        val match = _bundles.value.values.firstOrNull { it.organization.joinCode == code }
        if (match == null) {
            _feedback.value = "Kode demo tidak ditemukan. Coba WRK8821A atau EDU9014K."
            return
        }
        val orgType = OrganizationType.fromRaw(match.organization.type)
        val role = if (joinAsParent && orgType == OrganizationType.EDU) OrgRole.PARENT else OrgRole.MEMBER
        _activeOrgId.value = match.organization.id
        _activeRole.value = role
        _onboardingOpen.value = false
        _feedback.value = "Bergabung ke ${match.organization.name} sebagai ${role.label} (${fullName.ifBlank { memberCode }})."
    }

    fun performCheckIn(
        method: AttendanceMethod,
        latitude: Double?,
        longitude: Double?,
        distanceMeters: Long,
        withinGeofence: Boolean,
        qrTokenUsed: String?,
        selfiePath: String?,
        faceConsentGranted: Boolean
    ) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        if (bundle.attendances.any { it.dateKey == todayKey && it.userId == demoUid }) {
            _feedback.value = "Anda sudah melakukan presensi masuk hari ini."
            return
        }
        val member = bundle.members.firstOrNull { it.userId == demoUid } ?: return
        val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val rec = AttendanceRecord(
            id = "${demoUid}_$todayKey",
            organizationId = bundle.organization.id,
            userId = demoUid,
            staffUids = listOf(demoUid),
            memberName = member.fullName,
            memberCode = member.memberCode,
            unitName = member.unitName,
            dateKey = todayKey,
            checkInTimeText = timeText,
            checkInAt = Timestamp.now(),
            method = method.name,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            withinGeofence = withinGeofence,
            qrTokenUsed = qrTokenUsed,
            selfiePath = selfiePath,
            faceConsentGranted = faceConsentGranted,
            status = AttendanceStatus.HADIR.name,
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        val notif = AppNotification(
            id = "n_${UUID.randomUUID().toString().take(8)}",
            organizationId = bundle.organization.id,
            targetUid = demoUid,
            staffUids = listOf(demoUid),
            title = "Presensi Masuk Tercatat",
            message = "${member.fullName} berhasil masuk pukul $timeText via ${method.label}.",
            category = "ATTENDANCE"
        )
        updateCurrentBundle(
            bundle.copy(
                attendances = listOf(rec) + bundle.attendances,
                notifications = listOf(notif) + bundle.notifications
            )
        )
        _feedback.value = "Presensi masuk berhasil ($timeText • Tepat waktu)"
    }

    fun performCheckOut(todayRecord: AttendanceRecord, method: AttendanceMethod) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val updated = bundle.attendances.map {
            if (it.id == todayRecord.id) {
                it.copy(
                    checkOutTimeText = timeText,
                    checkOutMethod = method.name,
                    workMinutes = 480L,
                    overtimeMinutes = 15L
                )
            } else it
        }
        updateCurrentBundle(bundle.copy(attendances = updated))
        _feedback.value = "Presensi pulang berhasil dicatat ($timeText)."
    }

    fun submitRequest(type: RequestType, startDate: String, endDate: String, durationHours: Long, reason: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val member = bundle.members.firstOrNull { it.userId == demoUid } ?: return
        val req = LeaveOvertimeRequest(
            id = "req_${UUID.randomUUID().toString().take(8)}",
            organizationId = bundle.organization.id,
            userId = demoUid,
            staffUids = listOf(demoUid),
            memberName = member.fullName,
            unitName = member.unitName,
            type = type.name,
            startDate = startDate,
            endDate = endDate,
            durationHours = durationHours,
            reason = reason.trim(),
            approvalStatus = ApprovalStatus.PENDING.name
        )
        updateCurrentBundle(bundle.copy(requests = listOf(req) + bundle.requests))
        _feedback.value = "Pengajuan ${type.label} terkirim."
    }

    fun reviewRequest(req: LeaveOvertimeRequest, decision: ApprovalStatus, note: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val updated = bundle.requests.map {
            if (it.id == req.id) {
                it.copy(
                    approvalStatus = decision.name,
                    reviewedByUid = demoUid,
                    reviewedByName = _userProfile.value.displayName,
                    reviewNote = note.ifBlank { decision.label }
                )
            } else it
        }
        val log = AuditLog(
            id = "log_${UUID.randomUUID().toString().take(6)}",
            organizationId = bundle.organization.id,
            actorUid = demoUid,
            actorName = _userProfile.value.displayName,
            action = "VERIFIKASI_${req.type}",
            details = "${decision.label} pengajuan ${req.memberName}"
        )
        updateCurrentBundle(bundle.copy(requests = updated, auditLogs = listOf(log) + bundle.auditLogs))
        _feedback.value = "Pengajuan ${req.memberName} ${decision.label.lowercase()}."
    }

    fun addRosterMember(
        fullName: String,
        memberCode: String,
        role: OrgRole,
        subType: MemberSubType,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        email: String?,
        phone: String?
    ) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val id = "demo_m_${UUID.randomUUID().toString().take(6)}"
        val mem = OrgMember(
            id = id,
            organizationId = bundle.organization.id,
            userId = id,
            staffUids = listOf(demoUid),
            fullName = fullName.trim(),
            email = email,
            phone = phone,
            memberCode = memberCode.ifBlank { id.uppercase() },
            role = role.name,
            subType = subType.name,
            unitId = unit?.id,
            unitName = unit?.name,
            scheduleId = schedule?.id,
            scheduleName = schedule?.name
        )
        updateCurrentBundle(bundle.copy(members = bundle.members + mem))
        _feedback.value = "Anggota '${mem.fullName}' ditambahkan."
    }

    fun updateMemberAssignment(member: OrgMember, newRole: OrgRole, unit: OrgUnit?, schedule: WorkSchedule?, parentUid: String?) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val updated = bundle.members.map {
            if (it.id == member.id) {
                it.copy(
                    role = newRole.name,
                    unitId = unit?.id ?: it.unitId,
                    unitName = unit?.name ?: it.unitName,
                    scheduleId = schedule?.id ?: it.scheduleId,
                    scheduleName = schedule?.name ?: it.scheduleName,
                    parentUid = parentUid ?: it.parentUid
                )
            } else it
        }
        updateCurrentBundle(bundle.copy(members = updated))
        _feedback.value = "Penugasan ${member.fullName} diperbarui."
    }

    fun addUnit(kind: String, name: String, code: String, supervisor: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val unit = OrgUnit(
            id = "u_${UUID.randomUUID().toString().take(6)}",
            kind = kind,
            name = name.trim(),
            code = code.ifBlank { name.take(4).uppercase() },
            supervisorName = supervisor.trim()
        )
        updateCurrentBundle(bundle.copy(config = bundle.config.copy(units = bundle.config.units + unit)))
        _feedback.value = "${unit.name} ditambahkan."
    }

    fun addSchedule(name: String, start: String, end: String, tolerance: Long, workDays: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val sch = WorkSchedule(
            id = "sch_${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            startTime = start,
            endTime = end,
            lateToleranceMinutes = tolerance,
            workDaysLabel = workDays
        )
        updateCurrentBundle(bundle.copy(config = bundle.config.copy(schedules = bundle.config.schedules + sch)))
        _feedback.value = "Jadwal '${sch.name}' disimpan."
    }

    fun addLocation(name: String, lat: Double, lon: Double, radius: Long) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val loc = AttendanceLocation(
            id = "loc_${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            latitude = lat,
            longitude = lon,
            radiusMeters = radius,
            active = true
        )
        updateCurrentBundle(bundle.copy(config = bundle.config.copy(locations = bundle.config.locations + loc)))
        _feedback.value = "Lokasi '${loc.name}' disimpan."
    }

    fun rotateDynamicQr(locationName: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val token = "HDR-${bundle.organization.id.takeLast(4).uppercase()}-${UUID.randomUUID().toString().take(6).uppercase()}"
        updateCurrentBundle(
            bundle.copy(
                config = bundle.config.copy(
                    activeQrToken = token,
                    qrExpiresAtMillis = System.currentTimeMillis() + 5 * 60_000L,
                    qrLocationName = locationName
                )
            )
        )
        _feedback.value = "QR Dinamis diperbarui ($token)."
    }

    fun updatePolicies(requireGeofence: Boolean, requireDynamicQr: Boolean, requireSelfie: Boolean) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        updateCurrentBundle(
            bundle.copy(
                organization = bundle.organization.copy(
                    requireGeofence = requireGeofence,
                    requireDynamicQr = requireDynamicQr,
                    requireSelfie = requireSelfie
                )
            )
        )
        _feedback.value = "Kebijakan presensi diperbarui."
    }

    fun upgradePlan(plan: SubscriptionPlan, paymentMethod: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val invoice = "INV-DEMO-${UUID.randomUUID().toString().take(5).uppercase()}"
        val tx = BillingTransaction(
            id = "tx_${UUID.randomUUID().toString().take(6)}",
            organizationId = bundle.organization.id,
            organizationName = bundle.organization.name,
            userId = demoUid,
            staffUids = listOf(demoUid),
            planId = plan.id,
            planName = plan.name,
            amountIdr = plan.monthlyPriceIdr,
            status = "PAID",
            invoiceNumber = invoice,
            paymentMethod = paymentMethod
        )
        _transactions.value = listOf(tx) + _transactions.value
        updateCurrentBundle(
            bundle.copy(
                organization = bundle.organization.copy(
                    subscriptionPlanId = plan.id,
                    subscriptionStatus = "ACTIVE",
                    memberLimit = plan.maxUsers
                )
            )
        )
        _feedback.value = "Paket ${plan.name} aktif ($invoice)."
    }

    fun updateSubscriptionPlan(planId: String, priceIdr: Long, maxUsers: Long) {
        _plans.value = _plans.value.map {
            if (it.id == planId) it.copy(monthlyPriceIdr = priceIdr, maxUsers = maxUsers) else it
        }
        _feedback.value = "Paket $planId diperbarui."
    }

    fun markNotificationRead(notifId: String) {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val updated = bundle.notifications.map { if (it.id == notifId) it.copy(readFlag = true) else it }
        updateCurrentBundle(bundle.copy(notifications = updated))
    }

    fun seedExtraSampleData() {
        val bundle = _bundles.value[_activeOrgId.value] ?: return
        val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val extra = AttendanceRecord(
            id = "sample_${UUID.randomUUID().toString().take(5)}_$todayKey",
            organizationId = bundle.organization.id,
            userId = "demo_extra_1",
            staffUids = listOf(demoUid),
            memberName = "Citra Lestari",
            memberCode = "DEMO-999",
            unitName = bundle.config.units.firstOrNull()?.name,
            dateKey = todayKey,
            checkInTimeText = timeText,
            method = AttendanceMethod.QR.name,
            distanceMeters = 18L,
            withinGeofence = true,
            status = AttendanceStatus.HADIR.name,
            deviceInfo = "HADIRIN Demo Sandbox"
        )
        updateCurrentBundle(bundle.copy(attendances = listOf(extra) + bundle.attendances))
        _feedback.value = "Contoh data presensi baru ditambahkan."
    }

    private fun updateCurrentBundle(updated: DemoOrgBundle) {
        _bundles.value = _bundles.value + (updated.organization.id to updated)
    }

    private fun createInitialBundles(): Map<String, DemoOrgBundle> {
        val workOrg = Organization(
            id = "demo_work_1",
            name = "PT Karya Nusantara",
            type = OrganizationType.WORK.name,
            joinCode = "WRK8821A",
            address = "Jl. Sudirman Kav. 45, Jakarta Selatan",
            phone = "081234567890",
            ownerUid = demoUid,
            staffUids = listOf(demoUid),
            subscriptionPlanId = "BUSINESS",
            subscriptionStatus = "ACTIVE",
            memberLimit = 150L,
            requireGeofence = true,
            requireDynamicQr = false,
            requireSelfie = false
        )
        val workUnits = listOf(
            OrgUnit("u_ops", "DEPARTMENT", "Operasional", "OPS", "Halimawan"),
            OrgUnit("u_tech", "DEPARTMENT", "Teknologi", "TEK", "Fajar Nugroho"),
            OrgUnit("u_fin", "DIVISION", "Keuangan", "KEU", "Siti Rahma")
        )
        val workSchedules = listOf(
            WorkSchedule("sch_pagi", "Shift Pagi Reguler", "08:00", "17:00", 15L, "Senin - Jumat"),
            WorkSchedule("sch_siang", "Shift Siang", "13:00", "21:00", 15L, "Senin - Sabtu")
        )
        val workConfig = WorkspaceConfig(
            id = "config",
            organizationId = workOrg.id,
            units = workUnits,
            schedules = workSchedules,
            locations = listOf(
                AttendanceLocation("loc_hq", "Kantor Pusat Jakarta", -6.208763, 106.845599, 250L, true)
            ),
            activeQrToken = "HDR-WRK1-8821DEMO",
            qrExpiresAtMillis = System.currentTimeMillis() + 5 * 60_000L,
            qrLocationName = "Kantor Pusat Jakarta"
        )
        val workMembers = listOf(
            OrgMember(demoUid, workOrg.id, demoUid, listOf(demoUid), null, "Halimawan", "halimawan13@gmail.com", "081234567890", "ADM-001", OrgRole.ORG_ADMIN.name, MemberSubType.EMPLOYEE.name, "u_ops", "Operasional", null, "sch_pagi", "Shift Pagi Reguler"),
            OrgMember("w_1", workOrg.id, "w_1", listOf(demoUid), null, "Rina Wijaya", "rina@nusantara.id", "08129911001", "EMP-201", OrgRole.MEMBER.name, MemberSubType.EMPLOYEE.name, "u_ops", "Operasional", null, "sch_pagi", "Shift Pagi Reguler"),
            OrgMember("w_2", workOrg.id, "w_2", listOf(demoUid), null, "Fajar Nugroho", "fajar@nusantara.id", "08129911002", "EMP-202", OrgRole.SUPERVISOR.name, MemberSubType.EMPLOYEE.name, "u_tech", "Teknologi", null, "sch_pagi", "Shift Pagi Reguler"),
            OrgMember("w_3", workOrg.id, "w_3", listOf(demoUid), null, "Siti Rahma", "siti@nusantara.id", "08129911003", "EMP-203", OrgRole.MEMBER.name, MemberSubType.EMPLOYEE.name, "u_fin", "Keuangan", null, "sch_pagi", "Shift Pagi Reguler"),
            OrgMember("w_4", workOrg.id, "w_4", listOf(demoUid), null, "Bagus Saputra", "bagus@nusantara.id", "08129911004", "EMP-204", OrgRole.MEMBER.name, MemberSubType.EMPLOYEE.name, "u_tech", "Teknologi", null, "sch_siang", "Shift Siang")
        )
        val workAttendances = listOf(
            AttendanceRecord("wa_1", workOrg.id, "w_1", listOf(demoUid), null, "Rina Wijaya", "EMP-201", "Operasional", todayKey, "07:52", null, Timestamp.now(), null, AttendanceMethod.GPS.name, null, -6.2087, 106.8455, 32L, true, null, null, false, AttendanceStatus.HADIR.name, 0L, 0L, 0L, "Pixel Demo"),
            AttendanceRecord("wa_2", workOrg.id, "w_2", listOf(demoUid), null, "Fajar Nugroho", "EMP-202", "Teknologi", todayKey, "08:28", null, Timestamp.now(), null, AttendanceMethod.QR.name, null, null, null, 15L, true, "HDR-WRK1", null, false, AttendanceStatus.TERLAMBAT.name, 13L, 0L, 0L, "Samsung Demo"),
            AttendanceRecord("wa_3", workOrg.id, "w_1", listOf(demoUid), null, "Rina Wijaya", "EMP-201", "Operasional", yesterdayKey, "07:49", "17:35", Timestamp.now(), null, AttendanceMethod.GPS.name, AttendanceMethod.GPS.name, null, null, 20L, true, null, null, false, AttendanceStatus.HADIR.name, 0L, 526L, 35L, "Pixel Demo"),
            AttendanceRecord("wa_4", workOrg.id, "w_3", listOf(demoUid), null, "Siti Rahma", "EMP-203", "Keuangan", twoDaysAgoKey, "07:58", "17:02", Timestamp.now(), null, AttendanceMethod.SELFIE.name, AttendanceMethod.SELFIE.name, null, null, 12L, true, null, null, true, AttendanceStatus.HADIR.name, 0L, 484L, 2L, "Xiaomi Demo")
        )
        val workRequests = listOf(
            LeaveOvertimeRequest("wr_1", workOrg.id, "w_3", listOf(demoUid), "Siti Rahma", "Keuangan", RequestType.CUTI.name, todayKey, todayKey, 8L, "Acara wisuda keluarga di Bandung.", ApprovalStatus.PENDING.name)
        )
        val workNotifs = listOf(
            AppNotification("wn_1", workOrg.id, demoUid, listOf(demoUid), "Keterlambatan Tercatat", "Fajar Nugroho terlambat 13 menit pada $todayKey.", "LATE", false)
        )

        val eduOrg = Organization(
            id = "demo_edu_1",
            name = "SMA Negeri 1 Nusantara",
            type = OrganizationType.EDU.name,
            joinCode = "EDU9014K",
            address = "Jl. Pendidikan No. 1, Yogyakarta",
            phone = "08127770001",
            ownerUid = demoUid,
            staffUids = listOf(demoUid),
            subscriptionPlanId = "STARTER",
            subscriptionStatus = "ACTIVE",
            memberLimit = 50L,
            requireGeofence = true,
            requireDynamicQr = true,
            requireSelfie = false
        )
        val eduConfig = WorkspaceConfig(
            id = "config",
            organizationId = eduOrg.id,
            units = listOf(
                OrgUnit("u_xa", "CLASS", "X-IPA-1", "XIPA1", "Ibu Ratna"),
                OrgUnit("u_xi", "CLASS", "XI-IPS-2", "XIIPS2", "Pak Budi")
            ),
            schedules = listOf(
                WorkSchedule("sch_sek", "Jadwal Sekolah Pagi", "07:00", "14:00", 10L, "Senin - Jumat")
            ),
            locations = listOf(
                AttendanceLocation("loc_sek", "Gerbang Utama Sekolah", -7.795580, 110.369490, 200L, true)
            ),
            activeQrToken = "HDR-EDU1-9014DEMO",
            qrExpiresAtMillis = System.currentTimeMillis() + 5 * 60_000L,
            qrLocationName = "Gerbang Utama Sekolah"
        )
        val eduMembers = listOf(
            OrgMember(demoUid, eduOrg.id, demoUid, listOf(demoUid), null, "Halimawan", "halimawan13@gmail.com", "081234567890", "ADM-EDU", OrgRole.ORG_ADMIN.name, MemberSubType.TEACHER.name, "u_xa", "X-IPA-1", null, "sch_sek", "Jadwal Sekolah Pagi"),
            OrgMember("e_1", eduOrg.id, "e_1", listOf(demoUid), demoUid, "Aisyah Putri", "aisyah@sman1.sch.id", null, "SIS-101", OrgRole.MEMBER.name, MemberSubType.STUDENT.name, "u_xa", "X-IPA-1", null, "sch_sek", "Jadwal Sekolah Pagi"),
            OrgMember("e_2", eduOrg.id, "e_2", listOf(demoUid), null, "Rafi Ahmad", "rafi@sman1.sch.id", null, "SIS-102", OrgRole.MEMBER.name, MemberSubType.STUDENT.name, "u_xi", "XI-IPS-2", null, "sch_sek", "Jadwal Sekolah Pagi")
        )
        val eduAttendances = listOf(
            AttendanceRecord("ea_1", eduOrg.id, "e_1", listOf(demoUid), demoUid, "Aisyah Putri", "SIS-101", "X-IPA-1", todayKey, "06:48", "14:05", Timestamp.now(), null, AttendanceMethod.QR.name, AttendanceMethod.QR.name, null, null, 10L, true, "HDR-EDU1", null, false, AttendanceStatus.HADIR.name, 0L, 437L, 0L, "Gerbang QR"),
            AttendanceRecord("ea_2", eduOrg.id, "e_2", listOf(demoUid), null, "Rafi Ahmad", "SIS-102", "XI-IPS-2", todayKey, "07:19", null, Timestamp.now(), null, AttendanceMethod.GPS.name, null, null, null, 45L, true, null, null, false, AttendanceStatus.TERLAMBAT.name, 9L, 0L, 0L, "Android Siswa")
        )
        val eduRequests = listOf(
            LeaveOvertimeRequest("er_1", eduOrg.id, "e_2", listOf(demoUid), "Rafi Ahmad", "XI-IPS-2", RequestType.SAKIT.name, yesterdayKey, yesterdayKey, 8L, "Demam ringan, istirahat di rumah.", ApprovalStatus.APPROVED.name, demoUid, "Ibu Ratna", "Lekas sembuh")
        )

        return mapOf(
            workOrg.id to DemoOrgBundle(workOrg, workConfig, workMembers, workAttendances, workRequests, workNotifs, emptyList()),
            eduOrg.id to DemoOrgBundle(eduOrg, eduConfig, eduMembers, eduAttendances, eduRequests, emptyList(), emptyList())
        )
    }
}
