package android.graphics

/** JVM-заглушка android.graphics.RectF. */
class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f,
) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
}
