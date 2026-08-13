package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.turter.wageapp.application.data.shift.ShiftSessionRepository
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.transport.model.ShiftSession as TransportShiftSession
import org.turter.wageapp.transport.model.ShiftSessionStatus
import org.turter.wageapp.utils.session.CreateRecalculatingShiftSessionPayloadSupplier
import org.turter.wageapp.utils.session.OpenNewShiftSessionPayloadSupplier
import org.turter.wageapp.utils.session.ShiftSessionEntityFactory
import org.turter.wageapp.utils.session.UpdateShiftSessionStartWorkTimePayloadSupplier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class SessionControllerIT : CommonWageAppIT() {

    @Autowired
    private lateinit var shiftSessionRepository: ShiftSessionRepository

    @BeforeEach
    fun setup() {
        shiftSessionRepository.deleteAll().block()
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
    }

    @Test
    @DisplayName("Успешно возвращает открытую сессию со статусом OPENED")
    fun getOpenedSession_returnsOpenedSession() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = ShiftSessionEntityFactory.opened(company.id!!)
        val savedSession = shiftSessionRepository.save(session).block()!!

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/opened")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(TransportShiftSession::class.java)
            .returnResult()
            .responseBody

        assertNotNull(response)
        assertEquals(savedSession.id, response?.id)
        assertEquals(company.id, response?.companyId)
        assertEquals(ShiftSessionStatus.OPENED, response?.status)
    }

    @Test
    @DisplayName("Успешно возвращает открытую сессию со статусом OPENED_DRAFT")
    fun getOpenedSession_returnsOpenedDraftSession() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = ShiftSessionEntityFactory.opened(
            companyId = company.id!!,
            status = ShiftSession.Status.OPENED_DRAFT
        )

        val savedSession = shiftSessionRepository.save(session).block()!!

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/opened")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(TransportShiftSession::class.java)
            .returnResult()
            .responseBody

        assertNotNull(response)
        assertEquals(savedSession.id, response?.id)
        assertEquals(ShiftSessionStatus.OPENED_DRAFT, response?.status)
    }

    @Test
    @DisplayName("Возвращает 404, если открытая сессия для компании отсутствует")
    fun getOpenedSession_returns404IfNotFound() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/opened")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если для компании существует несколько открытых сессий")
    fun getOpenedSession_returns409IfSeveralOpenedSessions() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.opened(company.id!!)
        ).block()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.opened(
                company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()

        client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/opened")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании")
    fun getOpenedSession_returns409IfUserHasNoAccess() {
        val company = saveNewCompany()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.opened(company.id!!)
        ).block()

        client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/opened")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно возвращает доступную сессию со статусом OPENED")
    fun getAvailableById_returnsOpenedSession() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        val response = client.withUser()
            .get()
            .uri("/api/v1/session/get/available/{id}", session.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(TransportShiftSession::class.java)
            .returnResult()
            .responseBody

        assertNotNull(response)
        assertEquals(session.id, response?.id)
        assertEquals(ShiftSessionStatus.OPENED, response?.status)
    }

    @Test
    @DisplayName("Возвращает 409, если сессия имеет статус CLOSED")
    fun getAvailableById_returns409IfClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()!!

        client.withUser()
            .get()
            .uri("/api/v1/session/get/available/{id}", session.id)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании сессии")
    fun getAvailableById_returns409IfNoCompanyAccess() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        client.withUser()
            .get()
            .uri("/api/v1/session/get/available/{id}", session.id)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 404, если сессия не существует")
    fun getAvailableById_returns404IfSessionNotFound() {
        client.withUser()
            .get()
            .uri("/api/v1/session/get/available/{id}", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно возвращает все доступные сессии компании")
    fun getAllAvailableSessions_returnsAvailableSessions() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.RECALCULATING)
        ).block()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/available/all")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(object : ParameterizedTypeReference<List<TransportShiftSession>>() {})
            .returnResult()
            .responseBody!!

        assertEquals(2, response.size)
        response.forEach {
            assert(it.status != ShiftSessionStatus.CLOSED)
        }
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании")
    fun getAllAvailableSessions_returns409IfNoAccess() {
        val company = saveNewCompany()

        client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/available/all")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно возвращает пустой список, если доступные сессии отсутствуют")
    fun getAllAvailableSessions_returnsEmptyList() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/session/get/available/all")
                    .queryParam("companyId", company.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(object : ParameterizedTypeReference<List<TransportShiftSession>>() {})
            .returnResult()
            .responseBody!!

        assertEquals(0, response.size)
    }

    @Test
    @DisplayName("Успешно открывает новую смену и отправляет уведомление")
    fun openNewSession_success() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val payload = OpenNewShiftSessionPayloadSupplier.valid(
            companyId = company.id!!,
            startWorkAt = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0))
        )

        val expectedSessionId = UUID.randomUUID()
        val startWorkAt = LocalDateTime.parse(payload.startWorkAt as String)

        val notificationEvent = NotificationEvent.SessionOpened(
            meta = NotificationEvent.Meta(company.id!!, Instant.now()),
            sessionId = expectedSessionId,
            startWorkTime = startWorkAt.toLocalTime(),
            date = startWorkAt.toLocalDate()
        )

        setupStubTgBotAPI(notificationEvent)

        val response = client.withUser()
            .post()
            .uri("/api/v1/session/open")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(TransportShiftSession::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(company.id, response.companyId)
        assertEquals(ShiftSessionStatus.OPENED, response.status)

        awaitVerifyRequestedStubTgBot(notificationEvent)
    }

    @Test
    @DisplayName("Возвращает 400, если время начала новой сессии не является строкой")
    fun openNewSession_returns400IfStartWorkAtIsNotString() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client.withUser()
            .post()
            .uri("/api/v1/session/open")
            .bodyValue(mapOf("companyId" to company.id, "startWorkAt" to 10))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если для компании уже существует открытая сессия")
    fun openNewSession_returns409IfSessionAlreadyOpened() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()

        val payload = OpenNewShiftSessionPayloadSupplier.valid(company.id!!)

        client.withUser()
            .post()
            .uri("/api/v1/session/open")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не привязан к компании")
    fun openNewSession_returns409IfNoAccess() {
        val company = saveNewCompany()

        val payload = OpenNewShiftSessionPayloadSupplier.valid(company.id!!)

        client.withUser()
            .post()
            .uri("/api/v1/session/open")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно открывает сессию пересчёта для закрытой смены")
    fun openRecalculatingSession_success() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val closedSession = shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()!!

        val payload = CreateRecalculatingShiftSessionPayloadSupplier.valid(closedSession.id!!)

        val response = client.withUser()
            .post()
            .uri("/api/v1/session/recalculating")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(TransportShiftSession::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(ShiftSessionStatus.RECALCULATING, response.status)
    }

    @Test
    @DisplayName("Возвращает 409, если сессия не имеет статус CLOSED")
    fun openRecalculatingSession_returns409IfSessionNotClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val openedSession = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        val payload = CreateRecalculatingShiftSessionPayloadSupplier.valid(openedSession.id!!)

        client.withUser()
            .post()
            .uri("/api/v1/session/recalculating")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании пересчитываемой сессии")
    fun openRecalculatingSession_returns409IfNoAccess() {
        val company = saveNewCompany()

        val closedSession = shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()!!

        val payload = CreateRecalculatingShiftSessionPayloadSupplier.valid(closedSession.id!!)

        client.withUser()
            .post()
            .uri("/api/v1/session/recalculating")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно закрывает открытую сессию")
    fun closeSession_success() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        client.withUser()
            .put()
            .uri("/api/v1/session/{id}/close", session.id)
            .exchange()
            .expectStatus().isNoContent

        val updated = shiftSessionRepository.findById(session.id!!).block()!!
        assertEquals(ShiftSession.Status.CLOSED, updated.status)
    }

    @Test
    @DisplayName("Возвращает 409, если сессия уже закрыта")
    fun closeSession_returns409IfAlreadyClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()!!

        client.withUser()
            .put()
            .uri("/api/v1/session/{id}/close", session.id)
            .exchange()
            .expectStatus().isNoContent

        val actual = shiftSessionRepository.findById(session.id!!).block()!!

        assertEquals(ShiftSession.Status.CLOSED, actual.status)
    }

    @Test
    @DisplayName("Возвращает 204, если сессия находится в статусе RECALCULATING")
    fun closeSession_returns409IfRecalculating() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.RECALCULATING)
        ).block()!!

        client.withUser()
            .put()
            .uri("/api/v1/session/{id}/close", session.id)
            .exchange()
            .expectStatus().isNoContent

        val closed = shiftSessionRepository.findById(session.id!!).block()!!

        assertEquals(ShiftSession.Status.CLOSED, closed.status)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании")
    fun closeSession_returns409IfNoAccess() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        client.withUser()
            .put()
            .uri("/api/v1/session/{id}/close", session.id)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 404, если сессия не существует")
    fun closeSession_returns404IfNotFound() {
        client.withUser()
            .put()
            .uri("/api/v1/session/{id}/close", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Успешно обновляет время начала работы смены")
    fun updateSessionStartWorkTime_success() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        val payload = UpdateShiftSessionStartWorkTimePayloadSupplier.valid(
            sessionId = session.id!!,
            startWorkTime = LocalTime.of(11, 30)
        )

        client.withUser()
            .put()
            .uri("/api/v1/session/update/time")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNoContent

        val updated = shiftSessionRepository.findById(session.id!!).block()!!
        assertEquals(LocalTime.of(11, 30), updated.startWorkTime)
    }

    @Test
    @DisplayName("Возвращает 400, если новое время начала сессии некорректно")
    fun updateSessionStartWorkTime_returns400IfTimeIsInvalid() {
        val (_, company) = saveNewUserEmployeeAndCompany()
        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        client.withUser()
            .put()
            .uri("/api/v1/session/update/time")
            .bodyValue(mapOf("sessionId" to session.id, "startWorkTime" to "25:00"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если сессия закрыта")
    fun updateSessionStartWorkTime_returns409IfClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.closed(company.id!!)
        ).block()!!

        val payload = UpdateShiftSessionStartWorkTimePayloadSupplier.valid(session.id!!)

        client.withUser()
            .put()
            .uri("/api/v1/session/update/time")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 409, если пользователь не имеет доступа к компании")
    fun updateSessionStartWorkTime_returns409IfNoAccess() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.available(company.id!!, ShiftSession.Status.OPENED)
        ).block()!!

        val payload = UpdateShiftSessionStartWorkTimePayloadSupplier.valid(session.id!!)

        client.withUser()
            .put()
            .uri("/api/v1/session/update/time")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Возвращает 404, если сессия не найдена")
    fun updateSessionStartWorkTime_returns404IfNotFound() {
        val payload = UpdateShiftSessionStartWorkTimePayloadSupplier.valid(UUID.randomUUID())

        client.withUser()
            .put()
            .uri("/api/v1/session/update/time")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

}
