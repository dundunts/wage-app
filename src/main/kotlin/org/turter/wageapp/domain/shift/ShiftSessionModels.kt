package org.turter.wageapp.domain.shift

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

data class ShiftSession(
    val id: UUID,
    val companyId: UUID,
    val startWorkTime: LocalTime,
    val date: LocalDate,
    val status: Status,
    val checkpoints: List<Checkpoint>
) {
    enum class Status {
        OPENED, CLOSED, RECALCULATING, OPENED_DRAFT, RECALCULATING_DRAFT;

        fun close(): Status = CLOSED

        fun draft(): Status =
            when (this) {
                OPENED -> OPENED_DRAFT
                RECALCULATING -> RECALCULATING_DRAFT
                else -> this
            }

        fun cancelDraft(): Status =
            when (this) {
                OPENED_DRAFT -> OPENED
                RECALCULATING_DRAFT -> RECALCULATING
                else -> this
            }

        fun recalculating(): Status =
            when (this) {
                CLOSED -> RECALCULATING
                else -> this
            }

        fun recalculatingAvailable(): Boolean = this == CLOSED

        fun modifyAvailable(): Boolean =
            when (this) {
                OPENED, RECALCULATING -> true
                else -> false
            }
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