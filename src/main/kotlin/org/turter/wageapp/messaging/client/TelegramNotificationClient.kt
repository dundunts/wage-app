package org.turter.wageapp.messaging.client

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.domain.notification.NotificationEvent
import java.time.Duration

class TelegramNotificationClient(
    private val webClient: WebClient,
    private val timeout: Duration
) {

    suspend fun notify(event: NotificationEvent) {
        withTimeout(timeout.toMillis()) {
            webClient
                .post()
                .uri("/api/notifications")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .awaitSingleOrNull()
        }
    }
}
