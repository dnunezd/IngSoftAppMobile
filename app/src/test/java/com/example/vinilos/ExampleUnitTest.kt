package com.example.vinilos

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        // Given
        val a = 2
        val b = 2

        // When
        val suma = a + b

        // Then
        assertEquals(4, suma)
    }
}