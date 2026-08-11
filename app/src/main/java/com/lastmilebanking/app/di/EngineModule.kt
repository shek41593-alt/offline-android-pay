package com.lastmilebanking.app.di

import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import com.lastmilebanking.app.domain.engines.OfflinePaymentEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import com.lastmilebanking.app.domain.engines.impl.AuthenticationEngineImpl
import com.lastmilebanking.app.domain.engines.impl.OfflinePaymentEngineImpl
import com.lastmilebanking.app.domain.engines.impl.TransactionEngineImpl
import com.lastmilebanking.app.domain.engines.impl.ValidationEngineImpl
import com.lastmilebanking.app.domain.engines.impl.WalletEngineImpl
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.engines.impl.SynchronizationEngineImpl
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserverImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindAuthenticationEngine(
        authenticationEngineImpl: AuthenticationEngineImpl
    ): AuthenticationEngine

    @Binds
    @Singleton
    abstract fun bindWalletEngine(
        walletEngineImpl: WalletEngineImpl
    ): WalletEngine

    @Binds
    @Singleton
    abstract fun bindValidationEngine(
        validationEngineImpl: ValidationEngineImpl
    ): ValidationEngine

    @Binds
    @Singleton
    abstract fun bindTransactionEngine(
        transactionEngineImpl: TransactionEngineImpl
    ): TransactionEngine

    @Binds
    @Singleton
    abstract fun bindOfflinePaymentEngine(
        offlinePaymentEngineImpl: OfflinePaymentEngineImpl
    ): OfflinePaymentEngine

    @Binds
    @Singleton
    abstract fun bindSynchronizationEngine(
        synchronizationEngineImpl: SynchronizationEngineImpl
    ): SynchronizationEngine

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(
        connectivityObserverImpl: ConnectivityObserverImpl
    ): ConnectivityObserver
}
