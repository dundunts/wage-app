package org.turter.wageapp.application.service.shift

import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import java.util.UUID

interface CheckpointService {

    suspend fun createCheckpoint(payload: CreateRegularCheckpointPayload, userId: String): Checkpoint

    suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): Checkpoint

    suspend fun deleteById(checkpointId: UUID, userId: String)

}