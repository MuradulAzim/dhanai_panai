package com.example.ui.screens.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.models.ApiUserDto
import com.example.data.remote.models.RatesAndBankingDto
import com.example.data.remote.models.RechargeRequestDto
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: SecondNumberViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val adminRequests by viewModel.adminRequests.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val adminStats by viewModel.adminStats.collectAsState()
    val ratesAndBanking by viewModel.ratesAndBanking.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("এডমিন কন্ট্রোল প্যানেল", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Second Number Master Network", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAdminDashboard() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Banner
            adminStats?.let { stats ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("মোট ইউজার", style = MaterialTheme.typography.labelSmall)
                            Text("${stats.totalUsers}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("পেন্ডিং রিকোয়েস্ট", style = MaterialTheme.typography.labelSmall)
                            Text("${stats.pendingRequests}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (stats.pendingRequests > 0) CallRed else CallGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("মোট আয় (BDT)", style = MaterialTheme.typography.labelSmall)
                            Text("৳${stats.totalRevenueBdt.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CallGreen)
                        }
                    }
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("রিকোয়েস্ট (${adminRequests.count { it.status == "PENDING" }})") },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("রেট ও ব্যাংকিং") },
                    icon = { Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ইউজার (${adminUsers.size})") },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        viewModel.fetchMyTwilioNumbers()
                        viewModel.searchAvailableTwilioNumbers("US")
                    },
                    text = { Text("টুইলিও স্টক") },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> RequestsTab(adminRequests, onApprove = { viewModel.approveRequest(it) }, onReject = { id, note -> viewModel.rejectRequest(id, note) })
                1 -> RatesTab(ratesAndBanking, onSave = { viewModel.updateAdminRates(it) })
                2 -> UsersTab(adminUsers, onTopup = { key, s, m, n -> viewModel.topupUser(key, s, m, n) })
                3 -> TwilioInventoryTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RequestsTab(
    requests: List<RechargeRequestDto>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit
) {
    val context = LocalContext.current
    var rejectDialogReqId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("টাকা পাওয়া যায়নি বা TrxID ভুল") }

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো পেমেন্ট রিকোয়েস্ট নেই", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (req.status) {
                                    "APPROVED" -> CallGreen.copy(alpha = 0.15f)
                                    "REJECTED" -> CallRed.copy(alpha = 0.15f)
                                    else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = req.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when (req.status) {
                                        "APPROVED" -> CallGreen
                                        "REJECTED" -> CallRed
                                        else -> Color(0xFFB45309)
                                    }
                                )
                            }
                        }

                        Text("যোগাযোগ: ${req.userContact}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "প্যাকেজ: ${req.smsRequested} SMS + ${req.minutesRequested} Min কল = ৳${req.totalAmountBdt.toInt()} BDT",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${req.paymentMethod} • প্রেরক: ${req.senderNumber} • TrxID: ${req.transactionId}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", req.transactionId))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy TrxID", modifier = Modifier.size(14.dp))
                            }
                        }

                        if (!req.screenshotBase64.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📷 স্ক্রিনশট সংযুক্ত রয়েছে", fontSize = 12.sp, color = CallGreen)
                        }

                        if (req.assignedApiKey != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("ইস্যুকৃত Key: ${req.assignedApiKey}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (req.status == "PENDING") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(req.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CallGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অনুমোদন ও Key ইস্যু")
                                }
                                OutlinedButton(
                                    onClick = { rejectDialogReqId = req.id },
                                    modifier = Modifier.weight(0.5f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("বাতিল")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectDialogReqId != null) {
        AlertDialog(
            onDismissRequest = { rejectDialogReqId = null },
            title = { Text("রিকোয়েস্ট বাতিল করার কারণ") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("কারণ / নোট") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectDialogReqId!!, rejectReason)
                        rejectDialogReqId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed)
                ) {
                    Text("বাতিল নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogReqId = null }) { Text("ফিরে যান") }
            }
        )
    }
}

@Composable
fun RatesTab(
    rates: RatesAndBankingDto,
    onSave: (RatesAndBankingDto) -> Unit
) {
    var smsRate by remember(rates) { mutableStateOf(rates.pricePerSmsBdt.toString()) }
    var minRate by remember(rates) { mutableStateOf(rates.pricePerCallMinuteBdt.toString()) }
    var bkash by remember(rates) { mutableStateOf(rates.bkashNumber) }
    var nagad by remember(rates) { mutableStateOf(rates.nagadNumber) }
    var rocket by remember(rates) { mutableStateOf(rates.rocketNumber) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("সার্ভিস চার্জ নির্ধারণ (BDT)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = smsRate,
                        onValueChange = { smsRate = it },
                        label = { Text("প্রতি SMS রেট (টাকা)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minRate,
                        onValueChange = { minRate = it },
                        label = { Text("প্রতি মিনিট কল রেট (টাকা)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("বাংলাদেশি মোবাইল ব্যাংকিং নম্বরসমূহ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("ইউজাররা এই নম্বরগুলোতে টাকা পাঠিয়ে TrxID সাবমিট করবে।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = bkash,
                        onValueChange = { bkash = it },
                        label = { Text("bKash Personal নম্বর") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nagad,
                        onValueChange = { nagad = it },
                        label = { Text("Nagad Personal নম্বর") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rocket,
                        onValueChange = { rocket = it },
                        label = { Text("Rocket নম্বর") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSave(
                        rates.copy(
                            pricePerSmsBdt = smsRate.toDoubleOrNull() ?: rates.pricePerSmsBdt,
                            pricePerCallMinuteBdt = minRate.toDoubleOrNull() ?: rates.pricePerCallMinuteBdt,
                            bkashNumber = bkash.trim(),
                            nagadNumber = nagad.trim(),
                            rocketNumber = rocket.trim()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UsersTab(
    users: List<ApiUserDto>,
    onTopup: (apiKey: String, sms: Int, min: Int, assignedNum: String?) -> Unit
) {
    val context = LocalContext.current
    var topupUser by remember { mutableStateOf<ApiUserDto?>(null) }
    var addSmsInput by remember { mutableStateOf("50") }
    var addMinInput by remember { mutableStateOf("20") }
    var assignedNumInput by remember { mutableStateOf("") }

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো সক্রিয় API ইউজার নেই", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users, key = { it.apiKey }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("নম্বর: ${user.assignedNumber}", fontWeight = FontWeight.SemiBold, color = CallGreen)
                        }

                        Text("মোবাইল: ${user.contactNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ব্যালেন্স: ${user.smsBalance} SMS | ${user.callMinutesBalance} Min কল",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Button(
                                onClick = {
                                    topupUser = user
                                    assignedNumInput = user.assignedNumber
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("+ রিচার্জ", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("API Key: ${user.apiKey}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("API Key", user.apiKey))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy API Key", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (topupUser != null) {
        AlertDialog(
            onDismissRequest = { topupUser = null },
            title = { Text("${topupUser!!.name} - ব্যালেন্স রিচার্জ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addSmsInput,
                        onValueChange = { addSmsInput = it },
                        label = { Text("যোগ করার SMS সংখ্যা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addMinInput,
                        onValueChange = { addMinInput = it },
                        label = { Text("যোগ করার কল মিনিট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = assignedNumInput,
                        onValueChange = { assignedNumInput = it },
                        label = { Text("ভার্চুয়াল ফোন নম্বর অ্যাসাইন করুন") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = addSmsInput.toIntOrNull() ?: 0
                        val m = addMinInput.toIntOrNull() ?: 0
                        onTopup(topupUser!!.apiKey, s, m, assignedNumInput.trim())
                        topupUser = null
                    }
                ) {
                    Text("রিচার্জ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { topupUser = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
fun TwilioInventoryTab(viewModel: SecondNumberViewModel) {
    val context = LocalContext.current
    val myNumbers by viewModel.myTwilioNumbers.collectAsState()
    val availableNumbers by viewModel.availableTwilioNumbers.collectAsState()
    var searchCountry by remember { mutableStateOf("US") }
    var releaseTargetSid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchMyTwilioNumbers()
        viewModel.searchAvailableTwilioNumbers(searchCountry)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Web Portal Link Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অনলাইন রিচার্জ ওয়েব পোর্টাল", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "কাস্টমাররা টার্মিনাল বা অ্যাপ ছাড়াও যেকোনো ব্রাউজার দিয়ে বিকাশ/নগদে নম্বর কিনতে পারে:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val portalUrl = "${viewModel.repository.getServerUrl().trimEnd('/')}/portal"
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(portalUrl, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Portal URL", portalUrl))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Instant Purchase from Twilio (Mobile UI - No Terminal needed!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মোবাইল থেকে সরাসরি টুইলিও নম্বর কিনুন", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Default.Phone, contentDescription = null, tint = CallGreen)
                    }
                    Text(
                        "টার্মিনাল ছাড়াই টুইলিও থেকে তাৎক্ষণিক $1.15 খরচে নতুন ফ্রেশ ভার্চুয়াল নম্বর কিনুন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Country Selection
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("US" to "🇺🇸 USA", "GB" to "🇬🇧 UK", "CA" to "🇨🇦 Canada", "AU" to "🇦🇺 Australia").forEach { (code, label) ->
                            item {
                                FilterChip(
                                    selected = searchCountry == code,
                                    onClick = {
                                        searchCountry = code
                                        viewModel.searchAvailableTwilioNumbers(code)
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Available list from Twilio
                    if (availableNumbers.isEmpty()) {
                        Text("কোনো নম্বর লোড হয়নি। 'পুনরায় খুঁজুন' বাটনে চাপ দিন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        availableNumbers.take(4).forEach { avail ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(avail.friendlyName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${avail.locality ?: avail.region ?: avail.isoCountry} • Voice & SMS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.buyTwilioNumber(avail.phoneNumber, avail.friendlyName)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CallGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("কিনুন ($1.15)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Active Twilio Numbers Stock & 1-Tap Release
        item {
            Text("বর্তমানে সক্রিয় টুইলিও নম্বর স্টক (${myNumbers.size} টি)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("কাস্টমার রিনিউ না করলে এখান থেকে 'বাতিল' করলে টুইলিও আর কোনো মাসিক বিল কাটবে না।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (myNumbers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("কোনো সক্রিয় অতিরিক্ত নম্বর নেই", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(myNumbers, key = { it.sid }) { num ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(num.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("SID: ${num.sid.take(16)}...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedButton(
                            onClick = { releaseTargetSid = num.sid },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নম্বর বাতিল", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (releaseTargetSid != null) {
        AlertDialog(
            onDismissRequest = { releaseTargetSid = null },
            title = { Text("টুইলিও নম্বর বাতিল নিশ্চিতকরণ") },
            text = {
                Text("আপনি কি নিশ্চিত এই নম্বরটি টুইলিও থেকে মুছে ফেলতে চান? এটি মুছে দিলে পরবর্তী মাসে এর জন্য আর কোনো বিল কাটা হবে না।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.releaseTwilioNumber(releaseTargetSid!!)
                        releaseTargetSid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed)
                ) {
                    Text("হ্যাঁ, বাতিল করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { releaseTargetSid = null }) { Text("না") }
            }
        )
    }
}
