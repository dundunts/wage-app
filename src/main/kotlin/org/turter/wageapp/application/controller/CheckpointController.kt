package org.turter.wageapp.application.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.mapper.ShiftResultTransportMapper
import org.turter.wageapp.application.mapper.ShiftSessionCheckpointRequestMapper
import org.turter.wageapp.application.service.shift.CheckpointService
import org.turter.wageapp.transport.api.CheckpointApi
import org.turter.wageapp.transport.model.Checkpoint
import org.turter.wageapp.transport.model.CreateCheckpointRequest
import org.turter.wageapp.transport.model.UpdateCheckpointRequest
import java.util.UUID

@RestController
class CheckpointController(
    private val checkpointService: CheckpointService,
    private val requestMapper: ShiftSessionCheckpointRequestMapper,
    private val responseMapper: ShiftResultTransportMapper,
) : CheckpointApi {

    override suspend fun createCheckpoint(
        createCheckpointRequest: CreateCheckpointRequest,
    ): ResponseEntity<Checkpoint> {
        val checkpoint = checkpointService.createCheckpoint(
            requestMapper.toDomain(createCheckpointRequest),
            currentUserId(),
        )
        return responseMapper.toTransportResponse(HttpStatus.CREATED, checkpoint)
    }

    override suspend fun updateCheckpoint(
        updateCheckpointRequest: UpdateCheckpointRequest,
    ): ResponseEntity<Checkpoint> {
        val checkpoint = checkpointService.updateCheckpoint(
            requestMapper.toDomain(updateCheckpointRequest),
            currentUserId(),
        )
        return responseMapper.toTransportResponse(HttpStatus.OK, checkpoint)
    }

    override suspend fun deleteCheckpoint(
        checkpointId: UUID,
    ): ResponseEntity<Unit> {
        checkpointService.deleteById(checkpointId, currentUserId())
        return ResponseEntity.noContent().build()
    }
}
