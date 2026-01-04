package org.turter.wageapp.domain.salary

import java.time.LocalDate
import java.util.UUID

data class Payroll(
    val type: Type,
    val elements: List<Element>,
    val summaries: List<EmployeeSummary>
) {
    enum class Type {
        BY_DAY, BY_MONTH, BY_YEAR
    }

    data class Element(
        val date: LocalDate,
        val payments: List<Payment>
    )

    data class Payment(
        val employee: EmployeeInfo,
        val percentFromRevenue: Int,
        val tips: Int
    )

    data class EmployeeSummary(
        val employee: EmployeeInfo,
        val totalPercentFromRevenue: Int,
        val totalTips: Int
    )

    data class EmployeeInfo(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val patronymic: String,
        val simpleName: String?
    )
}

