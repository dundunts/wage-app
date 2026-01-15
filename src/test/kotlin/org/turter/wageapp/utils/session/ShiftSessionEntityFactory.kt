package org.turter.wageapp.utils.session

import org.turter.wageapp.application.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.domain.shift.ShiftSession
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

object ShiftSessionEntityFactory {

    fun create(
        id: UUID? = null,
        companyId: UUID,
        status: ShiftSession.Status,
        startWorkTime: LocalTime = LocalTime.of(9, 0),
        date: LocalDate = LocalDate.now()
    ): ShiftSessionDbEntity =
        ShiftSessionDbEntity().apply {
            this.id = id
            this.companyId = companyId
            this.status = status
            this.startWorkTime = startWorkTime
            this.date = date
        }

    fun opened(
        companyId: UUID,
        startWorkTime: LocalTime = LocalTime.of(9, 0),
        date: LocalDate = LocalDate.now(),
        status: ShiftSession.Status = ShiftSession.Status.OPENED
    ): ShiftSessionDbEntity =
        ShiftSessionDbEntity().apply {
            this.companyId = companyId
            this.status = status
            this.startWorkTime = startWorkTime
            this.date = date
        }

    fun available(
        companyId: UUID,
        status: ShiftSession.Status,
        startWorkTime: LocalTime = LocalTime.of(9, 0),
        date: LocalDate = LocalDate.now()
    ): ShiftSessionDbEntity =
        ShiftSessionDbEntity().apply {
            this.companyId = companyId
            this.status = status
            this.startWorkTime = startWorkTime
            this.date = date
        }

    fun closed(
        companyId: UUID,
        startWorkTime: LocalTime = LocalTime.of(9, 0),
        date: LocalDate = LocalDate.now()
    ): ShiftSessionDbEntity =
        ShiftSessionDbEntity().apply {
            this.companyId = companyId
            this.status = ShiftSession.Status.CLOSED
            this.startWorkTime = startWorkTime
            this.date = date
        }

}
