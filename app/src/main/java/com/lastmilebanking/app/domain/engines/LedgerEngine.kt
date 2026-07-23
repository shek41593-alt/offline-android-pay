package com.lastmilebanking.app.domain.engines

interface LedgerEngine {
    suspend fun recordDebit(transactionId: String, accountId: String, amount: Double): Result<Unit>
    suspend fun recordCredit(transactionId: String, accountId: String, amount: Double): Result<Unit>
    suspend fun verifyLedgerIntegrity(): Boolean
}
