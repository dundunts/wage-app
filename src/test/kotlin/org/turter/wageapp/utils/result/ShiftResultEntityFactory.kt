package org.turter.wageapp.utils.result

import org.turter.wageapp.application.data.shift.ShiftResultDbEntity
import org.turter.wageapp.domain.shift.CalculationSource
import java.time.LocalDate
import java.util.UUID

object ShiftResultEntityFactory {

    fun create(
        companyId: UUID,
        date: LocalDate,
        sessionId: UUID? = null,
        calculationSource: CalculationSource = CalculationSource.MANUAL_OVERRIDE
    ): ShiftResultDbEntity {
        return ShiftResultDbEntity().apply {
            id = null
            this.companyId = companyId
            this.date = date
            this.sessionId = sessionId
            this.calculationSource = calculationSource
        }
    }
}
