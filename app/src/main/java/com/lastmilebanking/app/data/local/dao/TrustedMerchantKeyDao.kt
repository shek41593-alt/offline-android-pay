package com.lastmilebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lastmilebanking.app.data.local.entity.TrustedMerchantKeyEntity

@Dao
interface TrustedMerchantKeyDao {
    @Query("SELECT * FROM trusted_merchant_keys WHERE merchantId = :merchantId LIMIT 1")
    suspend fun getTrustedKey(merchantId: String): TrustedMerchantKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedKey(key: TrustedMerchantKeyEntity)
}
