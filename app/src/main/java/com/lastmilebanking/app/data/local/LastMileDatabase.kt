package com.lastmilebanking.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.dao.UserDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.UserEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity

@Database(
    entities = [
        UserEntity::class,
        WalletEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LastMileDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "last_mile_banking.db"
    }
}
