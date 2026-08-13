package org.turter.wageapp.messaging.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.turter.wageapp.messaging.client.TelegramNotificationClient

@Configuration
class TelegramNotificationClientConfig {

    @Bean
    fun telegramNotificationClient(
        defaultWebClientBuilder: WebClient.Builder,
        clientRegistrationRepository: ReactiveClientRegistrationRepository,
        authorizedClientService: ReactiveOAuth2AuthorizedClientService,
        props: TelegramNotificationClientProps
    ): TelegramNotificationClient {
        val filter = ServerOAuth2AuthorizedClientExchangeFilterFunction(
            AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
            )
        )

        filter.setDefaultClientRegistrationId("keycloak")

        return TelegramNotificationClient(
            defaultWebClientBuilder
                .baseUrl(props.url)
                .filter(filter)
                .build(),
            props.timeout
        )
    }

}
