package com.lastmilebanking.app.data.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage
) {
    private val _unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvent = _unauthorizedEvent.asSharedFlow()

    fun logout() {
        tokenStorage.clearToken()
        _unauthorizedEvent.tryEmit(Unit)
    }

    fun handleUnauthorized() {
        tokenStorage.clearToken()
        _unauthorizedEvent.tryEmit(Unit)
    }
}
