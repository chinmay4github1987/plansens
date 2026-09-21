package com.prasjaychi.plantsense.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_queue ORDER BY created_at_ms ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingOperationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncOperationEntity): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteOperation(id: Long)

    @Query("DELETE FROM sync_queue WHERE target_type = :targetType AND remote_id = :remoteId")
    suspend fun deleteByRemoteId(targetType: String, remoteId: String)

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}
