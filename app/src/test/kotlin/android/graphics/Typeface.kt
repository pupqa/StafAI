package android.graphics

/** JVM-заглушка android.graphics.Typeface. */
class Typeface internal constructor(
    internal val familyName: String,
    internal val style: Int,
) {
    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        const val ITALIC = 2
        const val BOLD_ITALIC = 3

        @JvmField val SANS_SERIF = Typeface("SansSerif", NORMAL)

        @JvmField val SERIF = Typeface("Serif", NORMAL)

        @JvmField val MONOSPACE = Typeface("Monospaced", NORMAL)

        @JvmStatic
        fun create(family: String?, style: Int): Typeface =
            Typeface(family ?: "SansSerif", style)

        @JvmStatic
        fun create(typeface: Typeface?, style: Int): Typeface =
            Typeface(typeface?.familyName ?: "SansSerif", style)
    }
}
