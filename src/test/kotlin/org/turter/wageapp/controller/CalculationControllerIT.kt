package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.turter.wageapp.application.data.shift.*
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.shift.ConfirmDraftResponse
import org.turter.wageapp.domain.shift.ShiftResultDraft
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.utils.checkpoint.CheckpointEmployeeEntityFactory
import org.turter.wageapp.utils.checkpoint.CheckpointEntityFactory
import org.turter.wageapp.utils.draft.PaymentDraftEntityFactory
import org.turter.wageapp.utils.draft.ShiftResultDraftEntityFactory
import org.turter.wageapp.utils.session.ShiftSessionEntityFactory
import java.util.*

class CalculationControllerIT : CommonWageAppIT() {

    @Autowired
    lateinit var shiftSessionRepository: ShiftSessionRepository

    @Autowired
    lateinit var shiftResultDraftRepository: ShiftResultDraftRepository

    @Autowired
    lateinit var paymentDraftRepository: PaymentDraftRepository

    @Autowired
    lateinit var shiftResultRepository: ShiftResultRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var checkpointRepository: CheckpointRepository

    @Autowired
    private lateinit var checkpointEmployeeRepository: CheckpointEmployeeRepository

    @BeforeEach
    fun setup() {
        paymentRepository.deleteAll().block()
        shiftResultRepository.deleteAll().block()
        paymentDraftRepository.deleteAll().block()
        shiftResultDraftRepository.deleteAll().block()
        shiftSessionRepository.deleteAll().block()

        checkpointEmployeeRepository.deleteAll().block()
        checkpointRepository.deleteAll().block()

        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
    }

    @Test
    @DisplayName("Draft рассчитывается для сессии в статусе OPENED")
    fun shouldCalculateDraftWhenSessionOpened() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = ShiftSessionEntityFactory.create(
            companyId = company.id!!,
            status = ShiftSession.Status.OPENED
        )
        val savedSession = shiftSessionRepository.save(session).block()!!

