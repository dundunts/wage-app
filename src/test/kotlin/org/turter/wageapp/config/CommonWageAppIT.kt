package org.turter.wageapp.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.turter.wageapp.application.data.company.CompanyDbEntity
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeCompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.employee.entity.EmployeeCompanyDbEntity
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.utils.company.CompanyEntityFactory
import org.turter.wageapp.utils.employee.EmployeeCompanyEntityFactory
import org.turter.wageapp.utils.employee.EmployeeEntityFactory
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient
@ActiveProfiles(profiles = ["test-it"])
@Testcontainers
@EnableWireMock(
    ConfigureWireMock(port = 12345)
)
open class CommonWageAppIT {

    @Autowired
    protected lateinit var client: WebTestClient

    @Autowired
    protected lateinit var companyRepository: CompanyRepository

    @Autowired
    protected lateinit var employeeRepository: EmployeeRepository

    @Autowired
    protected lateinit var employeeCompanyRepository: EmployeeCompanyRepository

    @InjectWireMock
    protected lateinit var wireMockServer: WireMockServer

    companion object {
        @JvmStatic
        @Container
        val POSTGRES_SQL_CONTAINER: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")

//        @JvmStatic
//        @Container
//        val REDIS_CONTAINER: RedisContainer = RedisContainer(DockerImageName.parse("redis:6.2"))
//            .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { r2dbcUrl() }
            registry.add("spring.r2dbc.username") { POSTGRES_SQL_CONTAINER.username }
            registry.add("spring.r2dbc.password") { POSTGRES_SQL_CONTAINER.password }

            registry.add("spring.liquibase.url") { POSTGRES_SQL_CONTAINER.jdbcUrl }
            registry.add("spring.liquibase.user") { POSTGRES_SQL_CONTAINER.username }
            registry.add("spring.liquibase.password") { POSTGRES_SQL_CONTAINER.password }

//            registry.add("spring.data.redis.host") { REDIS_CONTAINER.host }
//            registry.add(
//                "spring.data.redis.port"
//            ) { REDIS_CONTAINER.getMappedPort(6379).toString() }
        }

        @JvmStatic
        fun r2dbcUrl() =
            "r2dbc:postgresql://${POSTGRES_SQL_CONTAINER.host}:" +
                    "${POSTGRES_SQL_CONTAINER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/" +
                    POSTGRES_SQL_CONTAINER.databaseName
    }

    protected fun setupStubTgBotAPI(event: NotificationEvent) = setupStubTgBotAPI(event.meta.companyId)

    protected fun setupStubTgBotAPI(companyId: UUID) {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/api/notifications"))
                .withRequestBody(
                    matchingJsonPath("$.meta.companyId", equalTo(companyId.toString())),
                )
                .willReturn(aResponse().withStatus(202))
        )
    }

    protected fun awaitVerifyRequestedStubTgBot(
        event: NotificationEvent,
        timeoutDuration: Duration = 10.toDuration(DurationUnit.SECONDS)
    ) = awaitVerifyRequestedStubTgBot(companyId = event.meta.companyId, timeoutDuration = timeoutDuration)

    protected fun awaitVerifyRequestedStubTgBot(
        companyId: UUID,
        timeoutDuration: Duration = 10.toDuration(DurationUnit.SECONDS)
    ) = runBlocking {
        val requestPattern = postRequestedFor(urlPathEqualTo("/api/notifications"))
            .withRequestBody(
                matchingJsonPath("$.meta.companyId", equalTo(companyId.toString()))
            )
        val pollingIntervalMillis = 100L
        val attempts = (timeoutDuration.inWholeMilliseconds / pollingIntervalMillis).coerceAtLeast(1)

        repeat((attempts - 1).toInt()) {
            try {
                wireMockServer.verify(exactly(1), requestPattern)
                return@runBlocking
            } catch (_: VerificationException) {
                delay(pollingIntervalMillis)
            }
        }

        wireMockServer.verify(exactly(1), requestPattern)
    }

    protected fun CompanyDbEntity.addEmployee(firstName: String = "first_name"): EmployeeDbEntity {
        val employee = saveNewEmployee(firstName = firstName)
        saveNewBind(employee.id!!, this.id!!)
        return employee
    }

    protected fun saveNewUserEmployeeAndCompany(userId: String = USER_ID): Pair<EmployeeDbEntity, CompanyDbEntity> {
        val employee = saveNewUserEmployee(userId)
        val company = saveNewCompany()
        saveNewBind(employee.id!!, company.id!!)
        return employee to company
    }

    protected fun saveNewUserEmployeeAndCompanies(
        userId: String = USER_ID,
        companiesCount: Int = 1
    ): Pair<EmployeeDbEntity, List<CompanyDbEntity>> {
        val employee = saveNewUserEmployee(userId)

        val companyList = mutableListOf<CompanyDbEntity>()

        for (i in 1..companiesCount) {
            val company = saveNewCompany(title = "Company #$i")
            saveNewBind(employee.id!!, company.id!!)
            companyList.add(company)
        }

        return employee to companyList
    }

    protected fun saveNewUserEmployee(userId: String = USER_ID): EmployeeDbEntity {
        return saveNewEmployee(userId = userId)
    }

    protected fun saveNewCompany(
        title: String = "Test company",
        employeeWageCoefficientFromRevenue: Int = 10,
        defaultShiftStartTime: String = "09:00"
    ): CompanyDbEntity {
        val company = CompanyEntityFactory.create(
            title = title,
            employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue,
            defaultShiftStartTime = defaultShiftStartTime
        )

        return companyRepository.save(company).block()!!
    }

    protected fun saveNewEmployee(
        id: UUID? = null,
        userId: String? = null,
        firstName: String = "first_name",
        lastName: String = "last_name",
        patronymic: String = "first_name",
        simpleName: String = "first_name",
        position: Employee.Position = Employee.Position.WAITER_ACTIVE
    ): EmployeeDbEntity {
        val employee = EmployeeEntityFactory.create(
            id = id,
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            simpleName = simpleName,
            position = position
        )

        return employeeRepository.save(employee).block()!!
    }

    protected fun saveNewBind(employeeId: UUID, companyId: UUID): EmployeeCompanyDbEntity {
        val bind = EmployeeCompanyEntityFactory.create(
            employeeId = employeeId,
            companyId = companyId
        )

        return employeeCompanyRepository.save(bind).block()!!
    }

    protected fun <T> runConcurrently(vararg actions: () -> T): List<T> {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val futures = actions.map { action ->
            CompletableFuture.supplyAsync {
                ready.countDown()
                check(start.await(10, TimeUnit.SECONDS)) { "Timed out waiting to start concurrent test actions" }
                action()
            }
        }

        try {
            check(ready.await(10, TimeUnit.SECONDS)) { "Timed out preparing concurrent test actions" }
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
        }
    }

    protected fun assertProblemDetail(
        result: EntityExchangeResult<ByteArray>,
        status: HttpStatus,
        detail: String
    ) {
        val responseBody = requireNotNull(result.responseBody).decodeToString()
        assertEquals(status.value(), result.status.value())
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, result.responseHeaders.contentType)
        assertTrue(responseBody.contains("\"title\":\"${status.reasonPhrase}\""))
        assertTrue(responseBody.contains("\"status\":${status.value()}"))
        assertTrue(responseBody.contains(detail))
    }
}
