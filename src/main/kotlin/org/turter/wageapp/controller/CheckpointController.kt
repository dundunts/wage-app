package org.turter.wageapp.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
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
class CheckpointController(
    private val calculatingShiftService: CheckpointService
) {

//    @PostMapping("/create/first")
//    suspend fun createFirstForSessionCheckpoint(
//        payload: CreateFirstShiftCheckpointPayload,
//        principal: Principal
//    ): ResponseEntity<ShiftSession> {
//        return ResponseEntity.status(201)
//            .body(calculatingShiftService.createFirstCheckpoint(payload, principal.name))
//    }

    @PostMapping("/create")
    suspend fun createCheckpointForSession(
        payload: CreateRegularCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.status(201)
            .body(calculatingShiftService.createCheckpoint(payload, principal.name))
    }

    @PostMapping("/update")
    suspend fun updateCheckpoint(
        payload: UpdateShiftCheckpointPayload,
        principal: Principal
    ): ResponseEntity<Checkpoint> {
        return ResponseEntity.ok(calculatingShiftService.updateCheckpoint(payload, principal.name))
    }

}