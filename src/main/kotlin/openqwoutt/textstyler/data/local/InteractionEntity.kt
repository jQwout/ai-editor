package openqwoutt.miniapp.textstyler.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing interaction history.
 */
@Entity(tableName = "interaction_history")
data class InteractionEntity(
    @PrimaryKey
    val id: Long,
    val timestamp: Long,
    val inputText: String,
    val outputText: String?,
    val mode: String,
    val status: String,  // "SUCCESS" or "ERROR"
    val errorMessage: String?
)