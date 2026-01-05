package org.turter.wageapp.config;

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer


@Configuration
class PageableArgumentResolverConfig : WebFluxConfigurer {
    @Bean
    fun defaultPageRequest(): PageRequest {
        return PageRequest.of(0, 100)
    }

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        val argumentResolver = ReactiveSortHandlerMethodArgumentResolver()
        argumentResolver.setSortParameter("sort")
        val resolver =
            ReactivePageableHandlerMethodArgumentResolver(argumentResolver)
        resolver.setFallbackPageable(defaultPageRequest())
        resolver.setPageParameterName("page")
        resolver.setSizeParameterName("size")
        configurer.addCustomResolver(resolver)
    }
}