package com.example.ui.screens.dialer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallStatus
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ActiveCallUiState
import com.example.ui.viewmodel.SecondNumberViewModel

@Composable
fun ActiveCallOverlay(
    callState: ActiveCallUiState,
    viewModel: SecondNumberViewModel
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("active_call_overlay"),
        color = Slate900.copy(alpha = 0.97f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Call Status Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = if (callState.isIncoming) "Incoming Second Number Call" else "Twilio Outgoing Call",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = callState.phoneNumber,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = when (callState.status) {
                    CallStatus.RINGING -> if (callState.isIncoming) "Ringing on Twilio Line..." else "Calling / Ringing..."
                    CallStatus.IN_PROGRESS -> {
                        val minutes = callState.durationSeconds / 60
                        val seconds = callState.durationSeconds % 60
                        String.format("%02d:%02d", minutes, seconds)
                    }
                    CallStatus.COMPLETED -> "Call Ended"
                    else -> "Connecting..."
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (callState.status == CallStatus.IN_PROGRESS) CallGreen else Color.LightGray
                )
            }

            // Animated Caller Avatar
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(if (callState.status == CallStatus.RINGING) pulseScale else 1f)
                        .background(
                            color = PrimaryBlue.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            color = PrimaryBlue.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Active Call",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // In-Call Controls or Incoming Call Controls
            if (callState.isIncoming && callState.status == CallStatus.RINGING) {
                // Accept / Decline Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { viewModel.rejectIncomingCall() },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("decline_call_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = CallRed,
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = Color.White, fontSize = 14.sp)
                    }

                    // Answer Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { viewModel.answerIncomingCall() },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("answer_call_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = CallGreen,
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Answer", color = Color.White, fontSize = 14.sp)
                    }
                }
            } else {
                // In-Call Action Grid (Mute, Keypad, Speaker) and End Call FAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mute button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier.size(60.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (callState.isMuted) Color.White else Color.DarkGray,
                                    contentColor = if (callState.isMuted) Slate900 else Color.White
                                ),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute"
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (callState.isMuted) "Unmute" else "Mute", color = Color.White, fontSize = 12.sp)
                        }

                        // Speaker button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = { viewModel.toggleSpeaker() },
                                modifier = Modifier.size(60.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (callState.isSpeaker) Color.White else Color.DarkGray,
                                    contentColor = if (callState.isSpeaker) Slate900 else Color.White
                                ),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker"
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (callState.isSpeaker) "Earpiece" else "Speaker", color = Color.White, fontSize = 12.sp)
                        }

                        // Keypad toggle placeholder
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = { /* DTMF Keypad */ },
                                modifier = Modifier.size(60.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.DarkGray,
                                    contentColor = Color.White
                                ),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Keypad"
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Keypad", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Hang up button
                    FilledIconButton(
                        onClick = { viewModel.hangupCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("end_call_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CallRed,
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}
