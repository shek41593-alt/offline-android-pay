package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.local.dao.TrustedMerchantKeyDao
import com.lastmilebanking.app.data.local.entity.TrustedMerchantKeyEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedMerchantKeyRepository @Inject constructor(
    private val dao: TrustedMerchantKeyDao
) {
    suspend fun getTrustedKey(merchantId: String): TrustedMerchantKeyEntity? {
        return dao.getTrustedKey(merchantId)
    }
    
    suspend fun insertTrustedKey(key: TrustedMerchantKeyEntity) {
        dao.insertTrustedKey(key)
    }
}
