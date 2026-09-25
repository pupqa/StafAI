package android.graphics.pdf

import android.graphics.Canvas

/**
 * JVM-заглушка android.graphics.pdf.PdfDocument: вместо PDF каждая страница
 * становится BufferedImage. Харнесс забирает страницы из [lastPages] после
 * generate() и сохраняет их как PNG для визуальной проверки шаблона.
 */
class PdfDocument {

    class PageInfo private constructor(val width: Int, val height: Int, val number: Int) {
        class Builder(val width: Int, val height: Int, val pageNumber: Int) {
            fun create(): PageInfo = PageInfo(width, height, pageNumber)
        }
    }

    class Page(val canvas: Canvas, val number: Int)

    private var pending: java.awt.image.BufferedImage? = null

    init {
        lastPages.clear()
    }

    fun startPage(pageInfo: PageInfo): Page {
        val img = java.awt.image.BufferedImage(
            pageInfo.width,
            pageInfo.height,
            java.awt.image.BufferedImage.TYPE_INT_RGB
        )
        val g = img.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, pageInfo.width, pageInfo.height)
        g.dispose()
        pending = img
        return Page(Canvas(img), pageInfo.number)
    }

    fun finishPage(page: Page) {
        pending?.let { lastPages.add(it) }
        pending = null
    }

    fun writeTo(destination: java.io.OutputStream) {
        // В превью-режиме PDF не пишется — страницы забирает харнесс
    }

    fun close() = Unit

    companion object {
        /** Страницы последнего созданного документа, в порядке номеров. */
        val lastPages = mutableListOf<java.awt.image.BufferedImage>()
    }
}
