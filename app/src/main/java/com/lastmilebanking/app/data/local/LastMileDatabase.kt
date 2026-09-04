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
    version = 3,
    exportSchema = false
)
abstract class LastMileDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "last_mile_banking.db"

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Delete duplicate wallets, keeping the most recently updated one
                db.execSQL("""
                    DELETE FROM wallet WHERE rowid NOT IN (
                        SELECT MAX(rowid) FROM wallet GROUP BY userId
                    )
                """.trimIndent())
                // Ensure unique constraint index exists
                db.execSQL("DROP INDEX IF EXISTS index_wallet_userId")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_wallet_userId ON wallet(userId)")
            }
        }
    }
}
