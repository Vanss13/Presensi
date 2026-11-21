package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ApprovalStatus
import com.example.data.model.AttendanceLocation
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AuditLog
import com.example.data.model.BillingTransaction
import com.example.data.model.LeaveOvertimeRequest
import com.example.data.model.MemberSubType
import com.example.data.model.OrgMember
import com.example.data.model.OrgRole
import com.example.data.model.OrgUnit
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.RequestType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.WorkSchedule
import com.example.data.model.WorkspaceConfig
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WorkspaceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    fun observeOrganization(orgId: String): Flow<Organization?> {
        val path = "organizations/$orgId"
        return db.collection("organizations").document(orgId)
            .snapshots()
            .map { it.toObject(Organization::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeWorkspaceConfig(orgId: String): Flow<WorkspaceConfig?> {
        val path = "organizations/$orgId/workspace/config"
        return db.collection("organizations").document(orgId)
            .collection("workspace").document("config")
            .snapshots()
            .map { it.toObject(WorkspaceConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeCurrentMember(orgId: String, uid: String): Flow<OrgMember?> {
        val path = "organizations/$orgId/members/$uid"
        return db.collection("organizations").document(orgId)
            .collection("members").document(uid)
            .snapshots()
            .map { it.toObject(OrgMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeMembers(orgId: String, role: OrgRole, uid: String): Flow<List<OrgMember>> {
        val path = "organizations/$orgId/members"
        val base = db.collection("organizations").document(orgId).collection("members")
        val query = when (role) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> base.whereArrayContains("staffUids", uid)
            OrgRole.PARENT -> base.whereEqualTo("parentUid", uid)
            OrgRole.MEMBER -> base.whereEqualTo("userId", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(OrgMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.fullName }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAttendances(orgId: String, role: OrgRole, uid: String): Flow<List<AttendanceRecord>> {
        val path = "organizations/$orgId/attendances"
        val base = db.collection("organizations").document(orgId).collection("attendances")
        val query = when (role) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> base.whereArrayContains("staffUids", uid)
            OrgRole.PARENT -> base.whereEqualTo("parentUid", uid)
            OrgRole.MEMBER -> base.whereEqualTo("userId", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.checkInAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeRequests(orgId: String, role: OrgRole, uid: String): Flow<List<LeaveOvertimeRequest>> {
        val path = "organizations/$orgId/requests"
        val base = db.collection("organizations").document(orgId).collection("requests")
        val query = when (role) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> base.whereArrayContains("staffUids", uid)
            OrgRole.MEMBER, OrgRole.PARENT -> base.whereEqualTo("userId", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(LeaveOvertimeRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeNotifications(orgId: String, role: OrgRole, uid: String): Flow<List<AppNotification>> {
        val path = "organizations/$orgId/notifications"
        val base = db.collection("organizations").document(orgId).collection("notifications")
        val query = when (role) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> base.whereArrayContains("staffUids", uid)
            OrgRole.MEMBER, OrgRole.PARENT -> base.whereEqualTo("targetUid", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAuditLogs(orgId: String, uid: String): Flow<List<AuditLog>> {
        val path = "organizations/$orgId/audit_logs"
        return db.collection("organizations").document(orgId)
            .collection("audit_logs")
            .whereArrayContains("staffUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeOrgTransactions(uid: String): Flow<List<BillingTransaction>> {
        val path = "transactions"
        return db.collection(path)
            .whereArrayContains("staffUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(BillingTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun checkIn(
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
        faceConsentGranted: Boolean
    ): Result<AttendanceRecord> {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        var docPath = "organizations/${org.id}/attendances"
        return try {
            val uid = requireUserId()
            val docId = "${uid}_$dateKey"
            val docRef = db.collection("organizations").document(org.id)
                .collection("attendances").document(docId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("Anda sudah melakukan presensi masuk hari ini ($dateKey)."))
            }

            val lateMinutes = computeLateMinutes(schedule)
            val status = if (lateMinutes > 0L) AttendanceStatus.TERLAMBAT else AttendanceStatus.HADIR
            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})".take(160)

            val payload = mapOf(
                "organizationId" to org.id,
                "userId" to uid,
                "staffUids" to org.staffUids.ifEmpty { listOf(org.ownerUid) },
                "parentUid" to member.parentUid,
                "memberName" to member.fullName,
                "memberCode" to member.memberCode,
                "unitName" to member.unitName,
                "dateKey" to dateKey,
                "checkInTimeText" to timeText,
                "checkInAt" to FieldValue.serverTimestamp(),
                "method" to method.name,
                "latitude" to latitude,
                "longitude" to longitude,
                "distanceMeters" to distanceMeters.coerceAtLeast(0L),
                "withinGeofence" to withinGeofence,
                "qrTokenUsed" to qrTokenUsed,
                "selfiePath" to selfiePath,
                "faceConsentGranted" to faceConsentGranted,
                "status" to status.name,
                "lateMinutes" to lateMinutes,
                "workMinutes" to 0L,
                "overtimeMinutes" to 0L,
                "deviceInfo" to deviceInfo,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(docRef, payload)

            // Notify parent if EDU student has linked parent
            if (!member.parentUid.isNullOrBlank()) {
                val notifRef = db.collection("organizations").document(org.id)
                    .collection("notifications").document("notif_${UUID.randomUUID().toString().take(10)}")
                val lateMsg = if (lateMinutes > 0) " (Terlambat $lateMinutes menit)" else " (Tepat waktu)"
                batch.set(
                    notifRef,
                    mapOf(
                        "organizationId" to org.id,
                        "targetUid" to member.parentUid,
                        "staffUids" to org.staffUids.ifEmpty { listOf(org.ownerUid) },
                        "title" to "Anak Anda Telah Masuk",
                        "message" to "${member.fullName} melakukan presensi masuk pukul $timeText via ${method.label}$lateMsg.",
                        "category" to if (lateMinutes > 0) "LATE" else "PARENT",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Notify staff if late
            if (lateMinutes > 0L) {
                val notifRef = db.collection("organizations").document(org.id)
                    .collection("notifications").document("notif_${UUID.randomUUID().toString().take(10)}")
                batch.set(
                    notifRef,
                    mapOf(
                        "organizationId" to org.id,
                        "targetUid" to org.ownerUid,
                        "staffUids" to org.staffUids.ifEmpty { listOf(org.ownerUid) },
                        "title" to "Keterlambatan Tercatat",
                        "message" to "${member.fullName} (${member.unitName ?: member.memberCode}) terlambat $lateMinutes menit pada pukul $timeText.",
                        "category" to "LATE",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            val saved = docRef.get().await()
                .toObject(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: AttendanceRecord(
                    id = docId,
                    organizationId = org.id,
                    userId = uid,
                    memberName = member.fullName,
                    dateKey = dateKey,
                    checkInTimeText = timeText,
                    method = method.name,
                    status = status.name,
                    lateMinutes = lateMinutes
                )
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun checkOut(
        org: Organization,
        member: OrgMember,
        todayRecord: AttendanceRecord,
        schedule: WorkSchedule?,
        method: AttendanceMethod
    ): Result<Unit> {
        var docPath = "organizations/${org.id}/attendances/${todayRecord.id}"
        return try {
            requireUserId()
            if (todayRecord.checkOutTimeText != null) {
                return Result.failure(IllegalStateException("Anda sudah melakukan presensi pulang hari ini."))
            }
            val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val checkInMillis = todayRecord.checkInAt?.toDate()?.time ?: System.currentTimeMillis()
            val workedMins = ((System.currentTimeMillis() - checkInMillis) / 60_000L).coerceIn(1L, 1440L)
            val overtimeMins = computeOvertimeMinutes(schedule)

            val docRef = db.collection("organizations").document(org.id)
                .collection("attendances").document(todayRecord.id)
            docPath = docRef.path

            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "checkOutTimeText" to timeText,
                    "checkOutAt" to FieldValue.serverTimestamp(),
                    "checkOutMethod" to method.name,
                    "workMinutes" to workedMins,
                    "overtimeMinutes" to overtimeMins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (!member.parentUid.isNullOrBlank()) {
                val notifRef = db.collection("organizations").document(org.id)
                    .collection("notifications").document("notif_${UUID.randomUUID().toString().take(10)}")
                batch.set(
                    notifRef,
                    mapOf(
                        "organizationId" to org.id,
                        "targetUid" to member.parentUid,
                        "staffUids" to org.staffUids.ifEmpty { listOf(org.ownerUid) },
                        "title" to "Anak Anda Telah Pulang",
                        "message" to "${member.fullName} melakukan presensi pulang pukul $timeText via ${method.label}.",
                        "category" to "PARENT",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitRequest(
        org: Organization,
        member: OrgMember,
        type: RequestType,
        startDate: String,
        endDate: String,
        durationHours: Long,
        reason: String
    ): Result<Unit> {
        var docPath = "organizations/${org.id}/requests"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim().take(500)
            require(cleanReason.length >= 3) { "Alasan pengajuan minimal 3 karakter." }
            val reqId = "req_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val docRef = db.collection("organizations").document(org.id)
                .collection("requests").document(reqId)
            docPath = docRef.path

            val staff = org.staffUids.ifEmpty { listOf(org.ownerUid) }
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "organizationId" to org.id,
                    "userId" to uid,
                    "staffUids" to staff,
                    "memberName" to member.fullName,
                    "unitName" to member.unitName,
                    "type" to type.name,
                    "startDate" to startDate.trim().take(30),
                    "endDate" to endDate.trim().take(30),
                    "durationHours" to durationHours.coerceIn(0L, 720L),
                    "reason" to cleanReason,
                    "approvalStatus" to ApprovalStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            )

            val notifRef = db.collection("organizations").document(org.id)
                .collection("notifications").document("notif_${UUID.randomUUID().toString().take(10)}")
            batch.set(
                notifRef,
                mapOf(
                    "organizationId" to org.id,
                    "targetUid" to org.ownerUid,
                    "staffUids" to staff,
                    "title" to "Pengajuan ${type.label} Baru",
                    "message" to "${member.fullName} mengajukan ${type.label} ($startDate - $endDate).",
                    "category" to "LEAVE",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reviewRequest(
        org: Organization,
        reviewerName: String,
        request: LeaveOvertimeRequest,
        decision: ApprovalStatus,
        note: String
    ): Result<Unit> {
        var docPath = "organizations/${org.id}/requests/${request.id}"
        return try {
            val uid = requireUserId()
            require(decision == ApprovalStatus.APPROVED || decision == ApprovalStatus.REJECTED) {
                "Keputusan harus disetujui atau ditolak."
            }
            val docRef = db.collection("organizations").document(org.id)
                .collection("requests").document(request.id)
            docPath = docRef.path

            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "approvalStatus" to decision.name,
                    "reviewedByUid" to uid,
                    "reviewedByName" to reviewerName.take(100),
                    "reviewNote" to note.trim().ifBlank { decision.label }.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val staff = org.staffUids.ifEmpty { listOf(org.ownerUid) }
            val notifRef = db.collection("organizations").document(org.id)
                .collection("notifications").document("notif_${UUID.randomUUID().toString().take(10)}")
            batch.set(
                notifRef,
                mapOf(
                    "organizationId" to org.id,
                    "targetUid" to request.userId,
                    "staffUids" to staff,
                    "title" to "Pengajuan ${RequestType.fromRaw(request.type).label} ${decision.label}",
                    "message" to "Ditinjau oleh $reviewerName: ${note.ifBlank { decision.label }}",
                    "category" to "LEAVE",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            val auditRef = db.collection("organizations").document(org.id)
                .collection("audit_logs").document("log_${UUID.randomUUID().toString().take(10)}")
            batch.set(
                auditRef,
                mapOf(
                    "organizationId" to org.id,
                    "actorUid" to uid,
                    "actorName" to reviewerName.take(100),
                    "staffUids" to staff,
                    "action" to "VERIFIKASI_${request.type}",
                    "details" to "${decision.label} pengajuan ${request.memberName} (${request.startDate}).",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addUnit(orgId: String, currentConfig: WorkspaceConfig, kind: String, name: String, code: String, supervisor: String): Result<Unit> {
        return try {
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama kelas/departemen wajib diisi." }
            val newUnit = OrgUnit(
                id = "u_${UUID.randomUUID().toString().take(8)}",
                kind = kind,
                name = cleanName,
                code = code.trim().ifBlank { cleanName.take(4).uppercase() },
                supervisorName = supervisor.trim()
            )
            updateWorkspaceField(orgId, mapOf("units" to (currentConfig.units + newUnit).map {
                mapOf("id" to it.id, "kind" to it.kind, "name" to it.name, "code" to it.code, "supervisorName" to it.supervisorName)
            }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSchedule(orgId: String, currentConfig: WorkspaceConfig, name: String, start: String, end: String, tolerance: Long, workDays: String): Result<Unit> {
        return try {
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama jadwal/shift wajib diisi." }
            val newSchedule = WorkSchedule(
                id = "sch_${UUID.randomUUID().toString().take(8)}",
                name = cleanName,
                startTime = start.trim().ifBlank { "08:00" },
                endTime = end.trim().ifBlank { "17:00" },
                lateToleranceMinutes = tolerance.coerceIn(0L, 180L),
                workDaysLabel = workDays.trim().ifBlank { "Senin - Jumat" }
            )
            updateWorkspaceField(orgId, mapOf("schedules" to (currentConfig.schedules + newSchedule).map {
                mapOf("id" to it.id, "name" to it.name, "startTime" to it.startTime, "endTime" to it.endTime, "lateToleranceMinutes" to it.lateToleranceMinutes, "workDaysLabel" to it.workDaysLabel)
            }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addLocation(orgId: String, currentConfig: WorkspaceConfig, name: String, lat: Double, lon: Double, radius: Long): Result<Unit> {
        return try {
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama lokasi presensi wajib diisi." }
            val newLoc = AttendanceLocation(
                id = "loc_${UUID.randomUUID().toString().take(8)}",
                name = cleanName,
                latitude = lat,
                longitude = lon,
                radiusMeters = radius.coerceIn(25L, 5000L),
                active = true
            )
            updateWorkspaceField(orgId, mapOf("locations" to (currentConfig.locations + newLoc).map {
                mapOf("id" to it.id, "name" to it.name, "latitude" to it.latitude, "longitude" to it.longitude, "radiusMeters" to it.radiusMeters, "active" to it.active)
            }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rotateDynamicQr(orgId: String, locationName: String, validityMinutes: Long = 5L): Result<String> {
        val token = "HDR-${orgId.takeLast(4).uppercase()}-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
        val expiresAt = System.currentTimeMillis() + validityMinutes * 60_000L
        val res = updateWorkspaceField(
            orgId,
            mapOf(
                "activeQrToken" to token,
                "qrExpiresAtMillis" to expiresAt,
                "qrLocationName" to locationName.take(120)
            )
        )
        return res.map { token }
    }

    private suspend fun updateWorkspaceField(orgId: String, fields: Map<String, Any>): Result<Unit> {
        val docRef = db.collection("organizations").document(orgId)
            .collection("workspace").document("config")
        return try {
            requireUserId()
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrgAttendancePolicies(
        orgId: String,
        requireGeofence: Boolean,
        requireDynamicQr: Boolean,
        requireSelfie: Boolean
    ): Result<Unit> {
        val docRef = db.collection("organizations").document(orgId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "requireGeofence" to requireGeofence,
                    "requireDynamicQr" to requireDynamicQr,
                    "requireSelfie" to requireSelfie,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addRosterMember(
        org: Organization,
        fullName: String,
        memberCode: String,
        role: OrgRole,
        subType: MemberSubType,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        email: String?,
        phone: String?
    ): Result<Unit> {
        var docPath = "organizations/${org.id}/members"
        return try {
            requireUserId()
            val cleanName = fullName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Nama anggota wajib diisi." }
            val memberDocId = "roster_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val docRef = db.collection("organizations").document(org.id)
                .collection("members").document(memberDocId)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "organizationId" to org.id,
                    "userId" to memberDocId,
                    "staffUids" to org.staffUids.ifEmpty { listOf(org.ownerUid) },
                    "fullName" to cleanName,
                    "email" to email?.trim()?.takeIf { it.isNotBlank() }?.take(160),
                    "phone" to phone?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                    "memberCode" to memberCode.trim().ifBlank { memberDocId.uppercase() }.take(40),
                    "role" to role.name,
                    "subType" to subType.name,
                    "unitId" to unit?.id,
                    "unitName" to unit?.name,
                    "scheduleId" to schedule?.id,
                    "scheduleName" to schedule?.name,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMemberAssignment(
        org: Organization,
        member: OrgMember,
        newRole: OrgRole,
        unit: OrgUnit?,
        schedule: WorkSchedule?,
        parentUid: String?
    ): Result<Unit> {
        val docRef = db.collection("organizations").document(org.id)
            .collection("members").document(member.id)
        return try {
            requireUserId()
            val batch = db.batch()
            val updates = mutableMapOf<String, Any>(
                "role" to newRole.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (unit != null) {
                updates["unitId"] = unit.id
                updates["unitName"] = unit.name
            }
            if (schedule != null) {
                updates["scheduleId"] = schedule.id
                updates["scheduleName"] = schedule.name
            }
            if (!parentUid.isNullOrBlank()) {
                updates["parentUid"] = parentUid.trim().take(128)
            }
            batch.update(docRef, updates)

            // Sync staffUids on organization and user_memberships role for authenticated member
            if (member.userId == member.id) {
                batch.update(
                    db.collection("user_memberships").document("${member.userId}_${org.id}"),
                    mapOf("role" to newRole.name, "updatedAt" to FieldValue.serverTimestamp())
                )
                if (newRole == OrgRole.SUPERVISOR || newRole == OrgRole.ORG_ADMIN) {
                    val nextStaff = (org.staffUids + member.userId).distinct().take(20)
                    batch.update(
                        db.collection("organizations").document(org.id),
                        mapOf("staffUids" to nextStaff, "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(orgId: String, notificationId: String): Result<Unit> {
        val docRef = db.collection("organizations").document(orgId)
            .collection("notifications").document(notificationId)
        return try {
            requireUserId()
            docRef.update(mapOf("readFlag" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upgradeOrganizationPlan(
        org: Organization,
        actorName: String,
        plan: SubscriptionPlan,
        paymentMethod: String
    ): Result<String> {
        var docPath = "transactions"
        return try {
            val uid = requireUserId()
            val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val invoiceNumber = "INV-${SimpleDateFormat("yyyyMM", Locale.US).format(Date())}-${txId.takeLast(5).uppercase()}"
            val staff = org.staffUids.ifEmpty { listOf(org.ownerUid) }

            val batch = db.batch()
            val txRef = db.collection("transactions").document(txId)
            docPath = txRef.path
            batch.set(
                txRef,
                mapOf(
                    "organizationId" to org.id,
                    "organizationName" to org.name,
                    "userId" to uid,
                    "staffUids" to staff,
                    "planId" to plan.id,
                    "planName" to plan.name,
                    "amountIdr" to plan.monthlyPriceIdr,
                    "status" to "PAID",
                    "invoiceNumber" to invoiceNumber,
                    "paymentMethod" to paymentMethod.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("organizations").document(org.id),
                mapOf(
                    "subscriptionPlanId" to plan.id,
                    "subscriptionStatus" to "ACTIVE",
                    "memberLimit" to plan.maxUsers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("organizations").document(org.id)
                    .collection("audit_logs").document("log_${UUID.randomUUID().toString().take(10)}"),
                mapOf(
                    "organizationId" to org.id,
                    "actorUid" to uid,
                    "actorName" to actorName.take(100),
                    "staffUids" to staff,
                    "action" to "UPGRADE_PAKET",
                    "details" to "Berhasil beralih ke paket ${plan.name} ($invoiceNumber).",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(invoiceNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedDemoWorkspaceData(org: Organization, currentUserId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val isEdu = OrganizationType.fromRaw(org.type) == OrganizationType.EDU
            val staff = org.staffUids.ifEmpty { listOf(org.ownerUid) }
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L))
            val batch = db.batch()

            val sampleNames = if (isEdu) {
                listOf("Aisyah Putri" to "IX-A", "Rafi Ahmad" to "IX-A", "Nadia Kusuma" to "X-IPA-1", "Dimas Pratama" to "X-IPA-1")
            } else {
                listOf("Rina Wijaya" to "Operasional", "Fajar Nugroho" to "Teknologi", "Siti Rahma" to "Keuangan", "Bagus Saputra" to "Pemasaran")
            }

            sampleNames.forEachIndexed { idx, (name, unitName) ->
                val memberId = "demo_m_$idx"
                val code = if (isEdu) "SIS-10${idx + 1}" else "EMP-20${idx + 1}"
                batch.set(
                    db.collection("organizations").document(org.id).collection("members").document(memberId),
                    mapOf(
                        "organizationId" to org.id,
                        "userId" to memberId,
                        "staffUids" to staff,
                        "parentUid" to if (isEdu && idx == 0) currentUserId else null,
                        "fullName" to name,
                        "memberCode" to code,
                        "role" to OrgRole.MEMBER.name,
                        "subType" to if (isEdu) MemberSubType.STUDENT.name else MemberSubType.EMPLOYEE.name,
                        "unitName" to unitName,
                        "status" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )

                val status = when (idx) {
                    0 -> AttendanceStatus.HADIR
                    1 -> AttendanceStatus.TERLAMBAT
                    2 -> AttendanceStatus.HADIR
                    else -> AttendanceStatus.IZIN
                }
                val dateKey = if (idx < 3) todayKey else yesterdayKey
                batch.set(
                    db.collection("organizations").document(org.id).collection("attendances").document("${memberId}_$dateKey"),
                    mapOf(
                        "organizationId" to org.id,
                        "userId" to memberId,
                        "staffUids" to staff,
                        "parentUid" to if (isEdu && idx == 0) currentUserId else null,
                        "memberName" to name,
                        "memberCode" to code,
                        "unitName" to unitName,
                        "dateKey" to dateKey,
                        "checkInTimeText" to if (status == AttendanceStatus.TERLAMBAT) "08:22" else "07:48",
                        "checkOutTimeText" to if (idx == 0) "16:30" else null,
                        "checkInAt" to FieldValue.serverTimestamp(),
                        "method" to if (idx % 2 == 0) AttendanceMethod.GPS.name else AttendanceMethod.QR.name,
                        "distanceMeters" to (25L + idx * 18L),
                        "withinGeofence" to true,
                        "faceConsentGranted" to false,
                        "status" to status.name,
                        "lateMinutes" to if (status == AttendanceStatus.TERLAMBAT) 22L else 0L,
                        "workMinutes" to if (idx == 0) 480L else 0L,
                        "overtimeMinutes" to if (idx == 0 && !isEdu) 30L else 0L,
                        "deviceInfo" to "HADIRIN Sample Client",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }

            batch.set(
                db.collection("organizations").document(org.id).collection("requests").document("demo_req_1"),
                mapOf(
                    "organizationId" to org.id,
                    "userId" to "demo_m_3",
                    "staffUids" to staff,
                    "memberName" to sampleNames[3].first,
                    "unitName" to sampleNames[3].second,
                    "type" to if (isEdu) RequestType.IZIN.name else RequestType.CUTI.name,
                    "startDate" to todayKey,
                    "endDate" to todayKey,
                    "durationHours" to 8L,
                    "reason" to "Keperluan keluarga terencana.",
                    "approvalStatus" to ApprovalStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "organizations/${org.id}")
            Result.failure(e)
        }
    }

    private fun computeLateMinutes(schedule: WorkSchedule?): Long {
        if (schedule == null) return 0L
        return try {
            val parts = schedule.startTime.split(":")
            val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = java.util.Calendar.getInstance()
            val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val deadlineMinutes = startHour * 60 + startMin + schedule.lateToleranceMinutes.toInt()
            (nowMinutes - deadlineMinutes).coerceAtLeast(0).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    private fun computeOvertimeMinutes(schedule: WorkSchedule?): Long {
        if (schedule == null) return 0L
        return try {
            val parts = schedule.endTime.split(":")
            val endHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
            val endMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = java.util.Calendar.getInstance()
            val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val endTotal = endHour * 60 + endMin
            (nowMinutes - endTotal).coerceAtLeast(0).toLong()
        } catch (_: Exception) {
            0L
        }
    }
}
