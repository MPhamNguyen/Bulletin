package com.jdrms.bulletin.domain.home.infrastructure.mapper

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.HomePrice
import com.jdrms.bulletin.domain.home.infrastructure.dto.HomeFeedItemDto

object HomeMapper {
    fun toDomain(dto: HomeFeedItemDto): HomeFeedItem {
        val category = runCatching { HomeFeedCategory.valueOf(dto.category.uppercase()) }
            .getOrDefault(HomeFeedCategory.OTHER)
        return HomeFeedItem(
            id = HomeItemId(dto.id),
            title = dto.title,
            description = dto.description,
            price = HomePrice(dto.price.coerceAtLeast(0.0)),
            category = category,
            score = dto.score.coerceAtLeast(0.0),
            reason = dto.reason,
            imageUrl = dto.imageUrl
        )
    }

    fun toDto(domain: HomeFeedItem): HomeFeedItemDto {
        return HomeFeedItemDto(
            id = domain.id.value,
            title = domain.title,
            description = domain.description,
            price = domain.price.amount,
            category = domain.category.name,
            score = domain.score,
            reason = domain.reason,
            imageUrl = domain.imageUrl
        )
    }
}
