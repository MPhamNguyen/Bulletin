package com.jdrms.bulletin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform