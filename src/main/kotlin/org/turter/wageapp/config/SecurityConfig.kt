package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@Configuration
class SecurityConfig {

    @Bean
    fun defaultWebFilterChain(httpSecurity: ServerHttpSecurity): SecurityWebFilterChain {
        return httpSecurity
            .authorizeExchange { customizer ->
                customizer
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .pathMatchers(HttpMethod.GET, "/test/private").authenticated()
                    .pathMatchers("/test/public").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().permitAll()
            }
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .oauth2ResourceServer { customizer -> customizer.jwt(Customizer.withDefaults()) }
            .exceptionHandling { exceptionHandlingSpec ->
                exceptionHandlingSpec
                    .authenticationEntryPoint { exchange, ex ->
                        Mono.fromRunnable{ exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)}
                    }
                    .accessDeniedHandler { exchange, ex ->
                        Mono.fromRunnable{ exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN) }
                    }
            }
            .build()
    }

}