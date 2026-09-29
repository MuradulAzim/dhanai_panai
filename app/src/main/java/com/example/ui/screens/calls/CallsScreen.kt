package com.example.ui.screens.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallDirection
import com.example.data.local.entity.CallEntity
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallFilter {
    ALL,
    MISSED,
    OUTGOING,
    INCOMING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(viewModel: SecondNumberViewModel) {
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    val allCalls by viewModel.allCalls.collectAsState()

    val filteredCalls = remember(allCalls, selectedFilter) {
        when (selectedFilter) {
            CallFilter.ALL -> allCalls
            CallFilter.MISSED -> allCalls.filter { it.direction == CallDirection.MISSED }
            CallFilter.OUTGOING -> allCalls.filter { it.direction == CallDirection.OUTGOING }
            CallFilter.INCOMING -> allCalls.filter { it.direction == CallDirection.INCOMING }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("calls_screen")
    ) {
        // Filter tabs row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CallFilter.values()) { filter ->
                ElevatedFilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            filter.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }
                )
            }
        }

        if (filteredCalls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhoneDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No ${selectedFilter.name.lowercase()} calls found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    CallLogItem(
                        call = call,
                        onCallClick = { viewModel.startCall(call.remoteNumber) },
                        onMessageClick = { viewModel.openConversation(call.remoteNumber) },
                        onDeleteClick = { viewModel.deleteCall(call.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallLogItem(
    call: CallEntity,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(call.timestamp) { dateFormat.format(Date(call.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallClick() }
            .testTag("call_item_${call.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction Icon Badge
            Surface(
                shape = CircleShape,
                color = when (call.direction) {
                    CallDirection.MISSED -> CallRed.copy(alpha = 0.15f)
                    CallDirection.OUTGOING -> PrimaryBlue.copy(alpha = 0.15f)
                    CallDirection.INCOMING -> CallGreen.copy(alpha = 0.15f)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (call.direction) {
                        CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                        CallDirection.OUTGOING -> Icons.Default.CallMade
                        CallDirection.INCOMING -> Icons.Default.CallReceived
                    }
                    val iconColor = when (call.direction) {
                        CallDirection.MISSED -> CallRed
                        CallDirection.OUTGOING -> PrimaryBlue
                        CallDirection.INCOMING -> CallGreen
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = call.direction.name,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName ?: call.remoteNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (call.direction == CallDirection.MISSED) CallRed else MaterialTheme.colorScheme.onSurface
                )
                if (call.contactName != null) {
                    Text(
                        text = call.remoteNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (call.durationSeconds > 0) {
                        Text(
                            text = " • ${call.durationSeconds / 60}m ${call.durationSeconds % 60}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action shortcuts
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMessageClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onCallClick) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call back",
                        tint = CallGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete call",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
