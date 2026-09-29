package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageDirection {
    INCOMING,
    OUTGOING
}

enum class MessageStatus {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    RECEIVED
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageSid: String? = null,
    val conversationNumber: String,
    val senderNumber: String,
    val receiverNumber: String,
    val body: String,
    val direction: MessageDirection,
    val status: MessageStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

data class ConversationSummary(
    val conversationNumber: String,
    val contactName: String? = null,
    val lastMessageBody: String,
    val lastMessageTimestamp: Long,
    val lastMessageDirection: MessageDirection,
    val unreadCount: Int = 0
)
