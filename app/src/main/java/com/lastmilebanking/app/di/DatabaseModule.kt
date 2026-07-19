package com.lastmilebanking.app.di

import android.content.Context
import androidx.room.Room
import com.lastmilebanking.app.data.local.LastMileDatabase
import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.dao.UserDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LastMileDatabase {
        return Room.databaseBuilder(
            context,
            LastMileDatabase::class.java,
            LastMileDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: LastMileDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideWalletDao(db: LastMileDatabase): WalletDao = db.walletDao()

    @Provides
    @Singleton
    fun provideTransactionDao(db: LastMileDatabase): TransactionDao = db.transactionDao()
}
