package org.turter.wageapp.application.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.mapper.ShiftResultTransportMapper
import org.turter.wageapp.application.service.shift.CalculationService
import org.turter.wageapp.transport.api.ShiftResultDraftApi
import org.turter.wageapp.transport.model.ConfirmShiftResultDraftResponse
import org.turter.wageapp.transport.model.ShiftResultDraft
import java.util.UUID

@RestController
class CalculationController(
    private val calculationService: CalculationService,
    private val transportMapper: ShiftResultTransportMapper,
) : ShiftResultDraftApi {

    override suspend fun getOrCalculateShiftResultDraft(
        sessionId: UUID,
    ): ResponseEntity<ShiftResultDraft> = ResponseEntity.ok(
        transportMapper.toTransport(
            calculationService.getOrCalculateDraft(sessionId, currentUserId()),
        ),
    )

    override suspend fun confirmShiftResultDraft(
        id: UUID,
    ): ResponseEntity<ConfirmShiftResultDraftResponse> = ResponseEntity.ok(
        transportMapper.toTransport(calculationService.confirmDraft(id, currentUserId())),
    )

    override suspend fun deleteShiftResultDraft(id: UUID): ResponseEntity<Unit> {
        calculationService.deleteDraft(id, currentUserId())
        return ResponseEntity.noContent().build()
    }
}
