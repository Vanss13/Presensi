const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const ADMIN_UID = "admin_org_1";
const MEMBER_UID = "member_1";
const PARENT_UID = "parent_1";
const OUTSIDER_UID = "outsider_99";
const SUPER_UID = "super_admin_1";
const ORG_ID = "org_smp_1";
const OTHER_ORG_ID = "org_corp_2";
const JOIN_CODE = "SMP01A";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedOrgWithAdmin() {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("organizations").doc(ORG_ID).set({
      name: "SMP Negeri 1 Nusantara",
      type: "EDU",
      joinCode: JOIN_CODE,
      address: "Jl. Pendidikan No. 10, Jakarta",
      phone: "0215550101",
      ownerUid: ADMIN_UID,
      staffUids: [ADMIN_UID],
      subscriptionPlanId: "FREE",
      subscriptionStatus: "TRIAL",
      memberLimit: 20,
      trialEndsAtMillis: Date.now() + 14 * 86400000,
      requireGeofence: true,
      requireDynamicQr: false,
      requireSelfie: false,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("org_join_codes").doc(JOIN_CODE).set({
      organizationId: ORG_ID,
      organizationName: "SMP Negeri 1 Nusantara",
      organizationType: "EDU",
      ownerUid: ADMIN_UID,
      memberLimit: 20,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("organizations").doc(ORG_ID).collection("members").doc(ADMIN_UID).set({
      organizationId: ORG_ID,
      userId: ADMIN_UID,
      staffUids: [ADMIN_UID],
      fullName: "Budi Santoso",
      email: "budi@sekolah.id",
      memberCode: "ADM-001",
      role: "ORG_ADMIN",
      subType: "TEACHER",
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("organizations").doc(ORG_ID).collection("workspace").doc("config").set({
      organizationId: ORG_ID,
      units: [{ id: "u1", kind: "CLASS", name: "IX-A", code: "9A", supervisorName: "Budi" }],
      schedules: [{ id: "s1", name: "Pagi Reguler", startTime: "07:00", endTime: "14:00", lateToleranceMinutes: 15, workDaysLabel: "Senin-Jumat" }],
      locations: [{ id: "l1", name: "Gerbang Utama", latitude: -6.2, longitude: 106.8, radiusMeters: 150, active: true }],
      activeQrToken: "QR_INIT_01",
      qrExpiresAtMillis: Date.now() + 300000,
      qrLocationName: "Gerbang Utama",
      updatedAt: past,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ADMIN_UID).get());
  await assertFails(unauthDb.collection("organizations").doc(ORG_ID).get());
  await assertFails(unauthDb.collection("subscription_plans").get());
  await assertFails(
    unauthDb.collection("users").doc("hacker").set({
      userId: "hacker",
      email: "h@x.com",
      displayName: "Hacker",
      superAdmin: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile creation prevents privilege escalation and email spoofing", async () => {
  const past = new Date(Date.now() - 1000);

  // Regular user cannot self-assign superAdmin: true
  const normalDb = testEnv.authenticatedContext(MEMBER_UID, {
    email: "member@gmail.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    normalDb.collection("users").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      email: "member@gmail.com",
      displayName: "Member",
      superAdmin: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Unverified spoofed super admin email must fail
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "halimawan13@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      email: "halimawan13@gmail.com",
      displayName: "Spoof",
      superAdmin: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Verified bootstrapped Super Admin succeeds
  const superDb = testEnv.authenticatedContext(SUPER_UID, {
    email: "halimawan13@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    superDb.collection("users").doc(SUPER_UID).set({
      userId: SUPER_UID,
      email: "halimawan13@gmail.com",
      displayName: "Platform Owner",
      superAdmin: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Organization atomic batch creation succeeds and isolates other tenants", async () => {
  const past = new Date(Date.now() - 2000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "budi@sekolah.id",
    email_verified: true,
  }).firestore();

  const batch = adminDb.batch();
  batch.set(adminDb.collection("organizations").doc(ORG_ID), {
    name: "SMP Negeri 1 Nusantara",
    type: "EDU",
    joinCode: JOIN_CODE,
    address: "Jl. Pendidikan No. 10",
    phone: "0215550101",
    ownerUid: ADMIN_UID,
    staffUids: [ADMIN_UID],
    subscriptionPlanId: "FREE",
    subscriptionStatus: "TRIAL",
    memberLimit: 20,
    trialEndsAtMillis: Date.now() + 14 * 86400000,
    requireGeofence: true,
    requireDynamicQr: false,
    requireSelfie: false,
    createdAt: past,
    updatedAt: past,
  });
  batch.set(adminDb.collection("org_join_codes").doc(JOIN_CODE), {
    organizationId: ORG_ID,
    organizationName: "SMP Negeri 1 Nusantara",
    organizationType: "EDU",
    ownerUid: ADMIN_UID,
    memberLimit: 20,
    createdAt: past,
    updatedAt: past,
  });
  batch.set(adminDb.collection("organizations").doc(ORG_ID).collection("workspace").doc("config"), {
    organizationId: ORG_ID,
    units: [],
    schedules: [],
    locations: [],
    activeQrToken: "QR_01",
    qrExpiresAtMillis: Date.now() + 300000,
    qrLocationName: "Gerbang",
    updatedAt: past,
  });
  batch.set(adminDb.collection("organizations").doc(ORG_ID).collection("members").doc(ADMIN_UID), {
    organizationId: ORG_ID,
    userId: ADMIN_UID,
    staffUids: [ADMIN_UID],
    fullName: "Budi Santoso",
    memberCode: "ADM-001",
    role: "ORG_ADMIN",
    subType: "TEACHER",
    status: "ACTIVE",
    createdAt: past,
    updatedAt: past,
  });
  batch.set(adminDb.collection("user_memberships").doc(`${ADMIN_UID}_${ORG_ID}`), {
    userId: ADMIN_UID,
    organizationId: ORG_ID,
    organizationName: "SMP Negeri 1 Nusantara",
    organizationType: "EDU",
    role: "ORG_ADMIN",
    memberCode: "ADM-001",
    createdAt: past,
    updatedAt: past,
  });
  await assertSucceeds(batch.commit());

  // Outsider from another tenant cannot read org or list memberships
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(outsiderDb.collection("organizations").doc(ORG_ID).get());
  await assertFails(outsiderDb.collection("organizations").doc(ORG_ID).collection("workspace").doc("config").get());
  await assertFails(outsiderDb.collection("organizations").get());
});

test("Join via valid code works for MEMBER/PARENT and blocks self-promotion to ORG_ADMIN", async () => {
  await seedOrgWithAdmin();
  const past = new Date(Date.now() - 2000);
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();

  // Can look up exact join code
  await assertSucceeds(memberDb.collection("org_join_codes").doc(JOIN_CODE).get());
  // Cannot list all join codes
  await assertFails(memberDb.collection("org_join_codes").get());

  // Self-assigning ORG_ADMIN via join fails
  await assertFails(
    memberDb.collection("organizations").doc(ORG_ID).collection("members").doc(MEMBER_UID).set({
      organizationId: ORG_ID,
      userId: MEMBER_UID,
      staffUids: [ADMIN_UID],
      fullName: "Andi",
      memberCode: "SIS-001",
      role: "ORG_ADMIN",
      subType: "STUDENT",
      joinCodeUsed: JOIN_CODE,
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Joining as MEMBER with valid joinCodeUsed + user_memberships succeeds
  const batch = memberDb.batch();
  batch.set(memberDb.collection("organizations").doc(ORG_ID).collection("members").doc(MEMBER_UID), {
    organizationId: ORG_ID,
    userId: MEMBER_UID,
    staffUids: [ADMIN_UID],
    fullName: "Andi Pratama",
    memberCode: "SIS-001",
    role: "MEMBER",
    subType: "STUDENT",
    joinCodeUsed: JOIN_CODE,
    status: "ACTIVE",
    createdAt: past,
    updatedAt: past,
  });
  batch.set(memberDb.collection("user_memberships").doc(`${MEMBER_UID}_${ORG_ID}`), {
    userId: MEMBER_UID,
    organizationId: ORG_ID,
    organizationName: "SMP Negeri 1 Nusantara",
    organizationType: "EDU",
    role: "MEMBER",
    memberCode: "SIS-001",
    createdAt: past,
    updatedAt: past,
  });
  await assertSucceeds(batch.commit());
});

test("Attendance check-in, check-out, parent read, and query isolation", async () => {
  await seedOrgWithAdmin();
  const past = new Date(Date.now() - 2000);

  // Add member and parent
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("organizations").doc(ORG_ID).collection("members").doc(MEMBER_UID).set({
      organizationId: ORG_ID,
      userId: MEMBER_UID,
      staffUids: [ADMIN_UID],
      parentUid: PARENT_UID,
      fullName: "Andi Pratama",
      memberCode: "SIS-001",
      role: "MEMBER",
      subType: "STUDENT",
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("organizations").doc(ORG_ID).collection("members").doc(PARENT_UID).set({
      organizationId: ORG_ID,
      userId: PARENT_UID,
      staffUids: [ADMIN_UID],
      fullName: "Ibu Ani",
      memberCode: "WALI-001",
      role: "PARENT",
      subType: "PARENT",
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    });
  });

  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const attId = `${MEMBER_UID}_2026-11-21`;

  // Member creates attendance check-in (no @DocumentId `id` field!)
  await assertSucceeds(
    memberDb.collection("organizations").doc(ORG_ID).collection("attendances").doc(attId).set({
      organizationId: ORG_ID,
      userId: MEMBER_UID,
      staffUids: [ADMIN_UID],
      parentUid: PARENT_UID,
      memberName: "Andi Pratama",
      memberCode: "SIS-001",
      unitName: "IX-A",
      dateKey: "2026-11-21",
      checkInTimeText: "06:55",
      checkInAt: past,
      method: "GPS",
      latitude: -6.2,
      longitude: 106.8,
      distanceMeters: 42,
      withinGeofence: true,
      faceConsentGranted: false,
      status: "HADIR",
      lateMinutes: 0,
      workMinutes: 0,
      overtimeMinutes: 0,
      deviceInfo: "Pixel 8 Android",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Member checks out via partial update
  await assertSucceeds(
    memberDb.collection("organizations").doc(ORG_ID).collection("attendances").doc(attId).update({
      checkOutTimeText: "14:05",
      checkOutAt: new Date(Date.now() - 500),
      checkOutMethod: "GPS",
      workMinutes: 430,
      overtimeMinutes: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Member cannot tamper with status directly
  await assertFails(
    memberDb.collection("organizations").doc(ORG_ID).collection("attendances").doc(attId).update({
      status: "ALPHA",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Member filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    memberDb.collection("organizations").doc(ORG_ID).collection("attendances").where("userId", "==", MEMBER_UID).get()
  );
  await assertFails(
    memberDb.collection("organizations").doc(ORG_ID).collection("attendances").get()
  );

  // Parent filtered query succeeds
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  await assertSucceeds(
    parentDb.collection("organizations").doc(ORG_ID).collection("attendances").where("parentUid", "==", PARENT_UID).get()
  );

  // Org Admin staff query succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    adminDb.collection("organizations").doc(ORG_ID).collection("attendances").where("staffUids", "array-contains", ADMIN_UID).get()
  );
});

test("Leave request terminal state locking and role enforcement", async () => {
  await seedOrgWithAdmin();
  const past = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("organizations").doc(ORG_ID).collection("members").doc(MEMBER_UID).set({
      organizationId: ORG_ID,
      userId: MEMBER_UID,
      staffUids: [ADMIN_UID],
      fullName: "Andi Pratama",
      memberCode: "SIS-001",
      role: "MEMBER",
      subType: "STUDENT",
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    });
  });

  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    memberDb.collection("organizations").doc(ORG_ID).collection("requests").doc("req_1").set({
      organizationId: ORG_ID,
      userId: MEMBER_UID,
      staffUids: [ADMIN_UID],
      memberName: "Andi Pratama",
      unitName: "IX-A",
      type: "IZIN",
      startDate: "2026-11-22",
      endDate: "2026-11-22",
      durationHours: 8,
      reason: "Acara keluarga mendesak",
      approvalStatus: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Member cannot approve their own request
  await assertFails(
    memberDb.collection("organizations").doc(ORG_ID).collection("requests").doc("req_1").update({
      approvalStatus: "APPROVED",
      reviewedByUid: MEMBER_UID,
      reviewedByName: "Andi",
      reviewNote: "Self approve",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin approves request
  await assertSucceeds(
    adminDb.collection("organizations").doc(ORG_ID).collection("requests").doc("req_1").update({
      approvalStatus: "APPROVED",
      reviewedByUid: ADMIN_UID,
      reviewedByName: "Budi Santoso",
      reviewNote: "Disetujui",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: Admin cannot flip already-APPROVED request back to REJECTED
  await assertFails(
    adminDb.collection("organizations").doc(ORG_ID).collection("requests").doc("req_1").update({
      approvalStatus: "REJECTED",
      reviewedByUid: ADMIN_UID,
      reviewedByName: "Budi Santoso",
      reviewNote: "Berubah pikiran",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
