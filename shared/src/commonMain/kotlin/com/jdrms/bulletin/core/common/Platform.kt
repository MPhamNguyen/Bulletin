package com.jdrms.bulletin.core.common

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
