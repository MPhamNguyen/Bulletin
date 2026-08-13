package com.jdrms.bulletin

import com.jdrms.bulletin.core.common.Greeting
import com.jdrms.bulletin.core.common.sayHello
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreetingTest {

    @Test
    fun testSayHello() {
        val result = sayHello("Android")
        assertEquals("Hello, Android!", result)
    }

    @Test
    fun testGreetingOutput() {
        val greeting = Greeting().greet()
        assertTrue(greeting.startsWith("Hello, "))
    }
}
