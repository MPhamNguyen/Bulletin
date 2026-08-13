package com.jdrms.bulletin

import com.jdrms.bulletin.core.common.Greeting
import kotlin.test.Test
import kotlin.test.assertTrue

class SharedLogicAndroidHostTest {

    @Test
    fun testAndroidHostGreeting() {
        val greeting = Greeting().greet()
        assertTrue(greeting.contains("Android") || greeting.startsWith("Hello,"))
    }
}