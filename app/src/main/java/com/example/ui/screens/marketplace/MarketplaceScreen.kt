package com.example.ui.screens.marketplace

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.remote.models.NumberPackageDto
import com.example.ui.screens.settings.PackagePurchaseDialog
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(viewModel: SecondNumberViewModel) {
    val context = LocalContext.current
    val catalog by viewModel.numberCatalog.collectAsState()
    val availableProducts by viewModel.playBillingManager.availableProducts.collectAsState()
    val isBillingReady by viewModel.playBillingManager.isReady.collectAsState()

    var selectedCountry by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showPurchaseDialog by remember { mutableStateOf(false) }

    val filteredList = catalog.filter { item ->
        val matchCountry = selectedCountry == "ALL" || item.countryCode.equals(selectedCountry, ignoreCase = true)
        val matchCategory = selectedCategory == "ALL" || item.category == selectedCategory
        matchCountry && matchCategory
    }

    if (showPurchaseDialog) {
        PackagePurchaseDialog(
            viewModel = viewModel,
            onDismiss = { showPurchaseDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ভার্চুয়াল নম্বর ও ওটিপি স্টোর",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "কল, এসএমএস ও ভেরিফিকেশন নম্বর কিনুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchNumberCatalog() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Catalog")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("marketplace_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Web Portal Announcement Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("অনলাইন রিচার্জ ওয়েব পোর্টাল", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "যেকোনো কম্পিউটার বা মোবাইল ব্রাউজার থেকে সরাসরি বিকাশ/নগদে নম্বর কিনতে আমাদের ওয়েব পোর্টাল ভিজিট করুন।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val url = "${viewModel.repository.getServerUrl().trimEnd('/')}/portal"
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Portal URL", url))
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পোর্টাল", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Country Filter Chips
            item {
                Column {
                    Text("দেশ বেছে নিন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val countries = listOf("ALL" to "🌐 সকল দেশ", "US" to "🇺🇸 United States", "GB" to "🇬🇧 United Kingdom", "CA" to "🇨🇦 Canada", "AU" to "🇦🇺 Australia")
                        items(countries) { (code, label) ->
                            FilterChip(
                                selected = selectedCountry == code,
                                onClick = { selectedCountry = code },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Service Category Filter Chips
            item {
                Column {
                    Text("সার্ভিসের ধরন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = listOf(
                            "ALL" to "সব সার্ভিস",
                            "CALL_AND_SMS" to "📞 ফুল কল + এসএমএস",
                            "WHATSAPP_TELEGRAM" to "💬 হোয়াটসঅ্যাপ ও সোশ্যাল OTP",
                            "BURNER_OTP" to "🔥 ১-ঘণ্টা টেম্পোরারি বার্নার"
                        )
                        items(categories) { (code, label) ->
                            FilterChip(
                                selected = selectedCategory == code,
                                onClick = { selectedCategory = code },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Catalog Items
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("এই ফিল্টারে কোনো নম্বর পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { pkg ->
                    CatalogItemCard(
                        pkg = pkg,
                        onBuyBdt = { showPurchaseDialog = true },
                        onBuyPlay = {
                            val activity = context as? Activity
                            val playProduct = availableProducts.firstOrNull { it.productId == pkg.id }
                            if (activity != null && playProduct != null) {
                                viewModel.playBillingManager.launchPurchaseFlow(activity, playProduct.productDetails)
                            } else {
                                // Default to direct purchase dialog
                                showPurchaseDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogItemCard(
    pkg: NumberPackageDto,
    onBuyBdt: () -> Unit,
    onBuyPlay: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Flag + Title + Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg.flag, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(pkg.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "নমুনা: ${pkg.sampleNumber}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (pkg.category) {
                        "BURNER_OTP" -> Color(0xFFF97316).copy(alpha = 0.12f)
                        "WHATSAPP_TELEGRAM" -> CallGreen.copy(alpha = 0.12f)
                        else -> PrimaryBlue.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = pkg.typeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (pkg.category) {
                            "BURNER_OTP" -> Color(0xFFC2410C)
                            "WHATSAPP_TELEGRAM" -> CallGreen
                            else -> PrimaryBlue
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(pkg.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            // Inclusions Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pkg.smsQuota > 0) {
                    InclusionPill(Icons.Default.Sms, "${pkg.smsQuota} SMS", PrimaryBlue)
                }
                if (pkg.callMinutesQuota > 0) {
                    InclusionPill(Icons.Default.Call, "${pkg.callMinutesQuota} Min", CallGreen)
                }
                InclusionPill(
                    icon = Icons.Default.Timer,
                    label = if (pkg.durationDays > 1) "${pkg.durationDays} দিন মেয়াদ" else "১ ঘণ্টা বার্নার",
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "৳${pkg.priceBdt.toInt()} BDT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = CallGreen
                    )
                    Text("($${pkg.priceUsd} USD)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // bKash / Nagad Button
                    Button(
                        onClick = onBuyBdt,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বিকাশ/নগদ", fontWeight = FontWeight.Bold)
                    }

                    // Google Play Button
                    OutlinedButton(
                        onClick = onBuyPlay,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play IAP")
                    }
                }
            }
        }
    }
}

@Composable
private fun InclusionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
