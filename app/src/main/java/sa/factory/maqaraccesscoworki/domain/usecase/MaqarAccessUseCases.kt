package sa.factory.maqaraccesscoworki.domain.usecase

import sa.factory.maqaraccesscoworki.domain.repository.MaqarAccessRepository
import sa.factory.maqaraccesscoworki.model.MaqarAccessItem
import sa.factory.maqaraccesscoworki.model.MaqarAccessOrder
import sa.factory.maqaraccesscoworki.model.MaqarAccessTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: MaqarAccessRepository) {
    operator fun invoke(): Flow<List<MaqarAccessItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: MaqarAccessRepository) {
    val cart = repository.activeCart
    fun add(item: MaqarAccessItem) = repository.addToCart(item)
    fun remove(item: MaqarAccessItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: MaqarAccessRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): MaqarAccessOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: MaqarAccessRepository) {
    operator fun invoke(): Flow<MaqarAccessTelemetry> = repository.telemetryFlow
}
