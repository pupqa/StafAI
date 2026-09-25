package android.graphics

import java.awt.image.BufferedImage

/**
 * JVM-заглушка android.graphics.Paint для превью-харнесса PDF-шаблонов.
 * Метрики шрифта берутся из Java2D (SansSerif ≈ Roboto), letterSpacing
 * согласованно применяется и в measureText, и в отрисовке.
 */
class Paint(val flags: Int = 0) {

    enum class Style { FILL, STROKE, FILL_AND_STROKE }

    var color: Int = 0xFF000000.toInt()
    var textSize: Float = 12f
    var letterSpacing: Float = 0f
    var strokeWidth: Float = 1f
    var style: Style = Style.FILL
    var isFakeBoldText: Boolean = false

    private var typefaceRef: Typeface? = null

    /** В Android сеттер цепочный и возвращает Typeface — сигнатура важна для байткода. */
    fun setTypeface(typeface: Typeface?): Typeface {
        typefaceRef = typeface
        return typeface ?: Typeface.SANS_SERIF
    }

    fun getTypeface(): Typeface? = typefaceRef

    companion object {
        const val ANTI_ALIAS_FLAG = 1

        /** Общий графический контекст для замера метрик шрифта. */
        internal val METRICS_G2 = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics()
    }

    private fun awtStyle(): Int {
        var s = (getTypeface()?.style ?: Typeface.NORMAL) and 3
        if (isFakeBoldText) s = s or java.awt.Font.BOLD
        return s
    }

    internal fun awtFont(): java.awt.Font =
        java.awt.Font(getTypeface()?.familyName ?: "SansSerif", awtStyle(), textSize.toInt().coerceAtLeast(1))

    fun measureText(text: String): Float {
        val fm = METRICS_G2.getFontMetrics(awtFont())
        return fm.stringWidth(text) + letterSpacing * textSize * text.length
    }

    // Сигнатура Android; не используется шаблоном, но нужна для совместимости
    fun measureText(text: CharSequence): Float = measureText(text.toString())

    fun getTextWidths(text: String): FloatArray = FloatArray(text.length)

    /** Заглушка Paint.FontMetrics: поля — настоящие public fields, как в Android. */
    class FontMetrics(@JvmField val ascent: Float, @JvmField val descent: Float)

    fun getFontMetrics(): FontMetrics {
        val fm = METRICS_G2.getFontMetrics(awtFont())
        return FontMetrics(ascent = -fm.ascent.toFloat(), descent = fm.descent.toFloat())
    }
}
