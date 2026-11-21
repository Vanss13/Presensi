package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrganizationType
import com.example.data.model.UserProfile
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgOnboardingScreen(
    userProfile: UserProfile?,
    isBusy: Boolean,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onCreateOrganization: (
        name: String,
        type: OrganizationType,
        phone: String,
        address: String,
        adminName: String
    ) -> Unit,
    onJoinOrganization: (
        joinCode: String,
        fullName: String,
        memberCode: String,
        joinAsParent: Boolean
    ) -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create Org, 1 = Join Org

    var orgName by remember { mutableStateOf("") }
    var orgType by remember { mutableStateOf(OrganizationType.WORK) }
    var orgPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var orgAddress by remember { mutableStateOf("") }
    var adminName by remember(userProfile?.displayName) { mutableStateOf(userProfile?.displayName ?: "") }

    var joinCode by remember { mutableStateOf("") }
    var memberFullName by remember(userProfile?.displayName) { mutableStateOf(userProfile?.displayName ?: "") }
    var memberCode by remember { mutableStateOf("") }
    var joinAsParent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Organisasi HADIRIN") },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("onboarding_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    if (userProfile?.superAdmin == true) {
                        IconButton(onClick = onOpenSuperAdmin, modifier = Modifier.testTag("open_super_admin_button")) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Platform Super Admin")
                        }
                    }
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("onboarding_sign_out_button")) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar Akun")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HadirinNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Halo, ${userProfile?.displayName ?: "Pengguna"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userProfile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                if (userProfile?.superAdmin == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenSuperAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("super_admin_banner_button")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Dashboard Super Admin SaaS")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("tab_register_org")
                    ) {
                        Text("Daftarkan Organisasi")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("tab_join_org")
                    ) {
                        Text("Gabung Organisasi")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Pilih Jenis Modul Organisasi",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = orgType == OrganizationType.EDU,
                                    onClick = { orgType = OrganizationType.EDU },
                                    label = { Text("HADIRIN EDU") },
                                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("select_edu_module")
                                )
                                FilterChip(
                                    selected = orgType == OrganizationType.WORK,
                                    onClick = { orgType = OrganizationType.WORK },
                                    label = { Text("HADIRIN WORK") },
                                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("select_work_module")
                                )
                            }

                            OutlinedTextField(
                                value = orgName,
                                onValueChange = { orgName = it },
                                label = { Text(if (orgType == OrganizationType.EDU) "Nama Sekolah / Pondok / Yayasan" else "Nama Perusahaan / UMKM") },
                                placeholder = { Text(if (orgType == OrganizationType.EDU) "Contoh: SMA Negeri 1 Nusantara" else "Contoh: PT Karya Digital Indonesia") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_org_name")
                            )

                            OutlinedTextField(
                                value = adminName,
                                onValueChange = { adminName = it },
                                label = { Text("Nama Administrator Utama") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_admin_name")
                            )

                            OutlinedTextField(
                                value = orgPhone,
                                onValueChange = { orgPhone = it },
                                label = { Text("Nomor Telepon / WhatsApp Organisasi") },
                                placeholder = { Text("081234567890") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_org_phone")
                            )

                            OutlinedTextField(
                                value = orgAddress,
                                onValueChange = { orgAddress = it },
                                label = { Text("Alamat Lengkap Organisasi") },
                                placeholder = { Text("Jl. Merdeka No. 12, Jakarta") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_org_address")
                            )

                            Button(
                                onClick = {
                                    onCreateOrganization(orgName, orgType, orgPhone, orgAddress, adminName)
                                },
                                enabled = !isBusy && orgName.trim().length >= 2 && orgPhone.trim().length >= 4 && orgAddress.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("create_org_submit_button")
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Buat Organisasi & Mulai Uji Coba", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Gabung ke Organisasi yang Sudah Ada",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Masukkan Kode Organisasi dari Admin sekolah atau HRD perusahaan Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase() },
                                label = { Text("Kode Organisasi") },
                                placeholder = { Text("Contoh: EDU7F2A1 atau WRK9B4C2") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_join_code")
                            )

                            OutlinedTextField(
                                value = memberFullName,
                                onValueChange = { memberFullName = it },
                                label = { Text("Nama Lengkap") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_join_full_name")
                            )

                            OutlinedTextField(
                                value = memberCode,
                                onValueChange = { memberCode = it },
                                label = { Text("NIS / NIP / ID Anggota (Opsional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_join_member_code")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Gabung sebagai Wali Murid (EDU)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("Aktifkan jika Anda orang tua siswa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = joinAsParent,
                                    onCheckedChange = { joinAsParent = it },
                                    modifier = Modifier.testTag("switch_join_as_parent")
                                )
                            }

                            Button(
                                onClick = {
                                    onJoinOrganization(joinCode, memberFullName, memberCode, joinAsParent)
                                },
                                enabled = !isBusy && joinCode.trim().length >= 4 && memberFullName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("join_org_submit_button")
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gabung Sekarang", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
