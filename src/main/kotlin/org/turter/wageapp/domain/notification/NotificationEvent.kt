package org.turter.wageapp.domain.notification

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type" // Jackson добавит это поле в JSON
)
@JsonSubTypes(
    JsonSubTypes.Type(value = NotificationEvent.SessionOpened::class, name = "SessionOpened"),
    JsonSubTypes.Type(value = NotificationEvent.CheckpointSaved::class, name = "CheckpointSaved"),
    JsonSubTypes.Type(value = NotificationEvent.CheckpointReplaced::class, name = "CheckpointReplaced"),
    JsonSubTypes.Type(value = NotificationEvent.CheckpointDeleted::class, name = "CheckpointDeleted"),
    JsonSubTypes.Type(value = NotificationEvent.ShiftResultCreated::class, name = "ShiftResultCreated")
)
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