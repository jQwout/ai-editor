package openqwoutt.miniapp.textstyler.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for interaction history.
 */
@Dao
interface InteractionDao {

    @Query("SELECT * FROM interaction_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<InteractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InteractionEntity)

    @Query("DELETE FROM interaction_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM interaction_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM interaction_history")
    suspend fun count(): Int

    @Query("DELETE FROM interaction_history WHERE id IN (SELECT id FROM interaction_history ORDER BY timestamp DESC LIMIT -1 OFFSET :keep)")
    suspend fun deleteOldest(keep: Int)
}