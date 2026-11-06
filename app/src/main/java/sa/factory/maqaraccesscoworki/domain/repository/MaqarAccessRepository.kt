package sa.factory.maqaraccesscoworki.domain.repository

import sa.factory.maqaraccesscoworki.model.MaqarAccessItem
import sa.factory.maqaraccesscoworki.model.MaqarAccessOrder
import sa.factory.maqaraccesscoworki.model.MaqarAccessTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MaqarAccessRepository {
    val catalogFlow: Flow<List<MaqarAccessItem>>
    val activeCart: StateFlow<List<MaqarAccessItem>>
    val telemetryFlow: Flow<MaqarAccessTelemetry>
    
    fun addToCart(item: MaqarAccessItem)
    fun removeFromCart(item: MaqarAccessItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): MaqarAccessOrder
    suspend fun refreshCatalog()
}
