package org.turter.wageapp.application.service.salary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.turter.wageapp.domain.salary.Payroll
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ExcelExporterService {

    // Переключаемся на IO диспетчер для тяжелой работы с файлами
    suspend fun generatePayrollExcel(payroll: Payroll): ByteArray = withContext(Dispatchers.IO) {
        // Используем use для автоматического закрытия ресурсов
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Reports")

            // --- ВАЖНО: Создаем стили ОДИН раз за пределами циклов ---
            val headerStyle = createHeaderStyle(workbook)
            val dataStyle = createDataCellStyle(workbook)

            val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

            // 1. Подготовка данных
            val sortedElements = payroll.elements.sortedBy { it.date }
            // Собираем уникальных сотрудников
            val employees = payroll.summaries.map { it.employee }
                .distinctBy { it.id }
                .sortedBy { it.lastName }

            val employeeRowMap = mutableMapOf<UUID, EmployeeRowIndices>()

            // 2. Рисуем шапку с датами
            val datesRow = sheet.createRow(0)
            // Объединяем ячейку заголовка имен (A1:B1)
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 1))

            var colIndex = 2
            sortedElements.forEach { element ->
                createCell(datesRow, colIndex, element.date.format(dateFormatter), headerStyle)
                colIndex++
            }

            // Колонка "Итого"
            createCell(datesRow, colIndex, "Итого:", headerStyle)
            val totalColumnIndex = colIndex

            // 3. Создаем строки для сотрудников
            var rowIndex = 1

            employees.forEach { emp ->
                val rowPercent = sheet.createRow(rowIndex)
                val rowTip = sheet.createRow(rowIndex + 1)

                employeeRowMap[emp.id] = EmployeeRowIndices(rowPercent, rowTip)

                // Имя сотрудника
                sheet.addMergedRegion(CellRangeAddress(rowIndex, rowIndex + 1, 0, 0))
                val fullName = "${emp.lastName} ${emp.firstName} ${emp.simpleName ?: ""}".trim()
                createCell(rowPercent, 0, fullName, dataStyle)

                // Метки
                createCell(rowPercent, 1, "%", dataStyle)
                createCell(rowTip, 1, "Чай", dataStyle)

                rowIndex += 2
            }

            // 4. Строка "За день" (Итого внизу)
            val totalPerDayRow = sheet.createRow(rowIndex)
            createCell(totalPerDayRow, 0, "За день", headerStyle)
            sheet.addMergedRegion(CellRangeAddress(rowIndex, rowIndex, 0, 1))

            // 5. Заполнение данными
            var dateCol = 2

            // Проход по дням (колонки)
            sortedElements.forEach { element ->
                var currentDayTotal = 0

                // Заполняем ячейки сотрудников
                employees.forEach { emp ->
                    val rows = employeeRowMap[emp.id]!!
                    // Ищем выплату сотрудника за этот день
                    val payment = element.payments.find { it.employee.id == emp.id }

                    if (payment != null) {
                        createCell(rows.percentRow, dateCol, payment.percentFromRevenue, dataStyle)
                        createCell(rows.tipRow, dateCol, payment.tips, dataStyle)
                        currentDayTotal += (payment.percentFromRevenue + payment.tips)
                    } else {
                        createCell(rows.percentRow, dateCol, 0, dataStyle)
                        createCell(rows.tipRow, dateCol, 0, dataStyle)
                    }
                }

                // Итого за день (нижняя строка)
                createCell(totalPerDayRow, dateCol, currentDayTotal, headerStyle)
                dateCol++
            }

            // 6. Заполнение колонки "Итого" (справа)
            var grandTotal = 0

            employees.forEach { emp ->
                val summary = payroll.summaries.find { it.employee.id == emp.id }
                val rows = employeeRowMap[emp.id]!!
                val empTotal = (summary?.totalPercentFromRevenue ?: 0) + (summary?.totalTips ?: 0)

                // Объединяем ячейки % и Чай в колонке Итого
                sheet.addMergedRegion(CellRangeAddress(rows.percentRow.rowNum, rows.tipRow.rowNum, totalColumnIndex, totalColumnIndex))

                createCell(rows.percentRow, totalColumnIndex, empTotal, dataStyle)
                // Нижнюю ячейку тоже создаем для корректности стиля (хоть она и скрыта объединением)
                createCell(rows.tipRow, totalColumnIndex, empTotal, dataStyle)

                grandTotal += empTotal
            }

            // Финальная сумма всего периода
            createCell(totalPerDayRow, totalColumnIndex, grandTotal, headerStyle)

            // Автосайз колонок (лучше делать в конце)
            // sheet.autoSizeColumn(0) // Можно включить для первой колонки

            // Запись в поток
            ByteArrayOutputStream().use { out ->
                workbook.write(out)
                out.toByteArray()
            }
        }
    }

    // --- Вспомогательные методы ---

    private data class EmployeeRowIndices(
        val percentRow: Row,
        val tipRow: Row
    )

    private fun createCell(row: Row, colIndex: Int, value: Any, style: CellStyle) {
        val cell = row.createCell(colIndex)
        cell.cellStyle = style

        when (value) {
            is Int -> cell.setCellValue(value.toDouble())
            is Long -> cell.setCellValue(value.toDouble())
            is Double -> cell.setCellValue(value)
            is String -> cell.setCellValue(value)
            is LocalDate -> cell.setCellValue(value)
        }
    }

    // ИСПРАВЛЕННЫЙ МЕТОД
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = createGeneralStyle(workbook)
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 14.toShort()
        font.fontName = "Times New Roman" // Явное имя шрифта вместо Family
        style.setFont(font)

        return style
    }

    // ИСПРАВЛЕННЫЙ МЕТОД
    private fun createDataCellStyle(workbook: Workbook): CellStyle {
        val style = createGeneralStyle(workbook)

        val font = workbook.createFont()
        font.fontHeightInPoints = 14.toShort()
        font.fontName = "Times New Roman" // Явное имя шрифта вместо Family
        style.setFont(font)

        return style
    }

    private fun createGeneralStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        return style
    }
}