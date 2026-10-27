package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage

internal class FakeContactSellerPort : ContactSellerPort {
    data class Call(val userId: String, val listingId: HomeItemId, val message: SellerMessage)

    val calls = mutableListOf<Call>()
    var respond: suspend () -> Result<Unit> = { Result.Success(Unit) }

    override suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        calls += Call(userId, listingId, message)
        return respond()
    }
}
