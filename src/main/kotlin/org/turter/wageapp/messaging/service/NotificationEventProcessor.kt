package org.turter.wageapp.messaging.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.turter.wageapp.messaging.client.TelegramNotificationClient

@Component
class NotificationEventProcessor(
    private val publisher: NotificationEventPublisherImpl,
    private val client: TelegramNotificationClient,
    @param:Qualifier("applicationCoroutineScope") private val scope: CoroutineScope
) {

    init {
        scope.launch {
            publisher.eventsFlow.collect { event ->
                client.notify(event)
            }
        }
    }

}