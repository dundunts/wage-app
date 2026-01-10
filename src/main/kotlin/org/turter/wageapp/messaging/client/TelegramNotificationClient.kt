package org.turter.wageapp.messaging.client

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.messaging.model.TelegramNotificationEvent
import java.util.UUID

class TelegramNotificationClient(
    private val webClient: WebClient
) {

    suspend fun notify(event: TelegramNotificationEvent) {
        webClient
            .post()
            .uri("/api/notifications")
            .bodyValue(event)
            .retrieve()
            .toBodilessEntity()
            .awaitSingleOrNull()
    }

    suspend fun notify(
        companyId: UUID,
        messageText: String
    ) {
        notify(
            TelegramNotificationEvent(
                companyId = companyId,
                messageText = messageText
            )
        )
    }
}