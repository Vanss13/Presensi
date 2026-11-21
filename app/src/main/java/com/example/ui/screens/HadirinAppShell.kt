package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrgRole
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinNavy
import com.example.viewmodel.WorkspaceViewModel

private enum class ShellDestination(
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    ATTENDANCE("Presensi", Icons.Default.Fingerprint),
    HISTORY("Riwayat", Icons.Default.History),
    PARENT_MONITOR("Anak", Icons.Default.ChildCare),
    LEAVE("Izin/Cuti", Icons.Default.EventNote),
    MANAGE("Kelola", Icons.Default.Settings),
    REPORTS("Laporan", Icons.Default.Assessment),
    PROFILE("Profil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadirinAppShell(
    userProfile: UserProfile?,
    memberships: List<UserMembership>,
    plans: List<SubscriptionPlan>,
    activeRole: OrgRole,
    workspaceViewModel: WorkspaceViewModel,
    snackbarHostState: SnackbarHostState,
    isSessionBusy: Boolean,
    onSwitchOrganization: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onUpdateProfile: (String, String?) -> Unit,
    onSignOut: () -> Unit,
    onShowLocalMessage: (String) -> Unit
) {
    val orgState by workspaceViewModel.organization.collectAsStateWithLifecycle()
    val config by workspaceViewModel.workspaceConfig.collectAsStateWithLifecycle()
    val currentMember by workspaceViewModel.currentMember.collectAsStateWithLifecycle()
    val members by workspaceViewModel.members.collectAsStateWithLifecycle()
    val attendances by workspaceViewModel.attendances.collectAsStateWithLifecycle()
    val requests by workspaceViewModel.requests.collectAsStateWithLifecycle()
    val notifications by workspaceViewModel.notifications.collectAsStateWithLifecycle()
    val auditLogs by workspaceViewModel.auditLogs.collectAsStateWithLifecycle()
    val transactions by workspaceViewModel.transactions.collectAsStateWithLifecycle()
    val isActionBusy by workspaceViewModel.isActionLoading.collectAsStateWithLifecycle()

    val isStaff = activeRole == OrgRole.ORG_ADMIN || activeRole == OrgRole.SUPERVISOR
    val primaryTabs = remember(activeRole) {
        when (activeRole) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> listOf(
                ShellDestination.DASHBOARD,
                ShellDestination.ATTENDANCE,
                ShellDestination.LEAVE,
                ShellDestination.MANAGE,
                ShellDestination.PROFILE
            )
            OrgRole.MEMBER -> listOf(
                ShellDestination.ATTENDANCE,
                ShellDestination.HISTORY,
                ShellDestination.LEAVE,
                ShellDestination.PROFILE
            )
            OrgRole.PARENT -> listOf(
                ShellDestination.PARENT_MONITOR,
                ShellDestination.LEAVE,
                ShellDestination.PROFILE
            )
        }
    }

    var currentDest by remember(activeRole) { mutableStateOf(primaryTabs.first()) }
    var manageInitialTab by remember { mutableIntStateOf(0) }
    var showOrgSwitcherMenu by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.readFlag }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Pusat Notifikasi (${notifications.size})") },
            text = {
                if (notifications.isEmpty()) {
                    Text("Belum ada notifikasi masuk atau pengingat.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            Card(
                                onClick = {
                                    if (!notif.readFlag && (notif.targetUid == userProfile?.userId || userProfile?.superAdmin == true)) {
                                        workspaceViewModel.markNotificationRead(notif.id)
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.readFlag) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                        if (!notif.readFlag) {
                                            StatusBadge(label = "Baru", statusKey = "PENDING")
                                        }
                                    }
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    val activeOrg: Organization? = (orgState as? UiState.Success)?.data

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeOrg?.name ?: "Memuat Organisasi...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeOrg != null) {
                            Text(
                                text = "${OrganizationType.fromRaw(activeOrg.type).moduleName} • ${activeRole.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    if (isStaff && activeOrg != null) {
                        IconButton(
                            onClick = { currentDest = ShellDestination.REPORTS },
                            modifier = Modifier.testTag("topbar_reports_button")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = "Laporan PDF & CSV")
                        }
                    }
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("topbar_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showOrgSwitcherMenu = true },
                            modifier = Modifier.testTag("topbar_org_switcher")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Pindah Organisasi")
                        }
                        DropdownMenu(
                            expanded = showOrgSwitcherMenu,
                            onDismissRequest = { showOrgSwitcherMenu = false }
                        ) {
                            memberships.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.organizationName} (${OrgRole.fromRaw(m.role).label})") },
                                    onClick = {
                                        showOrgSwitcherMenu = false
                                        onSwitchOrganization(m.organizationId)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Buat / Gabung Organisasi") },
                                onClick = {
                                    showOrgSwitcherMenu = false
                                    onOpenOnboarding()
                                }
                            )
                            if (userProfile?.superAdmin == true) {
                                DropdownMenuItem(
                                    text = { Text("Konsol Super Admin") },
                                    onClick = {
                                        showOrgSwitcherMenu = false
                                        onOpenSuperAdmin()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HadirinNavy,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BoxWithConstraints {
                if (maxWidth < 600.dp) {
                    NavigationBar {
                        primaryTabs.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("bottom_nav_${dest.name}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val useRail = maxWidth >= 600.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    val railTabs = if (isStaff) {
                        primaryTabs + listOf(ShellDestination.REPORTS, ShellDestination.HISTORY)
                    } else {
                        primaryTabs
                    }
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        railTabs.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_nav_${dest.name}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val state = orgState) {
                        UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                        is UiState.Error -> {
                            Box(modifier = Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.Default.Badge,
                                    title = "Gagal Memuat Organisasi",
                                    description = state.message
                                )
                            }
                        }
                        is UiState.Success -> {
                            val org = state.data
                            if (org == null) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    EmptyStateCard(
                                        icon = Icons.Default.Badge,
                                        title = "Organisasi Tidak Ditemukan",
                                        description = "Silakan pilih organisasi lain atau daftarkan organisasi baru."
                                    )
                                }
                            } else {
                                when (currentDest) {
                                    ShellDestination.DASHBOARD -> OrgDashboardScreen(
                                        organization = org,
                                        config = config,
                                        members = members,
                                        attendances = attendances,
                                        requests = requests,
                                        isBusy = isActionBusy,
                                        onOpenQrManage = {
                                            manageInitialTab = 2
                                            currentDest = ShellDestination.MANAGE
                                        },
                                        onOpenReports = { currentDest = ShellDestination.REPORTS },
                                        onOpenHistory = { currentDest = ShellDestination.HISTORY },
                                        onOpenApprovals = { currentDest = ShellDestination.LEAVE },
                                        onSeedDemoData = { workspaceViewModel.seedDemoData(org) },
                                        onFeedback = onShowLocalMessage
                                    )
                                    ShellDestination.ATTENDANCE -> {
                                        val activeSchedule = config.schedules.firstOrNull { it.id == currentMember?.scheduleId }
                                            ?: config.schedules.firstOrNull()
                                        MemberHomeScreen(
                                            userProfile = userProfile,
                                            organization = org,
                                            member = currentMember,
                                            config = config,
                                            attendances = attendances,
                                            isBusy = isActionBusy,
                                            onCheckIn = { method, lat, lon, dist, inFence, qr, selfie, consent ->
                                                val mem = currentMember ?: return@MemberHomeScreen
                                                workspaceViewModel.performCheckIn(
                                                    org = org,
                                                    member = mem,
                                                    schedule = activeSchedule,
                                                    method = method,
                                                    latitude = lat,
                                                    longitude = lon,
                                                    distanceMeters = dist,
                                                    withinGeofence = inFence,
                                                    qrTokenUsed = qr,
                                                    selfiePath = selfie,
                                                    faceConsentGranted = consent
                                                )
                                            },
                                            onCheckOut = { rec, method ->
                                                val mem = currentMember ?: return@MemberHomeScreen
                                                workspaceViewModel.performCheckOut(
                                                    org = org,
                                                    member = mem,
                                                    todayRecord = rec,
                                                    schedule = activeSchedule,
                                                    method = method
                                                )
                                            },
                                            onNotifyLocalMessage = onShowLocalMessage
                                        )
                                    }
                                    ShellDestination.HISTORY -> {
                                        if (isStaff) {
                                            BackHandler { currentDest = ShellDestination.DASHBOARD }
                                        }
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            if (isStaff && !useRail) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = { currentDest = ShellDestination.DASHBOARD }) {
                                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                                    }
                                                    Text("Kembali ke Dashboard", style = MaterialTheme.typography.labelLarge)
                                                }
                                            }
                                            AttendanceHistoryScreen(attendances = attendances)
                                        }
                                    }
                                    ShellDestination.PARENT_MONITOR -> ParentDashboardScreen(
                                        parentUid = userProfile?.userId.orEmpty(),
                                        organization = org,
                                        linkedChildren = members,
                                        childAttendances = attendances,
                                        notifications = notifications,
                                        onFeedback = onShowLocalMessage
                                    )
                                    ShellDestination.LEAVE -> LeaveOvertimeScreen(
                                        organization = org,
                                        member = currentMember,
                                        role = activeRole,
                                        requests = requests,
                                        isBusy = isActionBusy,
                                        onSubmitRequest = { type, start, end, hours, reason ->
                                            val mem = currentMember ?: return@LeaveOvertimeScreen
                                            workspaceViewModel.submitRequest(org, mem, type, start, end, hours, reason)
                                        },
                                        onReviewRequest = { req, decision, note ->
                                            val reviewerName = currentMember?.fullName ?: userProfile?.displayName ?: "Supervisor"
                                            workspaceViewModel.reviewRequest(org, reviewerName, req, decision, note)
                                        }
                                    )
                                    ShellDestination.MANAGE -> OrgManagementScreen(
                                        organization = org,
                                        role = activeRole,
                                        config = config,
                                        members = members,
                                        plans = plans,
                                        transactions = transactions,
                                        initialTab = manageInitialTab,
                                        isBusy = isActionBusy,
                                        onAddRosterMember = { name, code, r, sub, unit, sch, email, phone ->
                                            workspaceViewModel.addRosterMember(org, name, code, r, sub, unit, sch, email, phone)
                                        },
                                        onUpdateMemberAssignment = { mem, newRole, unit, sch, parentUid ->
                                            workspaceViewModel.updateMemberAssignment(org, mem, newRole, unit, sch, parentUid)
                                        },
                                        onAddUnit = { kind, name, code, sup ->
                                            workspaceViewModel.addUnit(kind, name, code, sup)
                                        },
                                        onAddSchedule = { name, start, end, tol, days ->
                                            workspaceViewModel.addSchedule(name, start, end, tol, days)
                                        },
                                        onAddLocation = { name, lat, lon, rad ->
                                            workspaceViewModel.addLocation(name, lat, lon, rad)
                                        },
                                        onRotateDynamicQr = { loc ->
                                            workspaceViewModel.rotateDynamicQr(loc)
                                        },
                                        onUpdatePolicies = { geo, qr, selfie ->
                                            workspaceViewModel.updatePolicies(geo, qr, selfie)
                                        },
                                        onUpgradePlan = { plan, method ->
                                            val actor = currentMember?.fullName ?: userProfile?.displayName ?: "Admin"
                                            workspaceViewModel.upgradePlan(org, actor, plan, method)
                                        },
                                        onFeedback = onShowLocalMessage
                                    )
                                    ShellDestination.REPORTS -> ReportsExportScreen(
                                        organization = org,
                                        config = config,
                                        attendances = attendances,
                                        onBack = if (!useRail) ({ currentDest = ShellDestination.DASHBOARD }) else null,
                                        onFeedback = onShowLocalMessage
                                    )
                                    ShellDestination.PROFILE -> ProfileSettingsScreen(
                                        userProfile = userProfile,
                                        activeOrg = org,
                                        activeRole = activeRole,
                                        memberships = memberships,
                                        auditLogs = auditLogs,
                                        isBusy = isActionBusy || isSessionBusy,
                                        onUpdateProfile = onUpdateProfile,
                                        onSwitchOrganization = onSwitchOrganization,
                                        onOpenOnboarding = onOpenOnboarding,
                                        onOpenSuperAdmin = onOpenSuperAdmin,
                                        onSignOut = onSignOut,
                                        onFeedback = onShowLocalMessage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
