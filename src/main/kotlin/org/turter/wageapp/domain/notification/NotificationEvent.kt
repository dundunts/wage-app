package org.turter.wageapp.domain.notification

import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

sealed interface NotificationEvent {

    val meta: Meta

    data class SessionOpened(
        override val meta: Meta,
        val sessionId: UUID,
        val startWorkTime: LocalTime,
        val date: LocalDate
    ) : NotificationEvent

    data class CheckpointSaved(
        override val meta: Meta,
        val checkpoint: Checkpoint
    ) : NotificationEvent

    data class CheckpointReplaced(
        override val meta: Meta,
        val replacedCheckpointId: UUID,
        val checkpoint: Checkpoint
    ) : NotificationEvent

    data class CheckpointDeleted(
        override val meta: Meta,
        val deletedId: UUID
    ) : NotificationEvent

    data class ShiftResultCreated(
        override val meta: Meta,
        val shiftResult: ShiftResultDetailed
    ) : NotificationEvent

    data class Meta(
        val companyId: UUID,
        val createdAt: Instant
    )
}