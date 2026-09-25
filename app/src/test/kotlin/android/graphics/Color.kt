package android.graphics

/**
 * JVM-заглушка android.graphics.Color. Члены — настоящие static-члены
 * (@JvmStatic/@JvmField): скомпилированный шаблон вызывает их как статика
 * настоящего android.graphics.Color.
 */
class Color private constructor() {

    companion object {
        @JvmField val WHITE = -1

        @JvmField val BLACK = -16777216

        @JvmField val DKGRAY = -12303292

        @JvmField val GRAY = -7829368

        @JvmField val LTGRAY = -3355444

        @JvmStatic
        fun rgb(red: Int, green: Int, blue: Int): Int =
            (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
