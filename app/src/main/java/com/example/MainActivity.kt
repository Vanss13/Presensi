package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.OrgRole
import com.example.data.model.UiState
import com.example.data.repository.SessionRepository
import com.example.data.repository.WorkspaceRepository
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOutUser
import com.example.service.NotificationService
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DemoAppShell
import com.example.ui.screens.HadirinAppShell
import com.example.ui.screens.OrgOnboardingScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.theme.HadirinTheme
import com.example.viewmodel.DemoSandboxController
import com.example.viewmodel.SessionViewModel
import com.example.viewmodel.WorkspaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationService.ensureChannel(this)
        setContent {
            HadirinTheme {
                HadirinRoot()
            }
        }
    }
}

@Composable
private fun HadirinRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val firebaseUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var demoModeActive by remember { mutableStateOf(false) }

    val user = firebaseUser
    if (user == null) {
        if (demoModeActive) {
            val demoController = remember { DemoSandboxController() }
            DemoAppShell(
                controller = demoController,
                onExitDemo = { demoModeActive = false }
            )
        } else {
            AuthScreen(
                onAuthSuccess = { demoModeActive = false },
                onEnterDemoMode = { demoModeActive = true }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedHadirinFlow(
                user = user,
                onSignOut = {
                    demoModeActive = false
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedHadirinFlow(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var localMessage by remember { mutableStateOf<String?>(null) }

    val sessionRepository = remember(context) { SessionRepository(context.applicationContext) }
    val workspaceRepository = remember(context) { WorkspaceRepository(context.applicationContext) }

    val sessionViewModel: SessionViewModel = viewModel(
        key = "session_${user.uid}",
        factory = viewModelFactory {
            addInitializer(SessionViewModel::class) {
                SessionViewModel(sessionRepository, user.uid)
            }
        }
    )

    val userProfileState by sessionViewModel.userProfile.collectAsStateWithLifecycle()
    val membershipsState by sessionViewModel.memberships.collectAsStateWithLifecycle()
    val plans by sessionViewModel.subscriptionPlans.collectAsStateWithLifecycle()
    val allOrgs by sessionViewModel.allOrganizations.collectAsStateWithLifecycle()
    val allUsers by sessionViewModel.allUsers.collectAsStateWithLifecycle()
    val allTransactions by sessionViewModel.allTransactions.collectAsStateWithLifecycle()
    val isSessionBusy by sessionViewModel.isBusy.collectAsStateWithLifecycle()
    val sessionFeedback by sessionViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val superAdminOpen by sessionViewModel.superAdminConsoleOpen.collectAsStateWithLifecycle()
    val onboardingOpen by sessionViewModel.onboardingOpen.collectAsStateWithLifecycle()

    LaunchedEffect(sessionFeedback) {
        val msg = sessionFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        sessionViewModel.clearFeedback()
    }

    LaunchedEffect(localMessage) {
        val msg = localMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        localMessage = null
    }

    val profile = (userProfileState as? UiState.Success)?.data
    val memberships = (membershipsState as? UiState.Success)?.data.orEmpty()

    if (userProfileState is UiState.Loading || membershipsState is UiState.Loading) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val activeMembership = memberships.firstOrNull { it.organizationId == profile?.activeOrganizationId }
        ?: memberships.firstOrNull()

    val showSuperAdmin = superAdminOpen || (profile?.superAdmin == true && memberships.isEmpty() && !onboardingOpen)
    val showOnboarding = onboardingOpen || (!showSuperAdmin && activeMembership == null)

    when {
        showSuperAdmin && profile?.superAdmin == true -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    SuperAdminScreen(
                        organizations = allOrgs,
                        users = allUsers,
                        plans = plans,
                        transactions = allTransactions,
                        isBusy = isSessionBusy,
                        canGoBack = activeMembership != null,
                        onBack = { sessionViewModel.setSuperAdminConsoleOpen(false) },
                        onOpenOrgSetup = {
                            sessionViewModel.setSuperAdminConsoleOpen(false)
                            sessionViewModel.setOnboardingOpen(true)
                        },
                        onUpdateSubscriptionPlan = { planId, price, maxUsers ->
                            sessionViewModel.updateSubscriptionPlan(planId, price, maxUsers)
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }

        showOnboarding -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    OrgOnboardingScreen(
                        userProfile = profile,
                        isBusy = isSessionBusy,
                        canNavigateBack = activeMembership != null,
                        onBack = { sessionViewModel.setOnboardingOpen(false) },
                        onCreateOrganization = { name, type, phone, address, adminName ->
                            sessionViewModel.createOrganization(name, type, phone, address, adminName)
                        },
                        onJoinOrganization = { code, fullName, memberCode, joinAsParent ->
                            sessionViewModel.joinOrganization(code, fullName, memberCode, joinAsParent)
                        },
                        onOpenSuperAdmin = {
                            sessionViewModel.setOnboardingOpen(false)
                            sessionViewModel.setSuperAdminConsoleOpen(true)
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }

        activeMembership != null -> {
            val activeOrgId = activeMembership.organizationId
            val activeRole = OrgRole.fromRaw(activeMembership.role)

            val workspaceViewModel: WorkspaceViewModel = viewModel(
                key = "ws_${user.uid}_${activeOrgId}_${activeRole.name}",
                factory = viewModelFactory {
                    addInitializer(WorkspaceViewModel::class) {
                        WorkspaceViewModel(workspaceRepository, activeOrgId, user.uid, activeRole)
                    }
                }
            )

            val wsActionMessage by workspaceViewModel.actionMessage.collectAsStateWithLifecycle()
            LaunchedEffect(wsActionMessage) {
                val msg = wsActionMessage ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(msg)
                workspaceViewModel.clearMessage()
            }

            HadirinAppShell(
                userProfile = profile,
                memberships = memberships,
                plans = plans,
                activeRole = activeRole,
                workspaceViewModel = workspaceViewModel,
                snackbarHostState = snackbarHostState,
                isSessionBusy = isSessionBusy,
                onSwitchOrganization = { orgId -> sessionViewModel.switchOrganization(orgId) },
                onOpenOnboarding = { sessionViewModel.setOnboardingOpen(true) },
                onOpenSuperAdmin = { sessionViewModel.setSuperAdminConsoleOpen(true) },
                onUpdateProfile = { name, phone -> sessionViewModel.updateProfile(name, phone) },
                onSignOut = onSignOut,
                onShowLocalMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
            )
        }
    }
}
