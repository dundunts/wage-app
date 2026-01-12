package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.messaging.client.TelegramNotificationClient

@Configuration
class ClientTestConfig {

    @Bean
    @Primary
    fun telegramNotificationTestClient(defaultWebClientBuilder: WebClient.Builder): TelegramNotificationClient =
        TelegramNotificationClient(
            defaultWebClientBuilder.baseUrl("http://localhost:12345").build()
        )

}