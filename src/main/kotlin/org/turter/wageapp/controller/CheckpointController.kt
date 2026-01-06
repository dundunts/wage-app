package org.turter.wageapp.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import org.turter.wageapp.service.shift.CheckpointService
import java.security.Principal

@RestController
@RequestMapping("/api/v1/checkpoint")
//TODO add endpoint for delete by id
class CheckpointController(
    private val calculatingShiftService: CheckpointService
) {

    @PostMapping("/create")
    suspend fun createCheckpointForSession(
        @RequestBody payload: CreateRegularCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.status(201)
            .body(calculatingShiftService.createCheckpoint(payload, principal.name))
    }

    @PostMapping("/update")
    suspend fun updateCheckpoint(
        @RequestBody payload: UpdateShiftCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.ok(calculatingShiftService.updateCheckpoint(payload, principal.name))
    }

}