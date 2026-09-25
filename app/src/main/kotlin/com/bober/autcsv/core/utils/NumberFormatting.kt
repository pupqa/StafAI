package com.bober.autcsv.core.utils

/**
 * Форматирование числовых значений для полей ввода и отображения.
 */
object NumberFormatting {

    /** Разделитель разрядов: узкий неразрывный пробел. */
    private const val GROUP_SEPARATOR = '\u202F'

    /**
     * Группирует цифры по три справа: «150000» -> «150 000».
     * Неразрывный пробел не ломает парсинг зарплаты в документах
     * и читается лучше «сырых» 150000.
     */
    fun groupThousands(input: String): String {
        val digits = input.filter { it.isDigit() }.take(12)
        if (digits.length <= 3) return digits
        return digits
            .reversed()
            .chunked(3)
            .joinToString(GROUP_SEPARATOR.toString())
            .reversed()
    }
}
