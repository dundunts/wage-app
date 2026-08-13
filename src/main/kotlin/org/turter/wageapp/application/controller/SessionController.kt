package org.turter.wageapp.application.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.mapper.ShiftResultTransportMapper
import org.turter.wageapp.application.mapper.ShiftSessionCheckpointRequestMapper
import org.turter.wageapp.application.service.shift.SessionService
import org.turter.wageapp.transport.api.ShiftSessionApi
import org.turter.wageapp.transport.model.OpenShiftSessionRecalculationRequest
import org.turter.wageapp.transport.model.OpenShiftSessionRequest
import org.turter.wageapp.transport.model.ShiftSession
import org.turter.wageapp.transport.model.UpdateShiftSessionStartRequest
import java.util.UUID

@RestController
class SessionController(
    private val sessionService: SessionService,
    private val requestMapper: ShiftSessionCheckpointRequestMapper,
    private val responseMapper: ShiftResultTransportMapper,
) : ShiftSessionApi {

    override suspend fun getOpenedShiftSession(
        companyId: UUID,
    ): ResponseEntity<ShiftSession> {
        val session = sessionService.getOpenedSessionForCompany(companyId, currentUserId())
        return ResponseEntity.ok(responseMapper.toTransport(session))
    }

    override suspend fun getAvailableShiftSession(
        sessionId: UUID,
    ): ResponseEntity<ShiftSession> {
        val session = sessionService.getAvailableById(sessionId, currentUserId())
        return ResponseEntity.ok(responseMapper.toTransport(session))
    }

    override suspend fun getAvailableShiftSessions(
        companyId: UUID,
    ): ResponseEntity<List<ShiftSession>> {
        val sessions = sessionService.getAllAvailableSessions(companyId, currentUserId())
        return ResponseEntity.ok(sessions.map { responseMapper.toTransport(it) })
    }

    override suspend fun openShiftSession(
        openShiftSessionRequest: OpenShiftSessionRequest,
    ): ResponseEntity<ShiftSession> {
        val session = sessionService.openNewSession(
            requestMapper.toDomain(openShiftSessionRequest),
            currentUserId(),
        )
        return ResponseEntity.status(201).body(responseMapper.toTransport(session))
    }

    override suspend fun openShiftSessionRecalculation(
        openShiftSessionRecalculationRequest: OpenShiftSessionRecalculationRequest,
    ): ResponseEntity<ShiftSession> {
        val session = sessionService.openRecalculatingSession(
            requestMapper.toDomain(openShiftSessionRecalculationRequest),
            currentUserId(),
        )
        return ResponseEntity.status(201).body(responseMapper.toTransport(session))
    }

    override suspend fun closeShiftSession(
        sessionId: UUID,
    ): ResponseEntity<Unit> {
        sessionService.closeSession(sessionId, currentUserId())
        return ResponseEntity.noContent().build()
    }

    override suspend fun updateShiftSessionStart(
        updateShiftSessionStartRequest: UpdateShiftSessionStartRequest,
    ): ResponseEntity<Unit> {
        sessionService.updateStartWorkTime(
            requestMapper.toDomain(updateShiftSessionStartRequest),
            currentUserId(),
        )
        return ResponseEntity.noContent().build()
    }
}
