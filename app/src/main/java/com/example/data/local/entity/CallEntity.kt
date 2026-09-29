package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallStatus {
    INITIATED,
    RINGING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    BUSY,
    NO_ANSWER
}

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callSid: String? = null,
    val remoteNumber: String,
    val contactName: String? = null,
    val direction: CallDirection,
    val status: CallStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val notes: String? = null
)
