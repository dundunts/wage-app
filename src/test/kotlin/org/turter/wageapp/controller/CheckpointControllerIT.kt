package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ProblemDetail
import org.turter.wageapp.application.data.shift.CheckpointEmployeeRepository
import org.turter.wageapp.application.data.shift.CheckpointMetricRecordRepository
import org.turter.wageapp.application.data.shift.CheckpointRepository
import org.turter.wageapp.application.data.shift.ShiftSessionRepository
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.transport.model.RegularCheckpoint
import org.turter.wageapp.utils.checkpoint.CheckpointDtoSupplier
import org.turter.wageapp.utils.checkpoint.CheckpointEmployeeEntityFactory
import org.turter.wageapp.utils.checkpoint.CheckpointEntityFactory
import org.turter.wageapp.utils.payload.CreateRegularCheckpointPayloadSupplier
import org.turter.wageapp.utils.session.ShiftSessionEntityFactory
import java.util.*

class CheckpointControllerIT : CommonWageAppIT() {

    @Autowired
    lateinit var shiftSessionRepository: ShiftSessionRepository

    @Autowired
    lateinit var checkpointRepository: CheckpointRepository

    @Autowired
    lateinit var checkpointEmployeeRepository: CheckpointEmployeeRepository

    @Autowired
    lateinit var checkpointMetricRecordRepository: CheckpointMetricRecordRepository

    @BeforeEach
    fun setup() {
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()

        checkpointMetricRecordRepository.deleteAll().block()
        checkpointEmployeeRepository.deleteAll().block()
        checkpointRepository.deleteAll().block()
        shiftSessionRepository.deleteAll().block()
    }

    @Test
    @DisplayName("POST /checkpoint/create — успешно создаёт чекпоинт для OPENED сессии и отправляет уведомление")
    fun shouldCreateCheckpointForOpenedSessionAndSendNotification() {
        // given
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(employee.id!!)
        )


        // ожидаемое событие
        setupStubTgBotAPI(company.id!!)

        // when
        val response = client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(RegularCheckpoint::class.java)
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(response.id)
        assertEquals(payload.revenue, response.revenue)
        assertEquals(payload.tips, response.tips)
        assertEquals(1, response.employees.size)

        val stored = checkpointRepository.findById(response.id).block()
        assertNotNull(stored)

