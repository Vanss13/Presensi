package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillingTransaction
import com.example.data.model.DefaultSubscriptionCatalog
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.repository.SessionRepository
import com.example.data.repository.unwrapFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val repository: SessionRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _superAdminConsoleOpen = MutableStateFlow(false)
    val superAdminConsoleOpen: StateFlow<Boolean> = _superAdminConsoleOpen.asStateFlow()

    private val _onboardingOpen = MutableStateFlow(false)
    val onboardingOpen: StateFlow<Boolean> = _onboardingOpen.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile()
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.unwrapFirestoreMessage("Gagal menyiapkan profil akun.")
            }
        }
    }

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).unwrapFirestoreMessage("Gagal memuat profil pengguna.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val memberships: StateFlow<UiState<List<UserMembership>>> = repository.observeUserMemberships(currentUserId)
        .map<List<UserMembership>, UiState<List<UserMembership>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).unwrapFirestoreMessage("Gagal memuat daftar organisasi.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = repository.observeSubscriptionPlans()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(DefaultSubscriptionCatalog.plans)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DefaultSubscriptionCatalog.plans)

    val allOrganizations: StateFlow<List<Organization>> = userProfile
        .flatMapLatest { state ->
            if (state is UiState.Success && state.data?.superAdmin == true) {
                repository.observeAllOrganizationsForSuperAdmin()
            } else {
                flowOf(emptyList())
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allUsers: StateFlow<List<UserProfile>> = userProfile
        .flatMapLatest { state ->
            if (state is UiState.Success && state.data?.superAdmin == true) {
                repository.observeAllUsersForSuperAdmin()
            } else {
                flowOf(emptyList())
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTransactions: StateFlow<List<BillingTransaction>> = userProfile
        .flatMapLatest { state ->
            if (state is UiState.Success && state.data?.superAdmin == true) {
                repository.observeAllTransactionsForSuperAdmin()
            } else {
                flowOf(emptyList())
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun setSuperAdminConsoleOpen(open: Boolean) {
        _superAdminConsoleOpen.value = open
    }

    fun setOnboardingOpen(open: Boolean) {
        _onboardingOpen.value = open
    }

    fun createOrganization(
        name: String,
        type: OrganizationType,
        phone: String,
        address: String,
        adminName: String,
        latitude: Double = -6.200000,
        longitude: Double = 106.816666
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createOrganization(
                name = name,
                type = type,
                phone = phone,
                address = address,
                adminDisplayName = adminName,
                latitude = latitude,
                longitude = longitude
            )
            _isBusy.value = false
            res.onSuccess {
                _onboardingOpen.value = false
                _superAdminConsoleOpen.value = false
                _feedbackMessage.value = "Organisasi '$name' berhasil dibuat dan aktif!"
            }.onFailure {
                _feedbackMessage.value = it.unwrapFirestoreMessage("Gagal membuat organisasi.")
            }
        }
    }

    fun joinOrganization(
        joinCode: String,
        fullName: String,
        memberCode: String,
        joinAsParent: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinOrganizationWithCode(joinCode, fullName, memberCode, joinAsParent)
            _isBusy.value = false
            res.onSuccess {
                _onboardingOpen.value = false
                _superAdminConsoleOpen.value = false
                _feedbackMessage.value = "Berhasil bergabung ke organisasi!"
            }.onFailure {
                _feedbackMessage.value = it.unwrapFirestoreMessage("Gagal bergabung dengan kode organisasi.")
            }
        }
    }

    fun switchOrganization(orgId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.switchActiveOrganization(orgId)
            _isBusy.value = false
            res.onSuccess {
                _superAdminConsoleOpen.value = false
                _onboardingOpen.value = false
            }.onFailure {
                _feedbackMessage.value = it.unwrapFirestoreMessage("Gagal berpindah organisasi.")
            }
        }
    }

    fun updateProfile(displayName: String, phone: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateUserProfile(displayName, phone)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Profil akun diperbarui."
            }.onFailure {
                _feedbackMessage.value = it.unwrapFirestoreMessage("Gagal memperbarui profil.")
            }
        }
    }

    fun updateSubscriptionPlan(planId: String, monthlyPriceIdr: Long, maxUsers: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateSubscriptionPlan(planId, monthlyPriceIdr, maxUsers)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Harga dan kuota paket $planId diperbarui."
            }.onFailure {
                _feedbackMessage.value = it.unwrapFirestoreMessage("Gagal mengubah paket.")
            }
        }
    }
}
