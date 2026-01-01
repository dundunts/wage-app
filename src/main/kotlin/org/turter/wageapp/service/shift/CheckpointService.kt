package org.turter.wageapp.service.shift

import org.turter.wageapp.domain.shift.CreateFirstShiftCheckpointPayload
import org.turter.wageapp.domain.shift.CreateRegularShiftCheckpointPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload

interface CheckpointService {

    suspend fun createFirstCheckpoint(payload: CreateFirstShiftCheckpointPayload, userId: String): ShiftSession

    suspend fun createCheckpoint(payload: CreateRegularShiftCheckpointPayload, userId: String): ShiftSession

    suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): ShiftSession

}