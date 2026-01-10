package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun defaultWebClientBuilder(): WebClient.Builder =
        WebClient.builder()

}