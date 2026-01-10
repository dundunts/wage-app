package org.turter.wageapp.application.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import org.turter.wageapp.application.service.shift.CheckpointService
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/checkpoint")
class CheckpointController(
    private val checkpointService: CheckpointService
) {

    @PostMapping("/create")
    suspend fun createCheckpointForSession(
        @RequestBody payload: CreateRegularCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.status(201)
            .body(checkpointService.createCheckpoint(payload, principal.name))
    }

    @PostMapping("/update")
    suspend fun updateCheckpoint(
        @RequestBody payload: UpdateShiftCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.ok(checkpointService.updateCheckpoint(payload, principal.name))
    }

    @DeleteMapping("/{checkpointId}/delete")
    suspend fun deleteCheckpointById(
        @PathVariable checkpointId: UUID,
        principal: Principal
    ): ResponseEntity<Unit> {
        checkpointService.deleteById(checkpointId, principal.name)
        return ResponseEntity.noContent().build()
    }

}