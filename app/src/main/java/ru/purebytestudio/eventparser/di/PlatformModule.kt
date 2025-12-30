package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.platform.AndroidErrorMessageProvider
import ru.purebytestudio.eventparser.data.platform.AndroidNetworkStatusProvider
import ru.purebytestudio.eventparser.data.platform.AndroidResourceProvider
import ru.purebytestudio.eventparser.data.platform.ExponentialBackoffRetryPolicy
import ru.purebytestudio.eventparser.data.platform.SystemTimeProvider
import ru.purebytestudio.eventparser.platform.ErrorMessageProvider
import ru.purebytestudio.eventparser.platform.NetworkStatusProvider
import ru.purebytestudio.eventparser.platform.ResourceProvider
import ru.purebytestudio.eventparser.platform.RetryPolicy
import ru.purebytestudio.eventparser.platform.TimeProvider

val platformModule = module {
    singleOf(::AndroidNetworkStatusProvider) bind NetworkStatusProvider::class
    single<RetryPolicy> { ExponentialBackoffRetryPolicy() }
    singleOf(::AndroidErrorMessageProvider) bind ErrorMessageProvider::class
    singleOf(::AndroidResourceProvider) bind ResourceProvider::class
    singleOf(::SystemTimeProvider) bind TimeProvider::class
}
