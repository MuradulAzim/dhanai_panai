package com.example.ui.screens.gemini

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.gemini.ChatMessage
import com.example.data.remote.gemini.ChatbotRole
import com.example.data.remote.gemini.GeminiClient
import com.example.data.remote.gemini.GeminiModel
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.SecondNumberViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(viewModel: SecondNumberViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedModel by remember { mutableStateOf(GeminiModel.FLASH) }
    var selectedRole by remember { mutableStateOf(ChatbotRole.TELECOM_ADVISOR) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Multi-turn chat history state
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "model",
                text = "হ্যালো! আমি আপনার সেকেন্ড নম্বর এআই টেলি-অ্যাসিস্ট্যান্ট। আন্তর্জাতিক ভার্চুয়াল নম্বর, ব্যবসায়িক এসএমএস ড্রাফটিং, কল স্ক্রিপ্ট বা গোপনীয়তা রক্ষায় যেকোনো প্রশ্ন করুন।"
            )
        )
    }

    fun sendMessage(textToSend: String) {
        val query = textToSend.trim()
        if (query.isBlank() || isLoading) return

        val userMsg = ChatMessage(role = "user", text = query)
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val result = GeminiClient.generateChatResponse(
                history = messages.dropLast(1),
                userMessage = query,
                model = selectedModel,
                role = selectedRole
            )

            isLoading = false
            result.onSuccess { responseText ->
                messages.add(ChatMessage(role = "model", text = responseText))
                listState.animateScrollToItem(messages.size - 1)
            }.onFailure { err ->
                messages.add(ChatMessage(role = "model", text = "ত্রুটি: ${err.message ?: "কানেকশন সমস্যা। অনুগ্রহ করে আবার চেষ্টা করুন।"}\n(টিপস: Secrets প্যানেলে আপনার GEMINI_API_KEY সেট করুন)"))
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Gemini AI অ্যাসিস্ট্যান্ট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(selectedRole.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        messages.clear()
                        messages.add(
                            ChatMessage(
                                role = "model",
                                text = "চ্যাট হিস্ট্রি রিসেট করা হয়েছে। আমি কীভাবে আপনাকে সাহায্য করতে পারি?"
                            )
                        )
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .imePadding()
                .testTag("gemini_chat_screen")
        ) {
            // Role & Model Selectors Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // Model Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Model:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GeminiModel.values().forEach { model ->
                                FilterChip(
                                    selected = selectedModel == model,
                                    onClick = { selectedModel = model },
                                    label = { Text(model.displayName.replace("Gemini ", ""), fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Role Selection Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(ChatbotRole.values()) { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.title, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Chat Messages Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg, onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Message", msg.text))
                    })
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Gemini লিখছে...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Quick Prompt Suggestions (when empty or new)
            if (messages.size <= 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptChips = listOf(
                        "ইউএস ক্লায়েন্টের জন্য প্রফেশনাল SMS ড্রাফট করো",
                        "অনলাইনে ভেরিফিকেশনে প্রাইভেসি কীভাবে রক্ষা করবো?",
                        "কল রিসিভ করার সুন্দর গ্রাহক সেবা স্ক্রিপ্ট দাও",
                        "ভার্চুয়াল নম্বরে ওটিপি কোড আসার সুবিধা কী?"
                    )
                    items(promptChips) { chipText ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            onClick = { sendMessage(chipText) }
                        ) {
                            Text(
                                text = chipText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_input_field"),
                        placeholder = { Text("Gemini-কে প্রশ্ন করুন...", fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (inputText.isNotBlank() && !isLoading) PrimaryBlue else Color.LightGray,
                                shape = CircleShape
                            )
                            .testTag("gemini_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onCopy: () -> Unit) {
    val isUser = message.role == "user"
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(28.dp)
                    .padding(bottom = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (!isUser) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
