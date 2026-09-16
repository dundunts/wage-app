package org.turter.wageapp.controller

import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.turter.wageapp.application.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.application.data.shift.ShiftSessionRepository
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.tips.proto.GetTipsByDurationRequest
import org.turter.wageapp.tips.proto.GetTipsByDurationResponse
import org.turter.wageapp.tips.proto.TipsServiceGrpc
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class SessionQrTipsControllerIT : CommonWageAppIT() {
    @Autowired
    private lateinit var sessions: ShiftSessionRepository

    companion object {
        private val requests = CopyOnWriteArrayList<GetTipsByDurationRequest>()
        @Volatile private var tips: Long = 12345
        @Volatile private var failure: Status? = null
        private val server = ServerBuilder.forPort(0)
            .addService(object : TipsServiceGrpc.TipsServiceImplBase() {
                override fun getTipsByDuration(
                    request: GetTipsByDurationRequest,
                    responseObserver: StreamObserver<GetTipsByDurationResponse>,
                ) {
                    requests.add(request)
                    val error = failure
                    if (error != null) {
                        responseObserver.onError(error.withDescription("private upstream detail").asRuntimeException())
                    } else {
                        responseObserver.onNext(GetTipsByDurationResponse.newBuilder().setTips(tips).build())
                        responseObserver.onCompleted()
                    }
                }
            }).build().start()

        @JvmStatic
        @DynamicPropertySource
        fun tipsProperties(registry: DynamicPropertyRegistry) {
            registry.add("tips.bot.base-url") { "localhost:${server.port}" }
        }

        @JvmStatic
        @AfterAll
        fun stopTipsServer() {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @BeforeEach
    fun setup() {
        sessions.deleteAll().block()
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
        requests.clear()
        tips = 12345
        failure = null
    }

    private fun session(status: ShiftSession.Status = ShiftSession.Status.CLOSED): ShiftSessionDbEntity {
        val (_, company) = saveNewUserEmployeeAndCompany()
        return sessions.save(ShiftSessionDbEntity(
            company.id!!, status, LocalTime.of(0, 30), LocalDate.of(2026, 1, 1),
        )).block()!!
    }

    @ParameterizedTest
    @EnumSource(ShiftSession.Status::class)
    fun `reads any session status over a fixed Moscow day without modifying it`(status: ShiftSession.Status) {
        val session = session(status)

        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isOk
            .expectBody().json("""{"tips":123}""")

        val request = requests.single()
        assertEquals(session.companyId.toString(), request.companyId)
        assertEquals(Instant.parse("2025-12-31T21:30:00Z").epochSecond, request.startTime.seconds)
        assertEquals(0, request.startTime.nanos)
        assertEquals(86400, request.duration.seconds)
        assertEquals(0, request.duration.nanos)
        val after = sessions.findById(session.id!!).block()!!
        assertEquals(status, after.status)
        assertEquals(session.date, after.date)
        assertEquals(session.startWorkTime, after.startWorkTime)
    }

    @ParameterizedTest
    @CsvSource("0,0", "99,0", "100,1", "199,1", "12399,123", "9223372036854775807,92233720368547758")
    fun `discards kopecks from the total and preserves long amounts`(kopecks: Long, rubles: Long) {
        val session = session()
        tips = kopecks
        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isOk
            .expectBody().json("""{"tips":$rubles}""")
    }

    @Test
    fun `rejects unknown sessions before contacting bot`() {
        client.withUser().get().uri("/api/v1/session/${UUID.randomUUID()}/qr-tips").exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        assertEquals(0, requests.size)
    }

    @Test
    fun `rejects access to another company before contacting bot`() {
        val session = session()
        employeeCompanyRepository.deleteAll().block()
        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isEqualTo(409)
        assertEquals(0, requests.size)
    }

    @Test
    fun `requires authentication`() {
        client.get().uri("/api/v1/session/${UUID.randomUUID()}/qr-tips").exchange()
            .expectStatus().isUnauthorized
        assertEquals(0, requests.size)
    }

    @Test
    fun `rejects malformed session id`() {
        client.withUser().get().uri("/api/v1/session/not-a-uuid/qr-tips").exchange()
            .expectStatus().isBadRequest
        assertEquals(0, requests.size)
    }

    @Test
    fun `cannot query tips without a session id`() {
        client.withUser().get().uri("/api/v1/session/qr-tips").exchange()
            .expectStatus().isNotFound
        assertEquals(0, requests.size)
    }

    @Test
    fun `unavailable bot returns 502 after two retries`() {
        val session = session()
        failure = Status.UNAVAILABLE
        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isEqualTo(502)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.detail").isEqualTo("Tips bot request failed")
        assertEquals(3, requests.size)
    }

    @Test
    fun `deadline failure returns 504 after two retries`() {
        val session = session()
        failure = Status.DEADLINE_EXCEEDED
        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isEqualTo(504)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.detail").isEqualTo("Tips bot request timed out")
        assertEquals(3, requests.size)
    }

    @Test
    fun `permanent bot error returns 502 without retrying`() {
        val session = session()
        failure = Status.INTERNAL
        client.withUser().get().uri("/api/v1/session/${session.id}/qr-tips").exchange()
            .expectStatus().isEqualTo(502)
            .expectBody().jsonPath("$.detail").isEqualTo("Tips bot request failed")
        assertEquals(1, requests.size)
    }
}
