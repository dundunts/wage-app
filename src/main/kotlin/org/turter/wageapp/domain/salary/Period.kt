package org.turter.wageapp.domain.salary

import java.time.LocalDate
import java.time.YearMonth

data class Period(
    val start: LocalDate,
    val end: LocalDate
) {
    init {
        require(!end.isBefore(start)) {
            "Period end must not be before start"
        }
    }

    companion object {

        /** Произвольный период */
        fun of(start: LocalDate, end: LocalDate): Period =
            Period(start, end)

        /** Прошедший зарплатный период */
        fun previous(currentDate: LocalDate): Period {
            return if (currentDate.dayOfMonth <= 15) {
                val previousMonth = YearMonth.from(currentDate).minusMonths(1)
                Period(
                    start = previousMonth.atDay(16),
                    end = previousMonth.atEndOfMonth()
                )
            } else {
                val currentMonth = YearMonth.from(currentDate)
                Period(
                    start = currentMonth.atDay(1),
                    end = currentMonth.atDay(15)
                )
            }
        }

        /** Текущий зарплатный период */
        fun current(currentDate: LocalDate): Period {
            val currentMonth = YearMonth.from(currentDate)

            return if (currentDate.dayOfMonth <= 15) {
                Period(
                    start = currentMonth.atDay(1),
                    end = currentMonth.atDay(15)
                )
            } else {
                Period(
                    start = currentMonth.atDay(16),
                    end = currentMonth.atEndOfMonth()
                )
            }
        }
    }
}

enum class PeriodType {
    CUSTOM, CURRENT, PREVIOUS
}