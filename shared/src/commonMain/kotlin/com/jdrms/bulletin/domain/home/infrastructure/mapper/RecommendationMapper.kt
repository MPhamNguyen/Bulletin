package com.jdrms.bulletin.domain.home.infrastructure.mapper

import com.jdrms.bulletin.domain.home.domain.model.*
import com.jdrms.bulletin.domain.home.infrastructure.dto.PreferencesDto
import com.jdrms.bulletin.domain.home.infrastructure.dto.RecommendationDto
import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.ListingMapper

object RecommendationMapper {
    fun toDomain(dto: RecommendationDto): Recommendation {
        return Recommendation(
            id = RecommendationId(dto.id),
            listing = ListingMapper.toDomain(dto.listing),
            score = dto.score,
            reason = dto.reason
        )
    }

    fun toDto(domain: Recommendation): RecommendationDto {
        return RecommendationDto(
            id = domain.id.value,
            listing = ListingMapper.toDto(domain.listing),
            score = domain.score,
            reason = domain.reason
        )
    }

    fun preferencesToDomain(dto: PreferencesDto): UserPreferences {
        val cats = dto.preferredCategories.mapNotNull { catStr ->
            try { Category.valueOf(catStr.uppercase()) } catch (ignored: IllegalArgumentException) { null }
        }
        return UserPreferences(
            preferredCategories = cats,
            maxPrice = dto.maxPrice,
            campusLocation = dto.campusLocation
        )
    }

    fun preferencesToDto(domain: UserPreferences): PreferencesDto {
        return PreferencesDto(
            preferredCategories = domain.preferredCategories.map { it.name },
            maxPrice = domain.maxPrice,
            campusLocation = domain.campusLocation
        )
    }
}
