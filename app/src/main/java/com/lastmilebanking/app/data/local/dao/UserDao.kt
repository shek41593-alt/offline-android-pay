package com.lastmilebanking.app.data.local.dao

import androidx.room.*
import com.lastmilebanking.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    fun getActiveUser(): Flow<UserEntity?>

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)
}
