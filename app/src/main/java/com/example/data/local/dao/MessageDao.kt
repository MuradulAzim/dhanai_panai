package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationNumber = :conversationNumber ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationNumber: String): Flow<List<MessageEntity>>

    @Query("SELECT DISTINCT conversationNumber FROM messages ORDER BY timestamp DESC")
    fun getAllConversationNumbers(): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE conversationNumber = :conversationNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationNumber: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE conversationNumber = :conversationNumber AND isRead = 0 AND direction = 'INCOMING'")
    suspend fun getUnreadCount(conversationNumber: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND direction = 'INCOMING'")
    fun getTotalUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationNumber = :conversationNumber")
    suspend fun markConversationAsRead(conversationNumber: String)

    @Query("DELETE FROM messages WHERE conversationNumber = :conversationNumber")
    suspend fun deleteConversation(conversationNumber: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
