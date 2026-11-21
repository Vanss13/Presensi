package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingTransaction
import com.example.data.model.Organization
import com.example.data.model.OrganizationType
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HadirinKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.HadirinBlue
import com.example.ui.theme.HadirinNavy
import com.example.ui.theme.HadirinSuccess
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    organizations: List<Organization>,
    users: List<UserProfile>,
    plans: List<SubscriptionPlan>,
    transactions: List<BillingTransaction>,
    isBusy: Boolean,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenOrgSetup: () -> Unit,
    onUpdateSubscriptionPlan: (planId: String, priceIdr: Long, maxUsers: Long) -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler(enabled = canGoBack) { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var editPriceText by remember { mutableStateOf("") }
    var editMaxUsersText by remember { mutableStateOf("") }

    val paidOrgCount = organizations.count { it.subscriptionPlanId != "FREE" && it.subscriptionStatus == "ACTIVE" }
    val totalRevenue = transactions.filter { it.status == "PAID" }.sumOf { it.amountIdr }

    if (editingPlan != null) {
        val plan = editingPlan!!
        AlertDialog(
            onDismissRequest = { editingPlan = null },
            title = { Text("Ubah Paket ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Bulanan (IDR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_price_input")
                    )
                    OutlinedTextField(
                        value = editMaxUsersText,
                        onValueChange = { editMaxUsersText = it.filter { c -> c.isDigit() } },
                        label = { Text("Kuota Maksimal Pengguna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_quota_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = editPriceText.toLongOrNull() ?: plan.monthlyPriceIdr
                        val maxUsers = editMaxUsersText.toLongOrNull() ?: plan.maxUsers
                        onUpdateSubscriptionPlan(plan.id, price, maxUsers)
                        editingPlan = null
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = HadirinBlue, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_edit_plan_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlan = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin SaaS HADIRIN") },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("super_admin_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("super_admin_sign_out_button")) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HadirinKpiCard(
                            title = "Total Organisasi",
                            value = organizations.size.toString(),
                            subtitle = "$paidOrgCount berbayar aktif",
                            icon = Icons.Default.Apartment,
                            accentColor = HadirinNavy,
                            modifier = Modifier.weight(1f)
                        )
                        HadirinKpiCard(
                            title = "Total Pengguna",
                            value = users.size.toString(),
                            subtitle = "Akun terdaftar",
                            icon = Icons.Default.Groups,
                            accentColor = HadirinBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HadirinKpiCard(
                            title = "Pendapatan Tercatat",
                            value = formatRupiah(totalRevenue),
                            subtitle = "${transactions.size} transaksi invoice",
                            icon = Icons.Default.Payments,
                            accentColor = HadirinSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        HadirinKpiCard(
                            title = "Paket Aktif",
                            value = plans.size.toString(),
                            subtitle = "Katalog langganan",
                            icon = Icons.Default.WorkspacePremium,
                            accentColor = HadirinBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenOrgSetup,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("super_admin_create_org_button")
                    ) {
                        Text("Daftarkan / Gabung Organisasi")
                    }
                }
            }

            item {
                val tabs = listOf("Organisasi", "Pengguna", "Paket", "Transaksi")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, tabs.size),
                            modifier = Modifier.testTag("sa_tab_$idx")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (organizations.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Apartment,
                                title = "Belum Ada Organisasi",
                                description = "Organisasi yang mendaftar di platform akan muncul secara real-time di sini."
                            )
                        }
                    } else {
                        items(organizations, key = { it.id }) { org ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(org.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        StatusBadge(label = org.subscriptionPlanId, statusKey = org.subscriptionStatus)
                                    }
                                    Text(
                                        text = "${OrganizationType.fromRaw(org.type).moduleName} • Kode: ${org.joinCode} • Limit ${org.memberLimit}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HadirinBlue
                                    )
                                    Text(org.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    items(users, key = { it.id }) { u ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    label = if (u.superAdmin) "SUPER ADMIN" else "USER",
                                    statusKey = if (u.superAdmin) "ACTIVE" else "INFO"
                                )
                            }
                        }
                    }
                }
                2 -> {
                    items(plans, key = { it.id }) { plan ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${plan.name} (${formatRupiah(plan.monthlyPriceIdr)}/bln)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Kuota: ${plan.maxUsers} pengguna", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = {
                                        editingPlan = plan
                                        editPriceText = plan.monthlyPriceIdr.toString()
                                        editMaxUsersText = plan.maxUsers.toString()
                                    },
                                    modifier = Modifier.testTag("edit_plan_${plan.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ubah")
                                }
                            }
                        }
                    }
                }
                3 -> {
                    if (transactions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.ReceiptLong,
                                title = "Belum Ada Transaksi",
                                description = "Pembayaran upgrade paket dari organisasi akan tercatat di sini."
                            )
                        }
                    } else {
                        items(transactions, key = { it.id }) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${tx.organizationName} • ${tx.planName}", style = MaterialTheme.typography.titleSmall)
                                        Text("${tx.invoiceNumber} • ${formatRupiah(tx.amountIdr)} (${tx.paymentMethod})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusBadge(label = tx.status, statusKey = tx.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
