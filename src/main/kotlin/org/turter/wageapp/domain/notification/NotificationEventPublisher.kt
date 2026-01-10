package org.turter.wageapp.domain.notification

import kotlinx.coroutines.flow.SharedFlow

interface NotificationEventPublisher {

    val eventsFlow: SharedFlow<NotificationEvent>

    fun publish(event: NotificationEvent)

}