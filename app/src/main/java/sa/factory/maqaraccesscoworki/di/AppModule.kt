package sa.factory.maqaraccesscoworki.di

import sa.factory.maqaraccesscoworki.data.repository.MaqarAccessRepositoryImpl
import sa.factory.maqaraccesscoworki.domain.repository.MaqarAccessRepository
import sa.factory.maqaraccesscoworki.domain.usecase.*
import sa.factory.maqaraccesscoworki.hardware.HardwareSensorDriver
import sa.factory.maqaraccesscoworki.ui.viewmodel.MaqarAccessViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: MaqarAccessRepository = MaqarAccessRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): MaqarAccessViewModel = MaqarAccessViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
