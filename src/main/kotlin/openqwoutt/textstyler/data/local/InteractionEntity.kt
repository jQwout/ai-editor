package openqwoutt.miniapp.textstyler.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing interaction history.
 */
@Entity(
    tableName = "interaction_history",
    indices = [Index(value = ["timestamp"])]
)
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val inputText: String,
    val outputText: String?,
    val mode: String,
    val status: String,  // "SUCCESS" or "ERROR"
    val errorMessage: String?
)