package com.example.andre.cchat

import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TextHelperTest {

    private lateinit var textHelper: TextHelper

    @Before
    fun setup() {
        textHelper = TextHelper()
    }

    @Test
    fun `Text Helper getText should return low if 5`() {
        val expected = "low"
        val result = textHelper.getText(5)

        assertEquals(expected, result)
    }

}
