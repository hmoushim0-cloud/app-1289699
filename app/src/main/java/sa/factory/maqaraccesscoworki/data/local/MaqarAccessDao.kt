package sa.factory.maqaraccesscoworki.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaqarAccessDao {
    @Query("SELECT * FROM coworking_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<MaqarAccessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MaqarAccessEntity>)

    @Query("DELETE FROM coworking_items_cache")
    suspend fun clearAll()
}
