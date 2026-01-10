package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.application.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftSession
import java.time.Instant

@Mapper(
    componentModel = "spring",
    uses = [CheckpointMapper::class]
)
abstract class SessionMapper {

    abstract fun toShiftSession(entity: ShiftSessionDbEntity, checkpoints: List<Checkpoint>): ShiftSession

    fun toOpenedSessionNotificationEvent(session: ShiftSessionDbEntity): NotificationEvent =
        NotificationEvent.SessionOpened(
            meta = NotificationEvent.Meta(session.companyId!!, Instant.now()),
            sessionId = session.id!!,
            startWorkTime = session.startWorkTime!!,
            date = session.date!!
        )

}