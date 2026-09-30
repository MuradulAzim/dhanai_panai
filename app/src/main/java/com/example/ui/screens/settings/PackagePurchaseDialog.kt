package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel

@Composable
fun PackagePurchaseDialog(
    viewModel: SecondNumberViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rates by viewModel.ratesAndBanking.collectAsState()

    var userName by remember { mutableStateOf("") }
    var userContact by remember { mutableStateOf("") }

    var selectedSmsOption by remember { mutableStateOf(100) }
    var selectedMinOption by remember { mutableStateOf(30) }

    var paymentMethod by remember { mutableStateOf("BKASH") }
    var senderNumber by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var screenshotAttached by remember { mutableStateOf(false) }

    var isSubmitted by remember { mutableStateOf(false) }
    var submittedRequestId by remember { mutableStateOf("") }

    val calculatedTotal = (selectedSmsOption * rates.pricePerSmsBdt) + (selectedMinOption * rates.pricePerCallMinuteBdt)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isSubmitted) "পেমেন্ট সফলভাবে জমা হয়েছে!" else "ভার্চুয়াল নম্বর ও সার্ভিস প্যাকেজ কিনুন",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CallGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "আপনার রিকোয়েস্ট আইডি: $submittedRequestId",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "এডমিন আপনার পেমেন্ট ভেরিফাই করা মাত্র আপনার এপিআই কি ও ভার্চুয়াল নম্বর স্বয়ংক্রিয়ভাবে সক্রিয় হয়ে যাবে।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Step 1: Package Selection
                    Text("১. প্যাকেজ নির্বাচন করুন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Text("SMS সংখ্যা:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 200, 500).forEach { sms ->
                            FilterChip(
                                selected = selectedSmsOption == sms,
                                onClick = { selectedSmsOption = sms },
                                label = { Text("$sms") }
                            )
                        }
                    }

                    Text("কল মিনিট:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { min ->
                            FilterChip(
                                selected = selectedMinOption == min,
                                onClick = { selectedMinOption = min },
                                label = { Text("${min}m") }
                            )
                        }
                    }

                    // Total Calculation Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোট প্রদেয় মূল্য:", fontWeight = FontWeight.SemiBold)
                            Text(
                                "৳${calculatedTotal.toInt()} BDT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Step 2: Payment Details
                    Text("২. নিচের নম্বরে টাকা পাঠান (Send Money):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PaymentNumberRow("bKash Personal:", rates.bkashNumber, context)
                            PaymentNumberRow("Nagad Personal:", rates.nagadNumber, context)
                            PaymentNumberRow("Rocket:", rates.rocketNumber, context)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Step 3: Transaction Info
                    Text("৩. আপনার পেমেন্টের তথ্য দিন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("আপনার নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = userContact,
                        onValueChange = { userContact = it },
                        label = { Text("আপনার মোবাইল নম্বর") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("পদ্ধতি:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf("BKASH" to "bKash", "NAGAD" to "Nagad", "ROCKET" to "Rocket").forEach { (code, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = paymentMethod == code,
                                    onClick = { paymentMethod = code }
                                )
                                Text(label, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = senderNumber,
                        onValueChange = { senderNumber = it },
                        label = { Text("যে নম্বর থেকে টাকা পাঠিয়েছেন") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text("Transaction ID (TrxID) *") },
                        placeholder = { Text("যেমন: 9X82KD71...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = { screenshotAttached = !screenshotAttached },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (screenshotAttached) "✓ পেমেন্ট স্ক্রিনশট যুক্ত হয়েছে" else "পেমেন্ট স্ক্রিনশট সংযুক্ত করুন")
                    }
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onDismiss) { Text("ঠিক আছে") }
            } else {
                Button(
                    onClick = {
                        if (userContact.isNotBlank() && transactionId.isNotBlank()) {
                            viewModel.submitPackageRequest(
                                userName = if (userName.isBlank()) "App User" else userName.trim(),
                                userContact = userContact.trim(),
                                smsRequested = selectedSmsOption,
                                minutesRequested = selectedMinOption,
                                totalAmountBdt = calculatedTotal,
                                paymentMethod = paymentMethod,
                                senderNumber = senderNumber.trim(),
                                transactionId = transactionId.trim(),
                                screenshotBase64 = if (screenshotAttached) "sample_screenshot_verified" else null
                            ) { id ->
                                submittedRequestId = id
                                isSubmitted = true
                            }
                        }
                    },
                    enabled = userContact.isNotBlank() && transactionId.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("রিকোয়েস্ট সাবমিট করুন")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) { Text("বাতিল") }
            }
        }
    )
}

@Composable
private fun PaymentNumberRow(label: String, number: String, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(number, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, number))
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
        }
    }
}
