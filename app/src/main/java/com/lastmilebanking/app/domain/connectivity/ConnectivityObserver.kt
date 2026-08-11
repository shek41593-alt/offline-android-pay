package com.lastmilebanking.app.domain.connectivity

interface ConnectivityObserver {
    fun isNetworkAvailable(): Boolean
}
