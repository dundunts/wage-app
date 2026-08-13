package org.turter.wageapp.openapi

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.configuration.SpringDocKotlinConfiguration
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.webflux.core.configuration.SpringDocWebFluxConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.turter.wageapp.application.controller.CompanyController
import org.turter.wageapp.application.service.company.CompanyService
import java.nio.file.Files
import java.nio.file.Path

@Tag("openapi-extraction")
@ActiveProfiles("openapi-docs")
@WebFluxTest(controllers = [CompanyController::class])
@ImportAutoConfiguration(
    SpringDocConfiguration::class,
    SpringDocKotlinConfiguration::class,
    SpringDocConfigProperties::class,
    SpringDocWebFluxConfiguration::class,
)
class CompanyOpenApiExtractionTest {

    @Autowired
    private lateinit var client: WebTestClient

    @MockitoBean
    private lateinit var companyService: CompanyService

    @Test
    fun `extract current Company operations`() {
        val document = client.mutateWith(mockJwt()).get()
            .uri("/v3/api-docs")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody
            ?: error("Springdoc returned an empty document")

        val output = Path.of(requireNotNull(System.getProperty("openapi.extraction.output")))
        Files.createDirectories(output.parent)
        Files.write(output, document)
    }
}
