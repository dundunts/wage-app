package org.turter.wageapp.config

import io.netty.handler.logging.LogLevel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.messaging.client.TelegramNotificationClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

@Configuration
class ClientTestConfig {

    @Bean
    @Primary
    fun testWebClientBuilder(): WebClient.Builder {
        val httpClient = HttpClient
            .create()
            .wiretap("reactor.netty.http.client.HttpClient",
                LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)

        return WebClient
            .builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
    }

    @Bean
    @Primary
    fun telegramNotificationTestClient(testWebClientBuilder: WebClient.Builder): TelegramNotificationClient =
        TelegramNotificationClient(
            testWebClientBuilder.baseUrl("http://localhost:12345").build()
        )

}