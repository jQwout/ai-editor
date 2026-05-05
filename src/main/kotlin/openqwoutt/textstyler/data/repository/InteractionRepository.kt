package openqwoutt.miniapp.textstyler.data.repository

import android.content.Context
import openqwoutt.miniapp.textstyler.data.local.AppDatabase
import openqwoutt.miniapp.textstyler.data.local.InteractionEntity
import openqwoutt.miniapp.textstyler.domain.model.Interaction
import openqwoutt.miniapp.textstyler.domain.model.InteractionStatus
import kotlin.random.Random

/**
 * Repository for interaction history.
 */
class InteractionRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).interactionDao()

    /**
     * Get all interactions ordered by timestamp DESC.
     */
    suspend fun getAll(limit: Int = 100): List<Interaction> {
        return dao.getAll(limit).map { it.toDomain() }
    }

    /**
     * Save a new interaction (transactional).
     */
    suspend fun save(
        inputText: String,
        outputText: String?,
        mode: String,
        status: InteractionStatus,
        errorMessage: String? = null
    ) {
        // Let Room auto-generate ID
        dao.insertWithLimit(
            InteractionEntity(
                timestamp = System.currentTimeMillis(),
                inputText = inputText,
                outputText = outputText,
                mode = mode,
                status = status.name,
                errorMessage = errorMessage
            ),
            MAX_ITEMS
        )
    }

    /**
     * Delete single interaction.
     */
    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    /**
     * Delete all interactions.
     */
    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun InteractionEntity.toDomain(): Interaction {
        return Interaction(
            id = id,
            timestamp = timestamp,
            inputText = inputText,
            outputText = outputText,
            mode = mode,
            status = try {
                InteractionStatus.valueOf(status)
            } catch (e: Exception) {
                InteractionStatus.ERROR
            },
            errorMessage = errorMessage
        )
    }

    companion object {
        private const val MAX_ITEMS = 100
    }
}