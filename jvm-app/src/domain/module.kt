package domain

import data.module
import org.koin.dsl.module

val module = module {
    includes(module)
    factory { FlowLocationsPagingUseCase(get()) }
}