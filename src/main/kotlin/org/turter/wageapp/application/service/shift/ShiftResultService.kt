package org.turter.wageapp.application.service.shift

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.SaveShiftResultResponse
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import java.util.UUID

interface ShiftResultService {

    suspend fun getDetailed(resultId: UUID, userId: String): ShiftResultDetailed

    suspend fun getDetailedPage(companyId: UUID, pageable: Pageable, period: Period, userId: String): Page<ShiftResultDetailed>

    suspend fun save(payload: SaveShiftResultPayload, userId: String): SaveShiftResultResponse

    suspend fun delete(resultId: UUID, userId: String)

}
