package sa.factory.maqaraccesscoworki.data.repository

import sa.factory.maqaraccesscoworki.domain.repository.MaqarAccessRepository
import sa.factory.maqaraccesscoworki.hardware.HardwareSensorDriver
import sa.factory.maqaraccesscoworki.model.MaqarAccessItem
import sa.factory.maqaraccesscoworki.model.MaqarAccessOrder
import sa.factory.maqaraccesscoworki.model.MaqarAccessTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MaqarAccessRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : MaqarAccessRepository {

    private val _catalog = MutableStateFlow<List<MaqarAccessItem>>(listOf(
        MaqarAccessItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع 13.56 MHz RFID Turnstile Access + TOTP Pass", 180.0, "معتمد", "الخدمات الرئيسية"),
        MaqarAccessItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        MaqarAccessItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<MaqarAccessItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<MaqarAccessItem>>(emptyList())
    override val activeCart: StateFlow<List<MaqarAccessItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<MaqarAccessTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            MaqarAccessTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: MaqarAccessItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: MaqarAccessItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): MaqarAccessOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = MaqarAccessOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
