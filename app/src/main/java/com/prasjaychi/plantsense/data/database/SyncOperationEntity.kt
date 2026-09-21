package com.prasjaychi.plantsense.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks pending background synchronization operations (e.g. deletions, batch mutations)
 * to guarantee that offline mutations in Room are reliably replayed to Firestore when connectivity returns.
 */
@Entity(tableName = "sync_queue")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "target_type")
    val targetType: String, // "PLANT", "REMINDER"

    @ColumnInfo(name = "operation_type")
    val operationType: String, // "DELETE", "UPSERT"

    @ColumnInfo(name = "local_id")
    val localId: Long = 0L,

    @ColumnInfo(name = "remote_id")
    val remoteId: String = "",

    @ColumnInfo(name = "payload_json")
    val payloadJson: String = "",

    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0
)
