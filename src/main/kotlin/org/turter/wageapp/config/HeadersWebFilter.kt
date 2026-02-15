package org.turter.wageapp.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.lang.String

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HeadersWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val headers = exchange.response.headers
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        headers.add(
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            String.join(
                ", ", listOf(
                    HttpMethod.OPTIONS.name(),
                    HttpMethod.GET.name(),
                    HttpMethod.POST.name(),
                    HttpMethod.PUT.name(),
                    HttpMethod.PATCH.name(),
                    HttpMethod.DELETE.name()
                )
            )
        )
        headers.add(
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            String.join(", ", listOf(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE))
        )
        return chain.filter(exchange)
    }
}