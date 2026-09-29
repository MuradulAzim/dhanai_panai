package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE direction = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE direction = 'OUTGOING' ORDER BY timestamp DESC")
    fun getOutgoingCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE direction = 'INCOMING' ORDER BY timestamp DESC")
    fun getIncomingCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE id = :id LIMIT 1")
    suspend fun getCallById(id: Long): CallEntity?

    @Query("SELECT * FROM calls WHERE callSid = :callSid LIMIT 1")
    suspend fun getCallBySid(callSid: String): CallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<CallEntity>)

    @Update
    suspend fun updateCall(call: CallEntity)

    @Query("DELETE FROM calls WHERE id = :id")
    suspend fun deleteCall(id: Long)

    @Query("DELETE FROM calls")
    suspend fun clearAllCalls()
}
