package com.bober.autcsv.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты форматтера телефона: нормализация транк-префикса «8» в код страны
 * «7» и поэтапное форматирование во время набора.
 */
class PhoneFormatterTest {

    @Test
    fun `trunk prefix 8 becomes country code 7`() {
        // Раньше «88005553535» превращался в несуществующий «+8 (800)…»
        assertEquals("+7 (800) 555-35-35", PhoneFormatter.formatPhoneNumber("88005553535"))
    }

    @Test
    fun `leading 8 with 7 already present is not double normalized`() {
        assertEquals("+7 (912) 345-67-89", PhoneFormatter.formatPhoneNumber("79123456789"))
        assertEquals("+7 (800) 555-35-35", PhoneFormatter.formatPhoneNumber("+78005553535"))
    }

    @Test
    fun `partial input formats progressively`() {
        assertEquals("+7", PhoneFormatter.formatPhoneNumber("8"))
        assertEquals("+7 (8", PhoneFormatter.formatPhoneNumber("88"))
        assertEquals("+7 (800)", PhoneFormatter.formatPhoneNumber("8800"))
        assertEquals("+7 (800) 555", PhoneFormatter.formatPhoneNumber("8800555"))
        assertEquals("+7 (800) 555-35", PhoneFormatter.formatPhoneNumber("880055535"))
    }

    @Test
    fun `formatted output round-trips through extractDigits`() {
        val original = "88005553535"
        val formatted = PhoneFormatter.formatPhoneNumber(original)
        assertEquals("78005553535", PhoneFormatter.extractDigits(formatted))
    }

    @Test
    fun `empty and garbage input yield empty or digits-only result`() {
        assertEquals("", PhoneFormatter.formatPhoneNumber(""))
        assertEquals("", PhoneFormatter.formatPhoneNumber("-"))
        assertTrue(PhoneFormatter.isValidPhoneNumber("+7 (800) 555-35-35"))
    }

    @Test
    fun `long input is capped at 15 digits`() {
        val formatted = PhoneFormatter.formatPhoneNumber("88005553535123456789")
        assertEquals(15, PhoneFormatter.extractDigits(formatted).length)
    }
}
