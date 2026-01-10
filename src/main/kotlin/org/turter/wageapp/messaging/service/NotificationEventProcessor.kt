package org.turter.wageapp.messaging.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.turter.wageapp.messaging.service.NotificationEventTextMessageFactory
import org.turter.wageapp.messaging.client.TelegramNotificationClient
import org.turter.wageapp.messaging.model.TelegramNotificationEvent

@Component
class NotificationEventProcessor(
    private val publisher: NotificationEventPublisherImpl,
    private val textMessageFactory: NotificationEventTextMessageFactory,
    private val client: TelegramNotificationClient,
    @param:Qualifier("applicationCoroutineScope") private val scope: CoroutineScope
) {

    init {
        scope.launch {
            publisher.eventsFlow.collect { event ->
                val textMessage = textMessageFactory.getTextMessage(event)
                val telegramNotificationEvent = TelegramNotificationEvent(
                    event.meta.companyId,
                    textMessage
                )

                client.notify(telegramNotificationEvent)
            }
        }
    }

}