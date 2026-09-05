package com.lastmilebanking.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.dao.UserDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.UserEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity
import com.lastmilebanking.app.data.local.dao.TrustedMerchantKeyDao
import com.lastmilebanking.app.data.local.entity.TrustedMerchantKeyEntity

@Database(
    entities = [
        UserEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        TrustedMerchantKeyEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class LastMileDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun trustedMerchantKeyDao(): TrustedMerchantKeyDao

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

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `transactionId` TEXT NOT NULL, 
                        `walletId` TEXT NOT NULL, 
                        `senderId` TEXT NOT NULL, 
                        `receiverId` TEXT NOT NULL, 
                        `receiverName` TEXT NOT NULL, 
                        `currency` TEXT NOT NULL, 
                        `paymentMode` TEXT NOT NULL, 
                        `encryptedPayload` TEXT NOT NULL, 
                        `note` TEXT NOT NULL, 
                        `isSynced` INTEGER NOT NULL, 
                        `clientOperationId` TEXT NOT NULL, 
                        `senderWalletId` TEXT NOT NULL, 
                        `receiverWalletId` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `transactionType` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `syncedAt` INTEGER, 
                        `retryCount` INTEGER NOT NULL, 
                        `transactionHash` TEXT NOT NULL, 
                        `failureReason` TEXT, 
                        PRIMARY KEY(`transactionId`)
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO transactions_new (
                        transactionId, walletId, senderId, receiverId, receiverName, currency, 
                        paymentMode, encryptedPayload, note, isSynced, amount, transactionType, 
                        status, createdAt, syncedAt, transactionHash, 
                        clientOperationId, senderWalletId, receiverWalletId, retryCount, failureReason
                    )
                    SELECT 
                        transactionId, walletId, senderId, receiverId, receiverName, currency, 
                        paymentMode, encryptedPayload, note, isSynced, amount, transactionType, 
                        status, createdAt, 
                        CASE WHEN syncedAt = 0 THEN NULL ELSE syncedAt END, 
                        transactionHash, 
                        transactionId, senderId, receiverId, 0, NULL
                    FROM transactions
                """.trimIndent())
                
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                
                // Recreate indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_walletId_createdAt` ON `transactions` (`walletId`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isSynced_createdAt` ON `transactions` (`isSynced`, `createdAt`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_clientOperationId` ON `transactions` (`clientOperationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_status` ON `transactions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
            }
        }
        
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `trusted_merchant_keys` (
                        `merchantId` TEXT NOT NULL,
                        `merchantWalletId` TEXT NOT NULL,
                        `publicKeyBase64` TEXT NOT NULL,
                        `keyAlgorithm` TEXT NOT NULL,
                        `signatureAlgorithm` TEXT NOT NULL,
                        `fingerprint` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`merchantId`)
                    )
                """.trimIndent())
            }
        }
    }
}
