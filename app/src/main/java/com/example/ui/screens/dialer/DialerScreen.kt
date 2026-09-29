package com.example.ui.screens.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel

data class KeypadButton(
    val digit: String,
    val letters: String
)

val KeypadRows = listOf(
    listOf(KeypadButton("1", ""), KeypadButton("2", "ABC"), KeypadButton("3", "DEF")),
    listOf(KeypadButton("4", "GHI"), KeypadButton("5", "JKL"), KeypadButton("6", "MNO")),
    listOf(KeypadButton("7", "PQRS"), KeypadButton("8", "TUV"), KeypadButton("9", "WXYZ")),
    listOf(KeypadButton("*", ""), KeypadButton("0", "+"), KeypadButton("#", ""))
)

val CountryCodePresets = listOf(
    "+1" to "US/CA",
    "+44" to "UK",
    "+49" to "DE",
    "+33" to "FR",
    "+61" to "AU",
    "+81" to "JP",
    "+91" to "IN"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(viewModel: SecondNumberViewModel) {
    val dialedNumber by viewModel.dialedNumber.collectAsState()
    val twilioNumber by viewModel.twilioPhoneNumber.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Caller ID badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Twilio line",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Outbound Line: $twilioNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick country code prefix chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            items(CountryCodePresets) { (code, country) ->
                FilterChip(
                    selected = dialedNumber.startsWith(code),
                    onClick = {
                        if (!dialedNumber.startsWith(code)) {
                            viewModel.setDialedNumber(code + dialedNumber.replace(Regex("^\\+\\d{1,3}"), ""))
                        }
                    },
                    label = { Text("$code ($country)") }
                )
            }
        }

        // Dialed Number Display Area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = if (dialedNumber.isEmpty()) "Enter phone number" else dialedNumber,
                style = if (dialedNumber.length > 12) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (dialedNumber.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = dialedNumber.isNotEmpty()) {
                Text(
                    text = "Long press 0 for + • International format supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in KeypadRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (btn in row) {
                        KeypadCell(
                            digit = btn.digit,
                            letters = btn.letters,
                            onClick = { viewModel.appendDigit(btn.digit) },
                            onLongClick = {
                                if (btn.digit == "0") {
                                    viewModel.appendDigit("+")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Action Row: Call Button and Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder spacer to balance layout
            Box(modifier = Modifier.size(56.dp))

            // Main Call FAB Button
            FilledIconButton(
                onClick = { viewModel.startCall() },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("call_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = CallGreen,
                    contentColor = Color.White
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Start Call",
                    modifier = Modifier.size(34.dp)
                )
            }

            // Backspace Button
            if (dialedNumber.isNotEmpty()) {
                FilledIconButton(
                    onClick = { viewModel.deleteDigit() },
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("backspace_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Delete digit",
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadCell(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
