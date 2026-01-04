package org.turter.wageapp.domain.salary

import java.time.LocalDate

class PayrollAggregator(
    private val strategy: (elements: List<Payroll.Element>) -> Payroll.Type = DEFAULT_STRATEGY
) {
    companion object {
        private val DEFAULT_STRATEGY: (elements: List<Payroll.Element>) -> Payroll.Type = { elements ->
            when {
                elements.size > 365 * 3 -> Payroll.Type.BY_YEAR
                elements.size > 31 -> Payroll.Type.BY_MONTH
                else -> Payroll.Type.BY_DAY
            }
        }
    }

    fun aggregate(
        elements: List<Payroll.Element>
    ): Payroll {

        val type = strategy(elements)

        val aggregatedElements = aggregateElements(elements, type)
        val summaries = calculateSummaries(aggregatedElements)

        return Payroll(
            type = type,
            elements = aggregatedElements,
            summaries = summaries
        )
    }

    private fun aggregateElements(
        elements: List<Payroll.Element>,
        type: Payroll.Type
    ): List<Payroll.Element> {

        if (type == Payroll.Type.BY_DAY) return elements

        val grouped = when (type) {
            Payroll.Type.BY_MONTH ->
                elements.groupBy { it.date.monthKey() }

            Payroll.Type.BY_YEAR ->
                elements.groupBy { it.date.yearKey() }

            else -> error("Unsupported type")
        }

        return grouped
            .map { (periodDate, elementsInPeriod) ->
                Payroll.Element(
                    date = periodDate,
                    payments = aggregatePayments(elementsInPeriod)
                )
            }
            .sortedBy { it.date }
    }

    private fun aggregatePayments(
        elements: List<Payroll.Element>
    ): List<Payroll.Payment> {

        return elements
            .flatMap { it.payments }
            .groupBy { it.employee.id }
            .map { (_, payments) ->
                Payroll.Payment(
                    employee = payments.first().employee,
                    percentFromRevenue = payments.sumOf { it.percentFromRevenue },
                    tips = payments.sumOf { it.tips }
                )
            }
    }

    private fun calculateSummaries(
        elements: List<Payroll.Element>
    ): List<Payroll.EmployeeSummary> {

        return elements
            .flatMap { it.payments }
            .groupBy { it.employee.id }
            .map { (_, payments) ->
                Payroll.EmployeeSummary(
                    employee = payments.first().employee,
                    totalPercentFromRevenue = payments.sumOf { it.percentFromRevenue },
                    totalTips = payments.sumOf { it.tips }
                )
            }
    }

    private fun LocalDate.monthKey(): LocalDate =
        withDayOfMonth(1)

    private fun LocalDate.yearKey(): LocalDate =
        withDayOfYear(1)
}