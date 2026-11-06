package sa.factory.maqaraccesscoworki.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MaqarAccessEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coworkingDao(): MaqarAccessDao
}
