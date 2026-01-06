package org.turter.wageapp.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.controller.validation.PeriodRequestParamsValidator
import org.turter.wageapp.domain.salary.PeriodType
import org.turter.wageapp.domain.shift.ShiftResultExtendedResponse
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.SaveShiftResultResponse
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import org.turter.wageapp.service.shift.SessionService
import org.turter.wageapp.service.shift.ShiftResultService
import java.security.Principal
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/shift-result")
class ShiftResultController(
    private val shiftResultService: ShiftResultService,
    private val sessionService: SessionService,
    private val periodValidator: PeriodRequestParamsValidator
) {

    @GetMapping("/{resultId}/get/detailed")
    suspend fun getResult(
        @PathVariable resultId: UUID,
        principal: Principal
    ): ResponseEntity<ShiftResultExtendedResponse> {
        val shiftResult = shiftResultService.getDetailed(resultId, principal.name)
        val session = shiftResult.sessionId?.let { sessionId -> sessionService.getById(sessionId) }

        return ResponseEntity.ok(ShiftResultExtendedResponse(shiftResult, session))
    }

    @GetMapping("/get/detailed/by-period/page")
    suspend fun getResultsByPeriodPage(
        @RequestParam companyId: UUID,
        @RequestParam periodType: PeriodType,
        @RequestParam(required = false) start: LocalDate,
        @RequestParam(required = false) end: LocalDate,
        @RequestParam(required = false) now: LocalDate,
        pageable: Pageable,
        principal: Principal
    ): ResponseEntity<Page<ShiftResultDetailed>> {
        return ResponseEntity.ok(
            shiftResultService.getDetailedPage(
                companyId,
                pageable,
                periodValidator.validateAndBuildPeriod(periodType, start, end, now),
                principal.name
            )
        )
    }

    @PostMapping("/save")
    suspend fun saveResult(
        @RequestBody payload: SaveShiftResultPayload,
        principal: Principal
    ): ResponseEntity<SaveShiftResultResponse> {
        return ResponseEntity.status(201).body(shiftResultService.save(payload, principal.name))
    }

    @DeleteMapping("/{resultId}/delete")
    suspend fun deleteResult(
        @PathVariable resultId: UUID,
        principal: Principal
    ): ResponseEntity<Unit> {
        shiftResultService.delete(resultId, principal.name)
        return ResponseEntity.noContent().build()
    }

}