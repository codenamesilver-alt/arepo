package com.example.garmento.feature.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 16f
    private const val MAX_LINES_PER_PAGE = 46

    fun createPdf(context: Context, fileName: String, title: String, lines: List<String>): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)

        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = Color.BLACK }
        val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 20f
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 24f
        var linesOnPage = 0

        for (line in lines) {
            if (linesOnPage >= MAX_LINES_PER_PAGE) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN + 20f
                linesOnPage = 0
            }
            canvas.drawText(line, MARGIN, y, bodyPaint)
            y += LINE_HEIGHT
            linesOnPage++
        }
        document.finishPage(page)
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
    }
}