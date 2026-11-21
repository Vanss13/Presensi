package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrgRole
import com.example.data.model.OrganizationType
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HadirinBlueSoft
import com.example.ui.theme.HadirinNavy
import com.example.viewmodel.DemoSandboxController

private enum class DemoDestination(
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
fun DemoAppShell(
    controller: DemoSandboxController,
    onExitDemo: () -> Unit
) {
    val profile by controller.userProfile.collectAsStateWithLifecycle()
    val bundles by controller.bundles.collectAsStateWithLifecycle()
    val activeOrgId by controller.activeOrgId.collectAsStateWithLifecycle()
    val activeRole by controller.activeRole.collectAsStateWithLifecycle()
    val plans by controller.plans.collectAsStateWithLifecycle()
    val transactions by controller.transactions.collectAsStateWithLifecycle()
    val superAdminOpen by controller.superAdminOpen.collectAsStateWithLifecycle()
    val onboardingOpen by controller.onboardingOpen.collectAsStateWithLifecycle()
    val feedback by controller.feedback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        controller.clearFeedback()
    }

    val bundle = bundles[activeOrgId] ?: bundles.values.first()
    val org = bundle.organization
    val memberships = remember(bundles, activeRole) { controller.getMemberships() }

    if (superAdminOpen) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                SuperAdminScreen(
                    organizations = bundles.values.map { it.organization },
                    users = listOf(profile),
                    plans = plans,
                    transactions = transactions,
                    isBusy = false,
                    canGoBack = true,
                    onBack = { controller.setSuperAdminOpen(false) },
                    onOpenOrgSetup = {
                        controller.setSuperAdminOpen(false)
                        controller.setOnboardingOpen(true)
                    },
                    onUpdateSubscriptionPlan = { id, price, maxUsers ->
                        controller.updateSubscriptionPlan(id, price, maxUsers)
                    },
                    onSignOut = onExitDemo
                )
            }
        }
        return
    }

    if (onboardingOpen) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                OrgOnboardingScreen(
                    userProfile = profile,
                    isBusy = false,
                    canNavigateBack = true,
                    onBack = { controller.setOnboardingOpen(false) },
                    onCreateOrganization = { name, type, phone, address, adminName ->
                        controller.createOrganization(name, type, phone, address, adminName)
                    },
                    onJoinOrganization = { code, fullName, memCode, asParent ->
                        controller.joinOrganization(code, fullName, memCode, asParent)
                    },
                    onOpenSuperAdmin = {
                        controller.setOnboardingOpen(false)
                        controller.setSuperAdminOpen(true)
                    },
                    onSignOut = onExitDemo
                )
            }
        }
        return
    }

    val isStaff = activeRole == OrgRole.ORG_ADMIN || activeRole == OrgRole.SUPERVISOR
    val primaryTabs = remember(activeRole) {
        when (activeRole) {
            OrgRole.ORG_ADMIN, OrgRole.SUPERVISOR -> listOf(
                DemoDestination.DASHBOARD,
                DemoDestination.ATTENDANCE,
                DemoDestination.LEAVE,
                DemoDestination.MANAGE,
                DemoDestination.PROFILE
            )
            OrgRole.MEMBER -> listOf(
                DemoDestination.ATTENDANCE,
                DemoDestination.HISTORY,
                DemoDestination.LEAVE,
                DemoDestination.PROFILE
            )
            OrgRole.PARENT -> listOf(
                DemoDestination.PARENT_MONITOR,
                DemoDestination.LEAVE,
                DemoDestination.PROFILE
            )
        }
    }

    var currentDest by remember(activeRole) { mutableStateOf(primaryTabs.first()) }
    var manageTab by remember { mutableIntStateOf(0) }
    var showOrgSwitcher by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(false) }

    val unreadCount = bundle.notifications.count { !it.readFlag }
    val currentMember = bundle.members.firstOrNull { it.userId == profile.userId } ?: bundle.members.firstOrNull()

    if (showNotifs) {
        AlertDialog(
            onDismissRequest = { showNotifs = false },
            title = { Text("Pusat Notifikasi (${bundle.notifications.size})") },
            text = {
                if (bundle.notifications.isEmpty()) {
                    Text("Belum ada notifikasi di organisasi ini.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bundle.notifications, key = { it.id }) { n ->
                            Card(
                                onClick = { controller.markNotificationRead(n.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (n.readFlag) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(n.title, style = MaterialTheme.typography.titleSmall)
                                    Text(n.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifs = false }) { Text("Tutup") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(org.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "UJI COBA • ${OrganizationType.fromRaw(org.type).moduleName} • ${activeRole.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { controller.setSuperAdminOpen(true) },
                        modifier = Modifier.testTag("demo_super_admin_btn")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Super Admin")
                    }
                    if (isStaff) {
                        IconButton(onClick = { currentDest = DemoDestination.REPORTS }) {
                            Icon(Icons.Default.Assessment, contentDescription = "Laporan PDF & CSV")
                        }
                    }
                    IconButton(onClick = { showNotifs = true }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                        }
                    }
                    Box {
                        IconButton(onClick = { showOrgSwitcher = true }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Pindah Organisasi")
                        }
                        DropdownMenu(
                            expanded = showOrgSwitcher,
                            onDismissRequest = { showOrgSwitcher = false }
                        ) {
                            bundles.values.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text("${b.organization.name} (${OrganizationType.fromRaw(b.organization.type).moduleName})") },
                                    onClick = {
                                        showOrgSwitcher = false
                                        controller.switchOrganization(b.organization.id)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Buat / Gabung Organisasi") },
                                onClick = {
                                    showOrgSwitcher = false
                                    controller.setOnboardingOpen(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Kembali ke Halaman Login Google") },
                                onClick = {
                                    showOrgSwitcher = false
                                    onExitDemo()
                                }
                            )
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
                                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val useRail = maxWidth >= 600.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    val railTabs = if (isStaff) primaryTabs + listOf(DemoDestination.REPORTS, DemoDestination.HISTORY) else primaryTabs
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        railTabs.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Quick Demo Role & Module Switcher Strip
                    Surface(
                        color = HadirinBlueSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = "Mode Coba:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HadirinNavy
                                )
                            }
                            items(bundles.values.toList()) { b ->
                                FilterChip(
                                    selected = b.organization.id == org.id,
                                    onClick = { controller.switchOrganization(b.organization.id) },
                                    label = {
                                        Text(
                                            if (b.organization.type == OrganizationType.EDU.name) "EDU: ${b.organization.name}"
                                            else "WORK: ${b.organization.name}",
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                            items(OrgRole.entries) { role ->
                                FilterChip(
                                    selected = activeRole == role,
                                    onClick = { controller.switchRole(role) },
                                    label = { Text(role.label, maxLines = 1) },
                                    modifier = Modifier.testTag("demo_role_${role.name}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentDest) {
                            DemoDestination.DASHBOARD -> OrgDashboardScreen(
                                organization = org,
                                config = bundle.config,
                                members = bundle.members,
                                attendances = bundle.attendances,
                                requests = bundle.requests,
                                isBusy = false,
                                onOpenQrManage = {
                                    manageTab = 2
                                    currentDest = DemoDestination.MANAGE
                                },
                                onOpenReports = { currentDest = DemoDestination.REPORTS },
                                onOpenHistory = { currentDest = DemoDestination.HISTORY },
                                onOpenApprovals = { currentDest = DemoDestination.LEAVE },
                                onSeedDemoData = { controller.seedExtraSampleData() },
                                onFeedback = { /* handled via controller */ }
                            )
                            DemoDestination.ATTENDANCE -> {
                                val schedule = bundle.config.schedules.firstOrNull { it.id == currentMember?.scheduleId }
                                    ?: bundle.config.schedules.firstOrNull()
                                MemberHomeScreen(
                                    userProfile = profile,
                                    organization = org,
                                    member = currentMember,
                                    config = bundle.config,
                                    attendances = bundle.attendances,
                                    isBusy = false,
                                    onCheckIn = { method, lat, lon, dist, inFence, qr, selfie, consent ->
                                        controller.performCheckIn(method, lat, lon, dist, inFence, qr, selfie, consent)
                                    },
                                    onCheckOut = { rec, method ->
                                        controller.performCheckOut(rec, method)
                                    },
                                    onNotifyLocalMessage = { /* optional local snackbar */ }
                                )
                            }
                            DemoDestination.HISTORY -> {
                                if (isStaff) {
                                    BackHandler { currentDest = DemoDestination.DASHBOARD }
                                }
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (isStaff && !useRail) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { currentDest = DemoDestination.DASHBOARD }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                            }
                                            Text("Kembali ke Dashboard", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    AttendanceHistoryScreen(attendances = bundle.attendances)
                                }
                            }
                            DemoDestination.PARENT_MONITOR -> ParentDashboardScreen(
                                parentUid = profile.userId,
                                organization = org,
                                linkedChildren = bundle.members.filter { it.parentUid == profile.userId },
                                childAttendances = bundle.attendances.filter { it.parentUid == profile.userId },
                                notifications = bundle.notifications,
                                onFeedback = {}
                            )
                            DemoDestination.LEAVE -> LeaveOvertimeScreen(
                                organization = org,
                                member = currentMember,
                                role = activeRole,
                                requests = bundle.requests,
                                isBusy = false,
                                onSubmitRequest = { type, start, end, hours, reason ->
                                    controller.submitRequest(type, start, end, hours, reason)
                                },
                                onReviewRequest = { req, decision, note ->
                                    controller.reviewRequest(req, decision, note)
                                }
                            )
                            DemoDestination.MANAGE -> OrgManagementScreen(
                                organization = org,
                                role = activeRole,
                                config = bundle.config,
                                members = bundle.members,
                                plans = plans,
                                transactions = transactions.filter { it.organizationId == org.id },
                                initialTab = manageTab,
                                isBusy = false,
                                onAddRosterMember = { n, c, r, s, u, sch, e, p ->
                                    controller.addRosterMember(n, c, r, s, u, sch, e, p)
                                },
                                onUpdateMemberAssignment = { m, r, u, sch, p ->
                                    controller.updateMemberAssignment(m, r, u, sch, p)
                                },
                                onAddUnit = { k, n, c, sup -> controller.addUnit(k, n, c, sup) },
                                onAddSchedule = { n, st, en, t, d -> controller.addSchedule(n, st, en, t, d) },
                                onAddLocation = { n, lat, lon, rad -> controller.addLocation(n, lat, lon, rad) },
                                onRotateDynamicQr = { loc -> controller.rotateDynamicQr(loc) },
                                onUpdatePolicies = { g, q, s -> controller.updatePolicies(g, q, s) },
                                onUpgradePlan = { p, m -> controller.upgradePlan(p, m) },
                                onFeedback = {}
                            )
                            DemoDestination.REPORTS -> ReportsExportScreen(
                                organization = org,
                                config = bundle.config,
                                attendances = bundle.attendances,
                                onBack = if (!useRail) ({ currentDest = DemoDestination.DASHBOARD }) else null,
                                onFeedback = {}
                            )
                            DemoDestination.PROFILE -> ProfileSettingsScreen(
                                userProfile = profile,
                                activeOrg = org,
                                activeRole = activeRole,
                                memberships = memberships,
                                auditLogs = bundle.auditLogs,
                                isBusy = false,
                                onUpdateProfile = { name, phone -> controller.updateProfile(name, phone) },
                                onSwitchOrganization = { orgId -> controller.switchOrganization(orgId) },
                                onOpenOnboarding = { controller.setOnboardingOpen(true) },
                                onOpenSuperAdmin = { controller.setSuperAdminOpen(true) },
                                onSignOut = onExitDemo,
                                onFeedback = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
