package org.turter.wageapp.config

import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@Configuration
class SecurityTestConfig {

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder = mock()

    @Bean
    fun reactiveClientRegistrationRepository(): ReactiveClientRegistrationRepository = mock()

//    @Bean
//    fun authorizedClientRepository(): ServerOAuth2AuthorizedClientRepository = mock()

}