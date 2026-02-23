package org.turter.wageapp.messaging.client

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.turter.wageapp.domain.notification.NotificationEvent

class TelegramNotificationClient(
    private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(TelegramNotificationClient::class.java)

    suspend fun notify(event: NotificationEvent) {
        try {
            webClient
                .post()
                .uri("/api/notifications")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .awaitSingleOrNull()
        } catch (e: WebClientResponseException) {
            log.error("Catch exception while sending notification", e)
        }
    }
}