        awaitVerifyRequestedStubTgBot(company.id!!)
    }

    @Test
    @DisplayName("POST /checkpoint/create — возвращает 400 для некорректной даты и времени")
    fun shouldReturn400WhenCheckpointDateTimeIsInvalid() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(companyId = company.id!!, status = ShiftSession.Status.OPENED)
        ).block()!!
        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(employee.id!!),
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(
                mapOf(
                    "sessionId" to payload.sessionId,
                    "revenue" to payload.revenue,
                    "tips" to payload.tips,
                    "employeeIds" to payload.employeeIds,
                    "dateTime" to "not-a-date",
                    "type" to payload.type,
                    "fieldRecords" to payload.fieldRecords,
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/create — успешно создаёт чекпоинт для RECALCULATING сессии без отправки уведомления")
    fun shouldCreateCheckpointForRecalculatingSessionWithoutNotification() {
        // given
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.RECALCULATING
            )
        ).block()!!

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(employee.id!!)
        )

        // when
        val response = client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(RegularCheckpoint::class.java)
            .returnResult()
            .responseBody!!

        // then
        assertEquals(payload.revenue, response.revenue)
        assertEquals(payload.tips, response.tips)

        val stored = checkpointRepository.findById(response.id).block()
        assertNotNull(stored)
    }

    @Test
    @DisplayName("POST /checkpoint/create — ошибка при попытке создать чекпоинт для CLOSED сессии")
    fun shouldFailWhenSessionIsClosed() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(employee.id!!)
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/create — ошибка при создании чекпоинта для OPENED_DRAFT сессии")
    fun shouldFailWhenSessionIsOpenedDraft() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED_DRAFT
            )
        ).block()!!

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(employee.id!!)
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/create — ошибка 404 если сессия не найдена")
    fun shouldFailWhenSessionNotFound() {
        val (employee, _) = saveNewUserEmployeeAndCompany()

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = UUID.randomUUID(),
            employeeIds = setOf(employee.id!!)
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/create — ошибка если пользователь не имеет доступа к компании сессии")
    fun shouldFailWhenUserHasNoAccessToCompany() {
        val company = saveNewCompany()
        val otherEmployee = company.addEmployee()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val payload = CreateRegularCheckpointPayloadSupplier.valid(
            sessionId = session.id!!,
            employeeIds = setOf(otherEmployee.id!!)
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/update — успешно обновляет чекпоинт в OPENED сессии и отправляет уведомление")
    fun shouldUpdateCheckpointInOpenedSessionAndSendNotification() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        checkpointEmployeeRepository.save(
            CheckpointEmployeeEntityFactory.create(
                checkpointId = checkpoint.id!!,
                employeeId = employee.id!!
            )
        ).block()

        val payload = CheckpointDtoSupplier.update(
            id = checkpoint.id!!,
            employeeIds = setOf(employee.id!!),
            tips = 200,
            revenue = 2000
        )

        setupStubTgBotAPI(company.id!!)

        val response = client.withUser()
            .post()
            .uri("/api/v1/checkpoint/update")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isOk
            .expectBody(RegularCheckpoint::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(payload.revenue, response.revenue)
        assertEquals(payload.tips, response.tips)

        // проверка в БД
        val stored = checkpointRepository.findById(checkpoint.id!!).block()
        assertNotNull(stored)
        assertEquals(payload.revenue, stored!!.revenue)
        assertEquals(payload.tips, stored.tips)

        awaitVerifyRequestedStubTgBot(company.id!!)
    }

    @Test
    @DisplayName("POST /checkpoint/update — успешно обновляет чекпоинт в RECALCULATING сессии без уведомления")
    fun shouldUpdateCheckpointInRecalculatingSessionWithoutNotification() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.RECALCULATING
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        checkpointEmployeeRepository.save(
            CheckpointEmployeeEntityFactory.create(
                checkpointId = checkpoint.id!!,
                employeeId = employee.id!!
            )
        ).block()

        val payload = CheckpointDtoSupplier.update(
            id = checkpoint.id!!,
            employeeIds = setOf(employee.id!!),
            tips = 200,
            revenue = 2000
        )

        val response = client.withUser()
            .post()
            .uri("/api/v1/checkpoint/update")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isOk
            .expectBody(RegularCheckpoint::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(payload.revenue, response.revenue)
        assertEquals(payload.tips, response.tips)
    }

    @Test
    @DisplayName("POST /checkpoint/update — ошибка при попытке обновить чекпоинт в CLOSED сессии")
    fun shouldFailUpdateInClosedSession() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        val payload = CheckpointDtoSupplier.update(
            id = checkpoint.id!!,
            employeeIds = setOf(employee.id!!),
            tips = 200,
            revenue = 2000
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/update")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/update — ошибка 404 если чекпоинт не найден")
    fun shouldFailUpdateCheckpointNotFound() {
        val (_, _) = saveNewUserEmployeeAndCompany()

        val payload = CheckpointDtoSupplier.update(
            id = UUID.randomUUID(),
            employeeIds = emptySet(),
            tips = 200,
            revenue = 2000
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/update")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /checkpoint/update — ошибка если пользователь не имеет доступа к компании чекпоинта")
    fun shouldFailUpdateWhenUserHasNoAccess() {
        val company = saveNewCompany()

        val otherEmployee = company.addEmployee()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        val payload = CheckpointDtoSupplier.update(
            id = checkpoint.id!!,
            employeeIds = setOf(otherEmployee.id!!),
            tips = 200,
            revenue = 2000
        )

        client.withUser()
            .post()
            .uri("/api/v1/checkpoint/update")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("DELETE /checkpoint/{id}/delete — успешно удаляет чекпоинт в OPENED сессии и отправляет уведомление")
    fun shouldDeleteCheckpointInOpenedSessionAndSendNotification() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        checkpointEmployeeRepository.save(
            CheckpointEmployeeEntityFactory.create(
                checkpointId = checkpoint.id!!,
                employeeId = employee.id!!
            )
        ).block()

        setupStubTgBotAPI(company.id!!)

        client.withUser()
            .delete()
            .uri("/api/v1/checkpoint/${checkpoint.id}/delete")
            .exchange()
            .expectStatus().isNoContent

        // проверка удаления из БД
        assertNull(checkpointRepository.findById(checkpoint.id!!).block())

        awaitVerifyRequestedStubTgBot(company.id!!)
    }

    @Test
    @DisplayName("DELETE /checkpoint/{id}/delete — успешно удаляет чекпоинт в RECALCULATING сессии без уведомления")
    fun shouldDeleteCheckpointInRecalculatingSessionWithoutNotification() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.RECALCULATING
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        checkpointEmployeeRepository.save(
            CheckpointEmployeeEntityFactory.create(
                checkpointId = checkpoint.id!!,
                employeeId = employee.id!!
            )
        ).block()

        client.withUser()
            .delete()
            .uri("/api/v1/checkpoint/${checkpoint.id}/delete")
            .exchange()
            .expectStatus().isNoContent

        assertNull(checkpointRepository.findById(checkpoint.id!!).block())
    }

    @Test
    @DisplayName("DELETE /checkpoint/{id}/delete — ошибка при попытке удалить чекпоинт в CLOSED сессии")
    fun shouldFailDeleteInClosedSession() {
        val (employee, company) = saveNewUserEmployeeAndCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.CLOSED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/checkpoint/${checkpoint.id}/delete")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("DELETE /checkpoint/{id}/delete — ошибка если пользователь не имеет доступа к компании чекпоинта")
    fun shouldFailDeleteWhenUserHasNoAccess() {
        val company = saveNewCompany()

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(
                companyId = company.id!!,
                status = ShiftSession.Status.OPENED
            )
        ).block()!!

        val checkpoint = checkpointRepository.save(
            CheckpointEntityFactory.create(
                shiftSessionId = session.id!!,
                tips = 100,
                revenue = 1000
            )
        ).block()!!

        client.withUser()
            .delete()
            .uri("/api/v1/checkpoint/${checkpoint.id}/delete")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

}
