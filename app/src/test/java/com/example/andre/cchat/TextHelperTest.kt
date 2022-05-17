package com.example.andre.cchat

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TextHelperTest {

    private var textHelper: TextHelper? = null

    @Before
    fun setup() {
        textHelper = TextHelper()
    }

    @Test
    fun `Text Helper getText should return low if 5`() {
        val expected = "low"
        val result = textHelper?.getText(5)

        assertEquals(expected, result)
    }

    @Test
    fun `TextHelper getText should return high if 100`() {
        val expected = "high"
        val result = textHelper?.getText(100)

        assertEquals(expected, result)
    }
}
