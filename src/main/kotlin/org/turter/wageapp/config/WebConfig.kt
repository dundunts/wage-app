package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class WebConfig {

    @Bean
    fun defaultCorsWebFilter(): CorsWebFilter {
        val corsConfigurationSource = UrlBasedCorsConfigurationSource()
        val globalCorsConfiguration = CorsConfiguration()

        //TODO configure CORS
        globalCorsConfiguration.setAllowedOriginPatterns(listOf("*"))

        globalCorsConfiguration.addAllowedHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)
        globalCorsConfiguration.addAllowedHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)
        globalCorsConfiguration.addAllowedHeader(HttpHeaders.AUTHORIZATION)
        globalCorsConfiguration.addAllowedHeader(HttpHeaders.CONTENT_TYPE)

        globalCorsConfiguration.addAllowedMethod(HttpMethod.OPTIONS)
        globalCorsConfiguration.addAllowedMethod(HttpMethod.GET)
        globalCorsConfiguration.addAllowedMethod(HttpMethod.POST)
        globalCorsConfiguration.addAllowedMethod(HttpMethod.PATCH)
        globalCorsConfiguration.addAllowedMethod(HttpMethod.PUT)
        globalCorsConfiguration.addAllowedMethod(HttpMethod.DELETE)

        globalCorsConfiguration.allowCredentials = true

        corsConfigurationSource.registerCorsConfiguration("/**", globalCorsConfiguration)
        return CorsWebFilter(corsConfigurationSource)
    }

}