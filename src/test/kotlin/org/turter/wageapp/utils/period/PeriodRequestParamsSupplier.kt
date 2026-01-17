package org.turter.wageapp.utils.period

import org.turter.wageapp.domain.salary.PeriodType
import java.time.LocalDate
import java.util.UUID

object PeriodRequestParamsSupplier {

    fun current(
        companyId: UUID,
        date: LocalDate
    ): Map<String, String> =
        mapOf(
            "companyId" to companyId.toString(),
            "periodType" to PeriodType.CURRENT.name,
            "now" to date.toString()
        )

    fun previous(
        companyId: UUID,
        date: LocalDate
    ): Map<String, String> =
        mapOf(
            "companyId" to companyId.toString(),
            "periodType" to PeriodType.PREVIOUS.name,
            "now" to date.toString()
        )

    fun custom(
        companyId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Map<String, String> =
        mapOf(
            "companyId" to companyId.toString(),
            "periodType" to PeriodType.CUSTOM.name,
            "start" to start.toString(),
            "end" to end.toString()
        )
}
