package com.example.garmento.feature.reports

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object ExcelExporter {

    fun export(
        context: Context,
        summary: TransactionSummary,
        revenuePerPlan: List<Pair<String, Double>>,
        ordersPerCustomer: List<Pair<String, Int>>,
        from: LocalDate,
        to: LocalDate
    ): Uri {
        val fileName = "garmento-report-$from-to-$to.xls"
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(directory, fileName)

        HSSFWorkbook().use { workbook ->
            writeSummarySheet(workbook, summary, from, to)
            writePairsSheet(
                workbook = workbook,
                sheetName = "Revenue by Plan",
                headers = listOf("Plan", "Revenue"),
                rows = revenuePerPlan.map { (k, v) -> listOf(k, formatCurrency(v)) }
            )
            writePairsSheet(
                workbook = workbook,
                sheetName = "Orders by Customer",
                headers = listOf("Customer", "Clothes"),
                rows = ordersPerCustomer.map { (k, v) -> listOf(k, v.toString()) }
            )
            FileOutputStream(file).use { workbook.write(it) }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun writeSummarySheet(
        workbook: HSSFWorkbook,
        summary: TransactionSummary,
        from: LocalDate,
        to: LocalDate
    ) {
        val sheet = workbook.createSheet("Summary")
        sheet.setColumnWidth(0, 24 * 256)
        sheet.setColumnWidth(1, 20 * 256)

        val headerCellStyle = headerStyle(workbook)
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("GARMENTO Report")
        dataRow(sheet, 2, listOf("Period From", from.toString()), headerCellStyle)
        dataRow(sheet, 3, listOf("Period To", to.toString()), headerCellStyle)
        dataRow(sheet, 5, listOf("Total Revenue", formatCurrency(summary.totalRevenue)), headerCellStyle)
        dataRow(sheet, 6, listOf("Transactions", summary.transactionCount.toString()), headerCellStyle)
        dataRow(sheet, 7, listOf("Orders", summary.orderCount.toString()), headerCellStyle)
        dataRow(sheet, 8, listOf("Avg Clothes per Order", String.format("%.2f", summary.averageClothesPerOrder)), headerCellStyle)
    }

    private fun writePairsSheet(
        workbook: HSSFWorkbook,
        sheetName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        val sheet = workbook.createSheet(sheetName)
        setColumnWidths(sheet, headers.size)
        val headerCellStyle = headerStyle(workbook)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                setCellStyle(headerCellStyle)
            }
        }
        rows.forEachIndexed { index, rowValues ->
            dataRow(sheet, index + 1, rowValues, headerCellStyle)
        }
    }

    private fun dataRow(sheet: Sheet, rowIndex: Int, values: List<String>, style: CellStyle): Row {
        val row = sheet.createRow(rowIndex)
        values.forEachIndexed { index, value ->
            val cell = row.createCell(index)
            cell.setCellValue(value)
            if (index == 0) cell.setCellStyle(style)
        }
        return row
    }

    private fun headerStyle(workbook: HSSFWorkbook): CellStyle {
        val font: Font = workbook.createFont().apply {
            setBold(true)
            color = IndexedColors.WHITE.index
        }
        return workbook.createCellStyle().apply {
            setFont(font)
            setFillForegroundColor(IndexedColors.DARK_BLUE.index)
            setFillPattern(FillPatternType.SOLID_FOREGROUND)
            setAlignment(HorizontalAlignment.LEFT)
        }
    }

    private fun setColumnWidths(sheet: Sheet, count: Int) {
        repeat(count) { index ->
            sheet.setColumnWidth(index, 30 * 256)
        }
    }

    private fun formatCurrency(value: Double): String = String.format("%.2f", value)
}