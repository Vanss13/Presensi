package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrganizationType(val label: String, val moduleName: String) {
    EDU("Pendidikan (Sekolah / Pesantren)", "HADIRIN EDU"),
    WORK("Perusahaan / UMKM / Instansi", "HADIRIN WORK");

    companion object {
        fun fromRaw(raw: String?): OrganizationType =
            entries.firstOrNull { it.name == raw } ?: WORK
    }
}

enum class OrgRole(val label: String) {
    ORG_ADMIN("Admin Organisasi"),
    SUPERVISOR("Supervisor / Guru / HRD"),
    MEMBER("Anggota"),
    PARENT("Wali Murid");

    companion object {
        fun fromRaw(raw: String?): OrgRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class MemberSubType(val label: String) {
    STUDENT("Siswa / Santri"),
    TEACHER("Guru / Pengajar"),
    EMPLOYEE("Karyawan"),
    PARENT("Orang Tua / Wali");

    companion object {
        fun fromRaw(raw: String?): MemberSubType =
            entries.firstOrNull { it.name == raw } ?: EMPLOYEE
    }
}

enum class AttendanceMethod(val label: String) {
    GPS("GPS Geofencing"),
    QR("QR Code Dinamis"),
    SELFIE("Selfie Kehadiran");

    companion object {
        fun fromRaw(raw: String?): AttendanceMethod =
            entries.firstOrNull { it.name == raw } ?: GPS
    }
}

enum class AttendanceStatus(val label: String) {
    HADIR("Hadir"),
    TERLAMBAT("Terlambat"),
    IZIN("Izin"),
    SAKIT("Sakit"),
    ALPHA("Tidak Hadir");

    companion object {
        fun fromRaw(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.name == raw } ?: HADIR
    }
}

enum class RequestType(val label: String) {
    CUTI("Cuti"),
    SAKIT("Sakit"),
    IZIN("Izin"),
    LEMBUR("Lembur"),
    KOREKSI("Koreksi Presensi");

    companion object {
        fun fromRaw(raw: String?): RequestType =
            entries.firstOrNull { it.name == raw } ?: IZIN
    }
}

enum class ApprovalStatus(val label: String) {
    PENDING("Menunggu"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak");

    companion object {
        fun fromRaw(raw: String?): ApprovalStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class SubscriptionPlanTier(val id: String, val label: String) {
    FREE("FREE", "Free"),
    STARTER("STARTER", "Starter"),
    BUSINESS("BUSINESS", "Business"),
    PROFESSIONAL("PROFESSIONAL", "Professional"),
    ENTERPRISE("ENTERPRISE", "Enterprise");

    companion object {
        fun fromRaw(raw: String?): SubscriptionPlanTier =
            entries.firstOrNull { it.id == raw } ?: FREE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phone: String? = null,
    val activeOrganizationId: String? = null,
    val superAdmin: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrgJoinCode(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val organizationType: String = OrganizationType.WORK.name,
    val ownerUid: String = "",
    val memberLimit: Long = 20L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserMembership(
    @DocumentId val id: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val organizationType: String = OrganizationType.WORK.name,
    val role: String = OrgRole.MEMBER.name,
    val memberCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Organization(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = OrganizationType.WORK.name,
    val joinCode: String = "",
    val address: String = "",
    val phone: String = "",
    val ownerUid: String = "",
    val staffUids: List<String> = emptyList(),
    val subscriptionPlanId: String = SubscriptionPlanTier.FREE.id,
    val subscriptionStatus: String = "TRIAL",
    val memberLimit: Long = 20L,
    val trialEndsAtMillis: Long = 0L,
    val requireGeofence: Boolean = true,
    val requireDynamicQr: Boolean = false,
    val requireSelfie: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrgUnit(
    val id: String = "",
    val kind: String = "CLASS", // CLASS | DEPARTMENT | DIVISION
    val name: String = "",
    val code: String = "",
    val supervisorName: String = ""
)

@Keep
@IgnoreExtraProperties
data class WorkSchedule(
    val id: String = "",
    val name: String = "",
    val startTime: String = "08:00",
    val endTime: String = "17:00",
    val lateToleranceMinutes: Long = 15L,
    val workDaysLabel: String = "Senin - Jumat"
)

@Keep
@IgnoreExtraProperties
data class AttendanceLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = -6.200000,
    val longitude: Double = 106.816666,
    val radiusMeters: Long = 150L,
    val active: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class WorkspaceConfig(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val units: List<OrgUnit> = emptyList(),
    val schedules: List<WorkSchedule> = emptyList(),
    val locations: List<AttendanceLocation> = emptyList(),
    val activeQrToken: String = "",
    val qrExpiresAtMillis: Long = 0L,
    val qrLocationName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrgMember(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val userId: String = "",
    val staffUids: List<String> = emptyList(),
    val parentUid: String? = null,
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val memberCode: String = "",
    val role: String = OrgRole.MEMBER.name,
    val subType: String = MemberSubType.EMPLOYEE.name,
    val unitId: String? = null,
    val unitName: String? = null,
    val division: String? = null,
    val scheduleId: String? = null,
    val scheduleName: String? = null,
    val joinCodeUsed: String? = null,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val userId: String = "",
    val staffUids: List<String> = emptyList(),
    val parentUid: String? = null,
    val memberName: String = "",
    val memberCode: String = "",
    val unitName: String? = null,
    val dateKey: String = "",
    val checkInTimeText: String = "",
    val checkOutTimeText: String? = null,
    @ServerTimestamp val checkInAt: Timestamp? = null,
    val checkOutAt: Timestamp? = null,
    val method: String = AttendanceMethod.GPS.name,
    val checkOutMethod: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceMeters: Long = 0L,
    val withinGeofence: Boolean = true,
    val qrTokenUsed: String? = null,
    val selfiePath: String? = null,
    val faceConsentGranted: Boolean = false,
    val status: String = AttendanceStatus.HADIR.name,
    val lateMinutes: Long = 0L,
    val workMinutes: Long = 0L,
    val overtimeMinutes: Long = 0L,
    val deviceInfo: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaveOvertimeRequest(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val userId: String = "",
    val staffUids: List<String> = emptyList(),
    val memberName: String = "",
    val unitName: String? = null,
    val type: String = RequestType.IZIN.name,
    val startDate: String = "",
    val endDate: String = "",
    val durationHours: Long = 8L,
    val reason: String = "",
    val approvalStatus: String = ApprovalStatus.PENDING.name,
    val reviewedByUid: String? = null,
    val reviewedByName: String? = null,
    val reviewNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val targetUid: String = "",
    val staffUids: List<String> = emptyList(),
    val title: String = "",
    val message: String = "",
    val category: String = "ATTENDANCE",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val staffUids: List<String> = emptyList(),
    val action: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val monthlyPriceIdr: Long = 0L,
    val maxUsers: Long = 20L,
    val features: List<String> = emptyList(),
    val tierOrder: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BillingTransaction(
    @DocumentId val id: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val userId: String = "",
    val staffUids: List<String> = emptyList(),
    val planId: String = SubscriptionPlanTier.FREE.id,
    val planName: String = "",
    val amountIdr: Long = 0L,
    val status: String = "PAID",
    val invoiceNumber: String = "",
    val paymentMethod: String = "Virtual Account",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object DefaultSubscriptionCatalog {
    val plans: List<SubscriptionPlan> = listOf(
        SubscriptionPlan(
            id = "FREE",
            name = "Free",
            monthlyPriceIdr = 0L,
            maxUsers = 20L,
            features = listOf("Maksimal 20 pengguna", "Presensi GPS dasar", "Dashboard ringkas"),
            tierOrder = 1L
        ),
        SubscriptionPlan(
            id = "STARTER",
            name = "Starter",
            monthlyPriceIdr = 149000L,
            maxUsers = 50L,
            features = listOf("Maksimal 50 pengguna", "GPS Geofencing & QR Code", "Laporan dasar"),
            tierOrder = 2L
        ),
        SubscriptionPlan(
            id = "BUSINESS",
            name = "Business",
            monthlyPriceIdr = 399000L,
            maxUsers = 150L,
            features = listOf("Maksimal 150 pengguna", "Pengajuan cuti & izin", "Export PDF & CSV", "Manajemen jadwal/shift"),
            tierOrder = 3L
        ),
        SubscriptionPlan(
            id = "PROFESSIONAL",
            name = "Professional",
            monthlyPriceIdr = 899000L,
            maxUsers = 500L,
            features = listOf("Maksimal 500 pengguna", "Multi-lokasi geofence", "Notifikasi orang tua & supervisor", "Analitik kehadiran"),
            tierOrder = 4L
        ),
        SubscriptionPlan(
            id = "ENTERPRISE",
            name = "Enterprise",
            monthlyPriceIdr = 2499000L,
            maxUsers = 5000L,
            features = listOf("Kapasitas kustom/kontrak", "Multi-cabang & unit", "Integrasi WhatsApp & Audit Log", "Dukungan prioritas"),
            tierOrder = 5L
        )
    )
}
