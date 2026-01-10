package org.turter.wageapp.messaging.mapper

import org.springframework.stereotype.Component
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.messaging.model.TelegramNotificationEvent

@Component
class NotificationEventMapper {

    fun toTelegramNotificationEvent(event: NotificationEvent): TelegramNotificationEvent =
        when(event) {
            is NotificationEvent.SessionOpened -> TODO()
            is NotificationEvent.CheckpointSaved -> TODO()
            is NotificationEvent.CheckpointReplaced -> TODO()
            is NotificationEvent.CheckpointDeleted -> TODO()
            is NotificationEvent.ShiftResultCreated -> TODO()
        }

}