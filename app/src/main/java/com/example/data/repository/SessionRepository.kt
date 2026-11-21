package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceLocation
import com.example.data.model.BillingTransaction
import com.example.data.model.DefaultSubscriptionCatalog
import com.example.data.model.MemberSubType
import com.example.data.model.OrgJoinCode
import com.example.data.model.OrgRole
import com.example.data.model.OrgUnit
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.SubscriptionPlanTier
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.model.WorkSchedule
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SessionRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUser(): FirebaseUser {
        return auth.currentUser
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    private fun isBootstrappedSuperAdmin(user: FirebaseUser): Boolean {
        return user.isEmailVerified && user.email.equals("halimawan13@gmail.com", ignoreCase = true)
    }

    suspend fun ensureUserProfile(): Result<UserProfile> {
        var docPath = "users"
        return try {
            val user = requireUser()
            val docRef = db.collection("users").document(user.uid)
            docPath = docRef.path
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val superAdminFlag = isBootstrappedSuperAdmin(user)
                val payload = mapOf(
                    "userId" to user.uid,
                    "email" to (user.email ?: "${user.uid}@hadirin.id"),
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna HADIRIN"),
                    "photoUrl" to user.photoUrl?.toString()?.take(500),
                    "superAdmin" to superAdminFlag,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                if (superAdminFlag) {
                    seedDefaultSubscriptionPlansIfNeeded()
                }
            }
            val fresh = docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(
                    id = user.uid,
                    userId = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName ?: "Pengguna HADIRIN",
                    superAdmin = isBootstrappedSuperAdmin(user)
                )
            Result.success(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeUserMemberships(uid: String): Flow<List<UserMembership>> {
        val path = "user_memberships"
        return db.collection(path)
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserMembership::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.organizationName }
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

    suspend fun updateUserProfile(displayName: String, phone: String?): Result<Unit> {
        var docPath = "users"
        return try {
            val user = requireUser()
            val trimmedName = displayName.trim().take(100)
            require(trimmedName.isNotEmpty()) { "Nama lengkap wajib diisi." }
            val docRef = db.collection("users").document(user.uid)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "displayName" to trimmedName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!phone.isNullOrBlank()) {
                updates["phone"] = phone.trim().take(30)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun switchActiveOrganization(orgId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val user = requireUser()
            val docRef = db.collection("users").document(user.uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "activeOrganizationId" to orgId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createOrganization(
        name: String,
        type: OrganizationType,
        phone: String,
        address: String,
        adminDisplayName: String,
        latitude: Double = -6.200000,
        longitude: Double = 106.816666
    ): Result<String> {
        var docPath = "organizations"
        return try {
            val user = requireUser()
            val cleanName = name.trim().take(120)
            val cleanPhone = phone.trim().take(30)
            val cleanAddress = address.trim().take(300)
            val cleanAdminName = adminDisplayName.trim().ifBlank { user.displayName ?: "Admin" }.take(100)
            require(cleanName.length >= 2) { "Nama organisasi minimal 2 karakter." }
            require(cleanPhone.length >= 4) { "Nomor telepon minimal 4 digit." }
            require(cleanAddress.isNotEmpty()) { "Alamat organisasi wajib diisi." }

            val orgId = "org_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            docPath = "organizations/$orgId"
            val prefix = if (type == OrganizationType.EDU) "EDU" else "WRK"
            val joinCode = "${prefix}${UUID.randomUUID().toString().replace("-", "").take(5).uppercase()}"
            val trialEndsAt = System.currentTimeMillis() + 14L * 24L * 60L * 60L * 1000L
            val nowToken = "QR_${orgId.takeLast(6).uppercase()}_${(System.currentTimeMillis() / 1000)}"
            val qrExpiresAt = System.currentTimeMillis() + 5L * 60L * 1000L

            val defaultUnit = if (type == OrganizationType.EDU) {
                OrgUnit(id = "unit_1", kind = "CLASS", name = "Kelas X-A", code = "XA", supervisorName = cleanAdminName)
            } else {
                OrgUnit(id = "unit_1", kind = "DEPARTMENT", name = "Operasional", code = "OPS", supervisorName = cleanAdminName)
            }
            val defaultSchedule = if (type == OrganizationType.EDU) {
                WorkSchedule(id = "sch_1", name = "Jadwal Sekolah Pagi", startTime = "07:00", endTime = "14:00", lateToleranceMinutes = 15L, workDaysLabel = "Senin - Jumat")
            } else {
                WorkSchedule(id = "sch_1", name = "Shift Pagi Reguler", startTime = "08:00", endTime = "17:00", lateToleranceMinutes = 15L, workDaysLabel = "Senin - Jumat")
            }
            val defaultLocation = AttendanceLocation(
                id = "loc_1",
                name = if (type == OrganizationType.EDU) "Gerbang Utama Sekolah" else "Kantor Pusat",
                latitude = latitude,
                longitude = longitude,
                radiusMeters = 200L,
                active = true
            )

            val batch = db.batch()
            val orgRef = db.collection("organizations").document(orgId)
            batch.set(
                orgRef,
                mapOf(
                    "name" to cleanName,
                    "type" to type.name,
                    "joinCode" to joinCode,
                    "address" to cleanAddress,
                    "phone" to cleanPhone,
                    "ownerUid" to user.uid,
                    "staffUids" to listOf(user.uid),
                    "subscriptionPlanId" to SubscriptionPlanTier.FREE.id,
                    "subscriptionStatus" to "TRIAL",
                    "memberLimit" to 20L,
                    "trialEndsAtMillis" to trialEndsAt,
                    "requireGeofence" to true,
                    "requireDynamicQr" to false,
                    "requireSelfie" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val joinCodeRef = db.collection("org_join_codes").document(joinCode)
            batch.set(
                joinCodeRef,
                mapOf(
                    "organizationId" to orgId,
                    "organizationName" to cleanName,
                    "organizationType" to type.name,
                    "ownerUid" to user.uid,
                    "memberLimit" to 20L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val configRef = orgRef.collection("workspace").document("config")
            batch.set(
                configRef,
                mapOf(
                    "organizationId" to orgId,
                    "units" to listOf(
                        mapOf(
                            "id" to defaultUnit.id,
                            "kind" to defaultUnit.kind,
                            "name" to defaultUnit.name,
                            "code" to defaultUnit.code,
                            "supervisorName" to defaultUnit.supervisorName
                        )
                    ),
                    "schedules" to listOf(
                        mapOf(
                            "id" to defaultSchedule.id,
                            "name" to defaultSchedule.name,
                            "startTime" to defaultSchedule.startTime,
                            "endTime" to defaultSchedule.endTime,
                            "lateToleranceMinutes" to defaultSchedule.lateToleranceMinutes,
                            "workDaysLabel" to defaultSchedule.workDaysLabel
                        )
                    ),
                    "locations" to listOf(
                        mapOf(
                            "id" to defaultLocation.id,
                            "name" to defaultLocation.name,
                            "latitude" to defaultLocation.latitude,
                            "longitude" to defaultLocation.longitude,
                            "radiusMeters" to defaultLocation.radiusMeters,
                            "active" to defaultLocation.active
                        )
                    ),
                    "activeQrToken" to nowToken,
                    "qrExpiresAtMillis" to qrExpiresAt,
                    "qrLocationName" to defaultLocation.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val adminCode = "ADM-001"
            val memberRef = orgRef.collection("members").document(user.uid)
            batch.set(
                memberRef,
                mapOf(
                    "organizationId" to orgId,
                    "userId" to user.uid,
                    "staffUids" to listOf(user.uid),
                    "fullName" to cleanAdminName,
                    "email" to (user.email ?: ""),
                    "phone" to cleanPhone,
                    "memberCode" to adminCode,
                    "role" to OrgRole.ORG_ADMIN.name,
                    "subType" to if (type == OrganizationType.EDU) MemberSubType.TEACHER.name else MemberSubType.EMPLOYEE.name,
                    "unitId" to defaultUnit.id,
                    "unitName" to defaultUnit.name,
                    "scheduleId" to defaultSchedule.id,
                    "scheduleName" to defaultSchedule.name,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val membershipRef = db.collection("user_memberships").document("${user.uid}_$orgId")
            batch.set(
                membershipRef,
                mapOf(
                    "userId" to user.uid,
                    "organizationId" to orgId,
                    "organizationName" to cleanName,
                    "organizationType" to type.name,
                    "role" to OrgRole.ORG_ADMIN.name,
                    "memberCode" to adminCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val auditRef = orgRef.collection("audit_logs").document("log_${UUID.randomUUID().toString().take(8)}")
            batch.set(
                auditRef,
                mapOf(
                    "organizationId" to orgId,
                    "actorUid" to user.uid,
                    "actorName" to cleanAdminName,
                    "staffUids" to listOf(user.uid),
                    "action" to "ORGANISASI_DIBUAT",
                    "details" to "Organisasi $cleanName (${type.moduleName}) didaftarkan dengan kode gabung $joinCode.",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            val userRef = db.collection("users").document(user.uid)
            batch.update(
                userRef,
                mapOf(
                    "activeOrganizationId" to orgId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(orgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinOrganizationWithCode(
        joinCodeRaw: String,
        fullName: String,
        memberCodeRaw: String,
        joinAsParent: Boolean
    ): Result<String> {
        var docPath = "org_join_codes"
        return try {
            val user = requireUser()
            val code = joinCodeRaw.trim().uppercase()
            require(code.length >= 4 && code.matches(Regex("^[A-Z0-9_\\-]+$"))) {
                "Kode organisasi tidak valid."
            }
            docPath = "org_join_codes/$code"
            val codeSnap = db.collection("org_join_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode organisasi '$code' tidak ditemukan."))
            }
            val joinMeta = codeSnap.toObject(OrgJoinCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Data kode organisasi tidak valid."))

            val orgId = joinMeta.organizationId
            val orgType = OrganizationType.fromRaw(joinMeta.organizationType)
            val role = if (joinAsParent && orgType == OrganizationType.EDU) OrgRole.PARENT else OrgRole.MEMBER
            val subType = when {
                role == OrgRole.PARENT -> MemberSubType.PARENT
                orgType == OrganizationType.EDU -> MemberSubType.STUDENT
                else -> MemberSubType.EMPLOYEE
            }
            val cleanName = fullName.trim().ifBlank { user.displayName ?: "Anggota Baru" }.take(100)
            val memberCode = memberCodeRaw.trim().ifBlank {
                "${if (role == OrgRole.PARENT) "WALI" else "MBR"}-${user.uid.take(5).uppercase()}"
            }.take(40)

            val batch = db.batch()
            val memberRef = db.collection("organizations").document(orgId).collection("members").document(user.uid)
            batch.set(
                memberRef,
                mapOf(
                    "organizationId" to orgId,
                    "userId" to user.uid,
                    "staffUids" to listOf(joinMeta.ownerUid),
                    "fullName" to cleanName,
                    "email" to (user.email ?: ""),
                    "memberCode" to memberCode,
                    "role" to role.name,
                    "subType" to subType.name,
                    "joinCodeUsed" to code,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            )

            val membershipRef = db.collection("user_memberships").document("${user.uid}_$orgId")
            batch.set(
                membershipRef,
                mapOf(
                    "userId" to user.uid,
                    "organizationId" to orgId,
                    "organizationName" to joinMeta.organizationName,
                    "organizationType" to joinMeta.organizationType,
                    "role" to role.name,
                    "memberCode" to memberCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                db.collection("users").document(user.uid),
                mapOf(
                    "activeOrganizationId" to orgId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(orgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeSubscriptionPlans(): Flow<List<SubscriptionPlan>> {
        val path = "subscription_plans"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (list.isEmpty()) DefaultSubscriptionCatalog.plans else list.sortedBy { it.tierOrder }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                emit(DefaultSubscriptionCatalog.plans)
            }
    }

    suspend fun seedDefaultSubscriptionPlansIfNeeded(): Result<Unit> {
        return try {
            val snap = db.collection("subscription_plans").get().await()
            if (snap.isEmpty) {
                val batch = db.batch()
                DefaultSubscriptionCatalog.plans.forEach { plan ->
                    batch.set(
                        db.collection("subscription_plans").document(plan.id),
                        mapOf(
                            "name" to plan.name,
                            "monthlyPriceIdr" to plan.monthlyPriceIdr,
                            "maxUsers" to plan.maxUsers,
                            "features" to plan.features,
                            "tierOrder" to plan.tierOrder,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "subscription_plans")
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionPlan(planId: String, newPriceIdr: Long, newMaxUsers: Long): Result<Unit> {
        var docPath = "subscription_plans/$planId"
        return try {
            requireUser()
            val planTemplate = DefaultSubscriptionCatalog.plans.firstOrNull { it.id == planId }
                ?: return Result.failure(IllegalArgumentException("Paket tidak dikenali"))
            val docRef = db.collection("subscription_plans").document(planId)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "name" to planTemplate.name,
                    "monthlyPriceIdr" to newPriceIdr.coerceAtLeast(0L),
                    "maxUsers" to newMaxUsers.coerceIn(1L, 100000L),
                    "features" to planTemplate.features,
                    "tierOrder" to planTemplate.tierOrder,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeAllOrganizationsForSuperAdmin(): Flow<List<Organization>> {
        val path = "organizations"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(Organization::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllUsersForSuperAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllTransactionsForSuperAdmin(): Flow<List<BillingTransaction>> {
        val path = "transactions"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(BillingTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                throw error
            }
    }
}
