package android.graphics

import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.RoundRectangle2D

/**
 * JVM-заглушка android.graphics.Canvas: рисует примитивы в Java2D.
 * Используется превью-харнессом PDF-шаблонов (см. PdfPreviewHarness).
 */
class Canvas(val image: java.awt.image.BufferedImage) {

    internal val g2: Graphics2D = image.createGraphics()

    init {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    private fun applyColor(paint: Paint) {
        g2.color = java.awt.Color(paint.color)
    }

    fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        applyColor(paint)
        val font = paint.awtFont()
        g2.font = font
        val fm = g2.getFontMetrics(font)
        val ls = paint.letterSpacing * paint.textSize
        var cx = x
        text.forEach { ch ->
            val s = ch.toString()
            g2.drawString(s, cx, y)
            cx += fm.stringWidth(s) + ls
        }
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        applyColor(paint)
        g2.stroke = BasicStroke(paint.strokeWidth.coerceAtLeast(0.4f))
        g2.draw(Line2D.Float(x1, y1, x2, y2))
    }

    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        applyColor(paint)
        val w = right - left
        val h = bottom - top
        when (paint.style) {
            Paint.Style.FILL -> g2.fillRect(left.toInt(), top.toInt(), w.toInt() + 1, h.toInt() + 1)
            else -> {
                g2.stroke = BasicStroke(paint.strokeWidth.coerceAtLeast(0.4f))
                g2.drawRect(left.toInt(), top.toInt(), w.toInt(), h.toInt())
            }
        }
    }

    fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        applyColor(paint)
        val shape = RoundRectangle2D.Float(
            rect.left, rect.top, rect.width(), rect.height(), rx * 2f, ry * 2f
        )
        when (paint.style) {
            Paint.Style.FILL -> g2.fill(shape)
            else -> {
                g2.stroke = BasicStroke(paint.strokeWidth.coerceAtLeast(0.4f))
                g2.draw(shape)
            }
        }
    }
}
