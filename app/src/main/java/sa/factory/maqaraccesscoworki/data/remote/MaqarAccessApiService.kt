package sa.factory.maqaraccesscoworki.data.remote

import sa.factory.maqaraccesscoworki.model.MaqarAccessItem
import sa.factory.maqaraccesscoworki.model.MaqarAccessOrder

interface MaqarAccessApiService {
    suspend fun fetchCatalog(): List<MaqarAccessItem>
    suspend fun submitOrder(order: MaqarAccessOrder): Boolean
}
