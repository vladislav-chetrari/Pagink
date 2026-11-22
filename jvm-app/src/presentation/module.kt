package presentation

import domain.module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val module = module {
    includes(module)
    viewModel <LocationsViewModel>{ LocationsViewModel(get()) }
}