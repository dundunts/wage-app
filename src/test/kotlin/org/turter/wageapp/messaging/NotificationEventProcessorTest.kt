package org.turter.wageapp.messaging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.messaging.client.TelegramNotificationClient
import org.turter.wageapp.messaging.service.NotificationEventProcessor
import org.turter.wageapp.messaging.service.NotificationEventPublisherImpl
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationEventProcessorTest {

    @Test
    fun `processing continues after a notification fails`() = runTest {
        assertProcessingContinuesAfter(
            Mono.error(IllegalStateException("Telegram service is unavailable"))
        )
    }

    @Test
    fun `processing continues after a notification times out`() = runTest {
        assertProcessingContinuesAfter(Mono.never()) {
            advanceTimeBy(10_000)
            runCurrent()
        }
    }

    private suspend fun TestScope.assertProcessingContinuesAfter(
        firstResponse: Mono<ClientResponse>,
        afterFirstAttempt: suspend TestScope.() -> Unit = {}
    ) {
        var requestCount = 0
        val client = TelegramNotificationClient(
            WebClient.builder()
                .exchangeFunction {
                    requestCount++
                    if (requestCount == 1) {
                        firstResponse
                    } else {
                        Mono.just(ClientResponse.create(HttpStatus.ACCEPTED).build())
                    }
                }
                .build(),
            Duration.ofSeconds(10)
        )
        val publisher = NotificationEventPublisherImpl()
        NotificationEventProcessor(publisher, client, backgroundScope)
        runCurrent()

        publisher.publish(sessionOpenedNotificationEvent(FIRST_SESSION_ID))
        runCurrent()
        afterFirstAttempt()
        publisher.publish(sessionOpenedNotificationEvent(SECOND_SESSION_ID))
        runCurrent()

        assertEquals(2, requestCount)
    }

    private companion object {
        val FIRST_SESSION_ID = UUID.fromString("98957342-7fbc-4426-aad2-651752d01f64")
        val SECOND_SESSION_ID = UUID.fromString("10648c54-2f90-44bd-b5f1-f0477c47c9d0")
    }

}
