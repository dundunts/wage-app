package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

@Configuration
class SecurityConfig {

    @Bean
    fun defaultWebFilterChain(httpSecurity: ServerHttpSecurity): SecurityWebFilterChain {
        return httpSecurity
            .authorizeExchange { customizer ->
                customizer
                    .pathMatchers("/test/private").authenticated()
                    .pathMatchers("/test/public").permitAll()
            }
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .oauth2ResourceServer { cutomizer -> cutomizer.jwt(Customizer.withDefaults()) }
            .build()
    }

}