        val savedCheckpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(shiftSessionId = session.id!!)
        ).block()!!

        checkpointEmployeeRepository.save(
            CheckpointEmployeeEntityFactory.create(checkpointId = savedCheckpoint.id!!, employeeId = employee.id!!)
        ).block()

        val response = client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", savedSession.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultDraft::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(savedSession.id, response.sessionId)
        assertNotNull(response.id)
        assertTrue(response.payments.isNotEmpty())

        val draftInDb = shiftResultDraftRepository
            .findBySessionId(savedSession.id!!)
            .block()

        assertNotNull(draftInDb)

        val updatedSession = shiftSessionRepository.findById(savedSession.id!!).block()!!
        assertEquals(ShiftSession.Status.OPENED_DRAFT, updatedSession.status)
    }

    @Test
    @DisplayName("Draft рассчитывается для сессии в статусе RECALCULATING")
    fun shouldCalculateDraftWhenSessionRecalculating() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = ShiftSessionEntityFactory.create(
            companyId = company.id!!,
            status = ShiftSession.Status.RECALCULATING
        )
        val savedSession = shiftSessionRepository.save(session).block()!!

        val response = client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", savedSession.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultDraft::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(savedSession.id, response.sessionId)

        val updatedSession = shiftSessionRepository.findById(savedSession.id!!).block()!!
        assertEquals(ShiftSession.Status.RECALCULATING_DRAFT, updatedSession.status)
    }

    @Test
    @DisplayName("Существующий draft возвращается без повторного расчёта")
    fun shouldReturnExistingDraftWhenOpenedDraft() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = session.id!!
            )
        ).block()!!

        val response = client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", session.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultDraft::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(draft.id, response.id)
        assertEquals(session.id, response.sessionId)
    }

    @Test
    @DisplayName("Ошибка 404 при запросе draft для несуществующей сессии")
    fun getDraft_shouldReturn404WhenSessionNotFound() {
        client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 при попытке получить draft для закрытой сессии")
    fun shouldReturn409WhenSessionClosed() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", session.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 если пользователь не привязан к компании сессии")
    fun getDraft_shouldReturn409WhenUserNotBoundToCompany() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        client.withUser()
            .get()
            .uri("/api/v1/calculation/draft/for-session/{id}", session.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Удаление draft, который не существует, возвращает 204")
    fun shouldReturn204WhenDraftNotExists() {
        client.withUser()
            .delete()
            .uri("/api/v1/calculation/draft/{id}/delete", UUID.randomUUID())
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    @DisplayName("Draft удаляется и статус сессии возвращается в OPENED")
    fun shouldDeleteDraftAndRollbackSessionStatus() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = session.id!!
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/calculation/draft/{id}/delete", draft.id)
            .exchange()
            .expectStatus().isNoContent

        val draftInDb = shiftResultDraftRepository.findById(draft.id!!).block()
        assertNull(draftInDb)

        val updatedSession = shiftSessionRepository.findById(session.id!!).block()!!
        assertEquals(ShiftSession.Status.OPENED, updatedSession.status)
    }

    @Test
    @DisplayName("Draft удаляется и статус сессии возвращается в RECALCULATING")
    fun shouldDeleteDraftAndRollbackRecalculatingStatus() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.RECALCULATING_DRAFT
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = session.id!!
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/calculation/draft/{id}/delete", draft.id)
            .exchange()
            .expectStatus().isNoContent

        val updatedSession = shiftSessionRepository.findById(session.id!!).block()!!
        assertEquals(ShiftSession.Status.RECALCULATING, updatedSession.status)
    }

    @Test
    @DisplayName("При удалении draft для закрытой сессии возвращает 204")
    fun deleteDraft_shouldReturn204WhenSessionClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = session.id!!
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/calculation/draft/{id}/delete", draft.id)
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    @DisplayName("Ошибка 409 при удалении draft пользователем без доступа к компании")
    fun deleteDraft_shouldReturn409WhenUserNotBoundToCompany() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = session.id!!
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/calculation/draft/{id}/delete", draft.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Подтверждение draft в статусе OPENED_DRAFT создаёт ShiftResult и Payments, отправляет уведомление")
    fun shouldConfirmDraftAndSendNotificationWhenOpenedDraft() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val draft = saveDraft(session.id!!)

        draft.addAndSavePayment(employee.id!!)

        // Подготовка stub для уведомления
        setupStubTgBotAPI(company.id!!)

        val response = client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", draft.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ConfirmDraftResponse::class.java)
            .returnResult()
            .responseBody!!

        // Проверка результата
        val shiftResult = shiftResultRepository.findById(response.resultId).block()
        assertNotNull(shiftResult)
        assertEquals(company.id, shiftResult?.companyId)
        assertEquals(session.id, shiftResult?.sessionId)

        val payments = paymentRepository.findAllByShiftResultId(response.resultId).collectList().block()
        assertNotNull(payments)
        assertEquals(1, payments?.size)
        assertEquals(employee.id, payments?.first()?.employeeId)

        // Проверка удаления draft
        val draftInDb = shiftResultDraftRepository.findById(draft.id!!).block()
        assertNull(draftInDb)

        // Проверка уведомления
        awaitVerifyRequestedStubTgBot(company.id!!)
    }

    private fun saveDraft(sessionId: UUID): ShiftResultDraftDbEntity {
        return shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(
                sessionId = sessionId
            )
        ).block()!!
    }

    private fun ShiftResultDraftDbEntity.addAndSavePayment(employeeId: UUID): PaymentDraftDbEntity {
        return paymentDraftRepository.save(PaymentDraftEntityFactory.create(
            shiftResultDraftId = this.id!!,
            employeeId = employeeId
        )).block()!!
    }

    @Test
    @DisplayName("Подтверждение draft в статусе RECALCULATING_DRAFT создаёт ShiftResult и Payments")
    fun shouldConfirmDraftWhenRecalculatingDraft() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.RECALCULATING_DRAFT
            )
        ).block()!!

        val draft = saveDraft(session.id!!)

        draft.addAndSavePayment(employee.id!!)

        setupStubTgBotAPI(company.id!!)

        val response = client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", draft.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ConfirmDraftResponse::class.java)
            .returnResult()
            .responseBody!!

        val shiftResult = shiftResultRepository.findById(response.resultId).block()
        assertNotNull(shiftResult)
        val payments = paymentRepository.findAllByShiftResultId(response.resultId).collectList().block()
        assertEquals(1, payments?.size)

        // Draft удалён
        assertNull(shiftResultDraftRepository.findById(draft.id!!).block())

        // Уведомление отправлено
        awaitVerifyRequestedStubTgBot(company.id!!)
    }

    @Test
    @DisplayName("Ошибка 404 при подтверждении несуществующего draft")
    fun shouldReturn404WhenDraftNotFound() {
        client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 при подтверждении draft, если сессия не в статусе draft")
    fun shouldReturn409WhenSessionNotDraft() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(sessionId = session.id!!)
        ).block()!!

        client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", draft.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 при подтверждении draft для закрытой сессии")
    fun confirmDraft_shouldReturn409WhenSessionClosed() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(sessionId = session.id!!)
        ).block()!!

        client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", draft.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 при подтверждении draft пользователем без доступа к компании")
    fun confirmDraft_shouldReturn409WhenUserNotBoundToCompany() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val draft = shiftResultDraftRepository.save(
            ShiftResultDraftEntityFactory.create(sessionId = session.id!!)
        ).block()!!

        client.withUser()
            .post()
            .uri("/api/v1/calculation/draft/{id}/confirm", draft.id)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }


}
