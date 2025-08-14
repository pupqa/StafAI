package com.bober.autcsv.core.utils

object PhoneFormatter {
    
    /**
     * Форматирует номер телефона в российском формате
     * Примеры:
     * "88005553535" -> "+8 (800) 555-35-35"
     * "8800555353" -> "+8 (800) 555-35-3"
     * "880055535" -> "+8 (800) 555-35"
     * "88005553" -> "+8 (800) 555-3"
     * "8800555" -> "+8 (800) 555"
     * "880055" -> "+8 (800) 55"
     * "88005" -> "+8 (800) 5"
     * "8800" -> "+8 (800)"
     * "880" -> "+8 (80"
     * "88" -> "+8 (8"
     * "8" -> "+8"
     */
    fun formatPhoneNumber(input: String): String {
        // Удаляем все нецифровые символы
        val digitsOnly = input.filter { it.isDigit() }
        
        val result = when {
            digitsOnly.isEmpty() -> ""
            digitsOnly.length == 1 -> "+$digitsOnly"
            digitsOnly.length == 2 -> "+$digitsOnly"
            digitsOnly.length == 3 -> "+$digitsOnly"
            digitsOnly.length == 4 -> "+${digitsOnly[0]} (${digitsOnly.substring(1)}"
            digitsOnly.length == 5 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 6 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 7 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 8 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 9 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 10 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6, 8)}-${digitsOnly.substring(8)}"
            digitsOnly.length == 11 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7, 9)}-${digitsOnly.substring(9)}"
            digitsOnly.length >= 12 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7, 9)}-${digitsOnly.substring(9, 11)}-${digitsOnly.substring(11, minOf(15, digitsOnly.length))}"
            else -> digitsOnly
        }
        

        
        return result
    }
    
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
    
    /**
     * Получает маску для ввода номера телефона
     */
    fun getPhoneMask(input: String): String {
        val digitsOnly = input.filter { it.isDigit() }
        
        return when {
            digitsOnly.isEmpty() -> "+"
            digitsOnly.length == 1 -> "+$digitsOnly"
            digitsOnly.length == 2 -> "+$digitsOnly"
            digitsOnly.length == 3 -> "+$digitsOnly"
            digitsOnly.length == 4 -> "+${digitsOnly[0]} (${digitsOnly.substring(1)}"
            digitsOnly.length == 5 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 6 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 7 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4)}"
            digitsOnly.length == 8 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 9 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 10 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6, 8)}-${digitsOnly.substring(8)}"
            digitsOnly.length >= 11 -> "+${digitsOnly[0]} (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7, 9)}-${digitsOnly.substring(9, 11)}-${digitsOnly.substring(11, minOf(15, digitsOnly.length))}"
            else -> digitsOnly
        }
    }
} 