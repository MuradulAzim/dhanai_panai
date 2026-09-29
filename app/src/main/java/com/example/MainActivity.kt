package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.screens.calls.CallsScreen
import com.example.ui.screens.dialer.ActiveCallOverlay
import com.example.ui.screens.dialer.DialerScreen
import com.example.ui.screens.messages.ConversationScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.SecondNumberTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.SecondNumberViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: SecondNumberViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            SecondNumberTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("EXTRA_INCOMING_CALL", false)) {
                val callerNumber = it.getStringExtra("EXTRA_CALLER_NUMBER") ?: "+1 (555) 000-0000"
                viewModel.simulateIncomingCall(callerNumber)
            }
            it.getStringExtra("EXTRA_OPEN_CONVERSATION")?.let { conversationNumber ->
                viewModel.selectTab(AppNavTab.MESSAGES)
                viewModel.openConversation(conversationNumber)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: SecondNumberViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val isConnected by viewModel.isServerConnected.collectAsState()
    val totalUnread by viewModel.totalUnreadCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request permissions for notifications & audio
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedConversation != null) {
            // Full conversation thread view
            ConversationScreen(
                phoneNumber = selectedConversation!!,
                viewModel = viewModel
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Second Number",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Connection pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isConnected) CallGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (isConnected) CallGreen else CallRed,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isConnected) "Live" else "Local",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isConnected) CallGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppNavTab.DIALER,
                            onClick = { viewModel.selectTab(AppNavTab.DIALER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Keypad"
                                )
                            },
                            label = { Text("Dialer") },
                            modifier = Modifier.testTag("nav_dialer")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.CALLS,
                            onClick = { viewModel.selectTab(AppNavTab.CALLS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Calls"
                                )
                            },
                            label = { Text("Calls") },
                            modifier = Modifier.testTag("nav_calls")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.MESSAGES,
                            onClick = { viewModel.selectTab(AppNavTab.MESSAGES) },
                            icon = {
                                if (totalUnread > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text("$totalUnread") }
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Messages"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Messages"
                                    )
                                }
                            },
                            label = { Text("Messages") },
                            modifier = Modifier.testTag("nav_messages")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.SETTINGS,
                            onClick = { viewModel.selectTab(AppNavTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        AppNavTab.DIALER -> DialerScreen(viewModel = viewModel)
                        AppNavTab.CALLS -> CallsScreen(viewModel = viewModel)
                        AppNavTab.MESSAGES -> MessagesScreen(viewModel = viewModel)
                        AppNavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Fullscreen Active Call Overlay
        AnimatedVisibility(
            visible = activeCall != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeCall?.let { call ->
                ActiveCallOverlay(callState = call, viewModel = viewModel)
            }
        }
    }
}
