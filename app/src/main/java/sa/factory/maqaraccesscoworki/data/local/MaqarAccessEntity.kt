package sa.factory.maqaraccesscoworki.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.maqaraccesscoworki.model.MaqarAccessItem

@Entity(tableName = "coworking_items_cache")
data class MaqarAccessEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): MaqarAccessItem = MaqarAccessItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
