package org.turter.wageapp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.turter.wageapp.config.CommonWageAppIT

class WageAppApplicationTests : CommonWageAppIT() {

    @Test
    fun contextLoads() {
    }

    @Test
    fun unauthenticatedApiRequestIsRejected() {
        client.get()
            .uri("/api/v1/company/get/page")
            .exchange()
            .expectStatus().isUnauthorized
    }

}
