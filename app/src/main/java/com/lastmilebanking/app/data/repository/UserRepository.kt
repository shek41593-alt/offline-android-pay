package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.local.dao.UserDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import com.lastmilebanking.app.data.local.entity.UserEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val walletDao: WalletDao
) {
    fun getActiveUser(): Flow<UserEntity?> = userDao.getActiveUser()

    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)

    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun createUser(
        userId: String,
        name: String,
        phoneNumber: String,
        accountNumber: String,
        ifscCode: String,
        bankName: String,
        publicPaymentId: String? = null
    ): UserEntity {
        val user = UserEntity(
            userId = userId,
            name = name,
            phoneNumber = phoneNumber,
            accountNumber = accountNumber,
            ifscCode = ifscCode,
            bankName = bankName,
            kycStatus = "VERIFIED",
            isActive = true,
            publicPaymentId = publicPaymentId
        )
        userDao.insertUser(user)

        // Create associated wallet
        val wallet = WalletEntity(
            walletId = UUID.randomUUID().toString(),
            userId = userId,
            availableBalance = 0.0,
            offlineBalance = 0.0,
            pendingBalance = 0.0
        )
        walletDao.insertWallet(wallet)

        return user
    }

    suspend fun setActiveUser(userId: String) {
        userDao.clearActiveUsers()
        userDao.setActiveUser(userId)
    }

    suspend fun clearActiveUser() {
        userDao.clearActiveUsers()
    }
}
