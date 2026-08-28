package com.jdrms.bulletin.domain.profile.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Email(val value: String) {
    init {
        require(value.contains("@") && value.contains(".")) {
            "Invalid email address: $value"
        }
    }

    val isUniversityEmail: Boolean
        get() = value.endsWith(".edu", ignoreCase = true) || value.contains(".edu.")
}
