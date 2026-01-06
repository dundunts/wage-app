package org.turter.wageapp.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.domain.shift.CreateRecalculatingShiftSessionPayload
import org.turter.wageapp.domain.shift.OpenNewShiftSessionPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.UpdateShiftSessionStartWorkTimePayload
import org.turter.wageapp.service.shift.SessionService
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/session")
//TODO add endpoint for getSessionByIdForModifying (will return only when session is not closed) otherwise 404
class SessionController(
    private val sessionService: SessionService
) {

    @GetMapping("/get/opened")
    suspend fun getOpenedSessionForCompanyId(
        @RequestParam companyId: UUID,
        principal: Principal
    ): ResponseEntity<ShiftSession> {
        return ResponseEntity.ok(sessionService.getOpenedSessionForCompany(companyId, principal.name))
    }

    @GetMapping("/get/available/all")
    suspend fun getAllAvailableSessions(
        @RequestParam companyId: UUID,
        principal: Principal
    ): ResponseEntity<List<ShiftSession>> {
        return ResponseEntity.ok(sessionService.getAllAvailableSessions(companyId, principal.name))
    }

    @PostMapping("/open")
    suspend fun openNewSession(
        @RequestBody payload: OpenNewShiftSessionPayload,
        principal: Principal
    ): ResponseEntity<ShiftSession> {
        return ResponseEntity.status(201).body(sessionService.openNewSession(payload, principal.name))
    }

    @PostMapping("/recalculating")
    suspend fun openRecalculatingSession(
        @RequestBody payload: CreateRecalculatingShiftSessionPayload,
        principal: Principal
    ): ResponseEntity<ShiftSession> {
        return ResponseEntity.status(201).body(sessionService.openRecalculatingSession(payload, principal.name))
    }

    @PutMapping("/{sessionId}/close")
    suspend fun closeSession(
        @PathVariable sessionId: UUID,
        principal: Principal
    ): ResponseEntity<Unit> {
        sessionService.closeSession(sessionId, principal.name)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/update/time")
    suspend fun updateSessionStartWorkTime(
        @RequestBody payload: UpdateShiftSessionStartWorkTimePayload,
        principal: Principal
    ): ResponseEntity<Unit> {
        sessionService.updateStartWorkTime(payload, principal.name)
        return ResponseEntity.noContent().build()
    }

}