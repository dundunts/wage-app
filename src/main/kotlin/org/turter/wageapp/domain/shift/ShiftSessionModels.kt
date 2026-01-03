package org.turter.wageapp.domain.shift

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

data class ShiftSession(
    val id: UUID,
    val companyId: UUID,
    val startWorkAt: LocalDateTime,
    val status: Status,
    val checkpoints: List<Checkpoint>
) {
    enum class Status {
        OPENED, CLOSED, RECALCULATING, DRAFTED
    }
}

data class OpenNewShiftSessionPayload(
    val companyId: UUID,
    val startWorkAt: LocalDateTime
)

data class CreateRecalculatingShiftSessionPayload(
    val closedSessionId: UUID
)

data class UpdateShiftSessionStartWorkTimePayload(
    val sessionId: UUID,
    val startWorkTime: LocalTime
)