package org.turter.wageapp.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.turter.wageapp.domain.shift.ConfirmDraftResponse
import org.turter.wageapp.domain.shift.ShiftResultDraft
import org.turter.wageapp.service.shift.CalculationService
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api/v1/calculation")
class CalculationController(
    private val calculationService: CalculationService
) {

    @GetMapping("/draft/for-session/{sessionId}")
    suspend fun getDraftForSession(
        @PathVariable sessionId: UUID,
        principal: Principal
    ): ResponseEntity<ShiftResultDraft> {
        return ResponseEntity.ok(calculationService.getOrCalculateDraft(sessionId, principal.name))
    }

    @PostMapping("/draft/{id}/confirm")
    suspend fun confirmDraft(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<ConfirmDraftResponse> {
        return ResponseEntity.ok(calculationService.confirmDraft(id, principal.name))
    }

    @DeleteMapping("/draft/{id}/delete")
    suspend fun deleteDraft(@PathVariable id: UUID, principal: Principal): ResponseEntity<Unit> {
        calculationService.deleteDraft(id, principal.name)
        return ResponseEntity.noContent().build()
    }

}