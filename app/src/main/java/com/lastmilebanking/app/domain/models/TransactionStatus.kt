package com.lastmilebanking.app.domain.models

enum class TransactionStatus {
    CREATED,
    VALIDATED,
    PENDING_SYNC,
    SYNCING,
    SYNCED,
    SETTLED,
    COMPLETED,
    FAILED,
    CANCELLED,
    CONFLICT,
    ACTION_REQUIRED
}
