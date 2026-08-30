package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ProfileRepository {
    suspend fun getProfile(userId: UserId): Result<StudentProfile?>
    suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile>
    suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit>
    suspend fun getReputation(userId: UserId): StudentReputation
}
