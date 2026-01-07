package org.turter.wageapp.application.controller.validation

import org.springframework.stereotype.Component
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.domain.salary.PeriodType
import java.time.LocalDate

@Component
class PeriodRequestParamsValidator {

    fun validateAndBuildPeriod(
        periodType: PeriodType,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?
    ): Period {
        return when(periodType) {
            PeriodType.CUSTOM -> {
                if (start == null || end == null) throw IllegalArgumentException("Start and end are required")
                Period.of(start, end)
            }
            PeriodType.CURRENT -> {
                if (now == null) throw IllegalArgumentException("Now is required")
                Period.current(now)
            }
            PeriodType.PREVIOUS -> {
                if (now == null) throw IllegalArgumentException("Now is required")
                Period.previous(now)
            }
        }
    }

}