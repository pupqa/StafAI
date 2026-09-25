package com.bober.autcsv.core.utils

object PhoneFormatter {

    /**
     * Форматирует номер в российском формате +7 (XXX) XXX-XX-XX.
     * Транк-префикс «8» нормализуется в код страны «7», иначе номер
     * уезжает в документы с несуществующим кодом «+8»:
     * «88005553535» -> «+7 (800) 555-35-35».
     *
     * Промежуточные состояния ввода форматируются по мере набора:
     * «8» -> «+7», «880» -> «+7 (80…» и т.д.
     */
    fun formatPhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() }.normalizeRu().take(15)
        if (digits.isEmpty()) return ""

        val rest = digits.substring(1)
        return buildString {
            append('+').append(digits[0])
            if (rest.isNotEmpty()) append(" (").append(rest.take(3))
            if (rest.length >= 3) append(')')
            if (rest.length > 3) append(' ').append(rest.substring(3, minOf(6, rest.length)))
            if (rest.length > 6) append('-').append(rest.substring(6, minOf(8, rest.length)))
            if (rest.length > 8) append('-').append(rest.substring(8, minOf(10, rest.length)))
            if (rest.length > 10) append('-').append(rest.substring(10))
        }
    }

    /**
     * Приводит цифры к российскому плану нумерации: ведущая «8»
     * (транк-префикс) заменяется на код страны «7».
     */
    private fun String.normalizeRu(): String =
        if (isNotEmpty() && this[0] == '8') "7${substring(1)}" else this

    /**
     * Извлекает только цифры из отформатированного номера
     */
    fun extractDigits(formattedPhone: String): String {
        return formattedPhone.filter { it.isDigit() }
    }

    /**
     * Проверяет, является ли номер телефона валидным
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val digits = extractDigits(phone)
        return digits.length >= 10 && digits.length <= 15
    }
}
