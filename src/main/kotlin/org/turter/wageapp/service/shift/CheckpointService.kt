package org.turter.wageapp.service.shift

import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload

interface CheckpointService {

//    suspend fun createFirstCheckpoint(payload: CreateFirstShiftCheckpointPayload, userId: String): Checkpoint

    suspend fun createCheckpoint(payload: CreateRegularCheckpointPayload, userId: String): Checkpoint

    suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): Checkpoint

}