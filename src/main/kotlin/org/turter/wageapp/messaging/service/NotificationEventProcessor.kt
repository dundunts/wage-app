package org.turter.wageapp.messaging.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.messaging.client.TelegramNotificationClient

@Component
class NotificationEventProcessor(
    private val publisher: NotificationEventPublisherImpl,
    private val client: TelegramNotificationClient,
    @param:Qualifier("applicationCoroutineScope") private val scope: CoroutineScope
) {
    private val log = LoggerFactory.getLogger(NotificationEventProcessor::class.java)

    init {
        scope.launch {
            publisher.eventsFlow.collect { event ->
                try {
                    client.notify(event)
                } catch (e: TimeoutCancellationException) {
                    logDeliveryFailure(event, e)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logDeliveryFailure(event, e)
                }
            }
        }
    }

    private fun logDeliveryFailure(event: NotificationEvent, e: Exception) {
        log.error(
            "Failed to send notification event type={} companyId={}",
            event::class.simpleName,
            event.meta.companyId,
            e
        )
    }

}
