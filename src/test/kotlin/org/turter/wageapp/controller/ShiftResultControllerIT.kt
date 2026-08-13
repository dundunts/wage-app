package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.turter.wageapp.application.data.shift.*
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.salary.PeriodType
import org.turter.wageapp.domain.shift.*
import org.turter.wageapp.transport.model.CalculationSource as TransportCalculationSource
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultResponse
import org.turter.wageapp.transport.model.ShiftResultPage
import org.turter.wageapp.transport.model.ShiftResultResponse
import org.turter.wageapp.utils.result.PaymentEntityFactory
import org.turter.wageapp.utils.result.PaymentPayloadDtoSupplier
import org.turter.wageapp.utils.result.SaveShiftResultPayloadDtoSupplier
import org.turter.wageapp.utils.result.ShiftResultEntityFactory
import org.turter.wageapp.utils.session.ShiftSessionEntityFactory
import java.time.LocalDate
import java.util.*

@DisplayName("ShiftResultController — интеграционные тесты")
class ShiftResultControllerIT : CommonWageAppIT() {

    @Autowired
    private lateinit var shiftResultRepository: ShiftResultRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var shiftSessionRepository: ShiftSessionRepository

    @Autowired
    private lateinit var checkpointRepository: CheckpointRepository

    @BeforeEach
    fun setup() {
        paymentRepository.deleteAll().block()
        shiftResultRepository.deleteAll().block()
        checkpointRepository.deleteAll().block()
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
    }

    @Test
    @DisplayName("Получение детализированного результата смены без привязанной сессии")
    fun getResult_returnsDetailedResultWithoutSession() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        val shiftResult = saveShiftResult(
            companyId = company.id!!,
            date = date,
            sessionId = null
        )

        savePayment(
            shiftResultId = shiftResult.id!!,
            employeeId = employee.id!!
        )

        val response = client
            .withUser()
            .get()
            .uri("/api/v1/shift-result/{id}/get/detailed", shiftResult.id)
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultResponse::class.java)
            .returnResult()
            .responseBody!!

        val result = response.shiftResult

        assertEquals(shiftResult.id, result.id)
        assertEquals(date, result.date)
        assertEquals(TransportCalculationSource.MANUAL_OVERRIDE, result.calculationSource)
        assertEquals(1, result.payments.size)
        assertEquals(employee.id, result.payments.first().employee.id)
        assertEquals(null, response.session)
    }

    @Test
    @DisplayName("Получение детализированного результата смены с привязанной сессией")
    fun getResult_returnsDetailedResultWithSession() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 20)

        val session = shiftSessionRepository.save(
            ShiftSessionEntityFactory.create(companyId = company.id!!, status = ShiftSession.Status.CLOSED)
        ).block()!!

        val sessionId = session.id!!
        checkpointRepository.save(
            org.turter.wageapp.utils.checkpoint.CheckpointEntityFactory.create(
                shiftSessionId = sessionId,
                type = CheckpointType.REGULAR,
            ),
        ).block()!!

        val shiftResult = saveShiftResult(
            companyId = company.id!!,
            date = date,
            sessionId = sessionId
        )

        savePayment(
            shiftResultId = shiftResult.id!!,
            employeeId = employee.id!!
        )

        client
            .withUser()
            .get()
            .uri("/api/v1/shift-result/{id}/get/detailed", shiftResult.id)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.shiftResult.id").isEqualTo(shiftResult.id.toString())
            .jsonPath("$.session.id").isEqualTo(sessionId.toString())
            .jsonPath("$.session.checkpoints[0].type").isEqualTo("REGULAR")
    }

    @Test
    @DisplayName("Ошибка 404 при попытке получить несуществующий результат смены")
    fun getResult_returns404_whenResultNotFound() {
        val unknownId = UUID.randomUUID()

        client
            .withUser()
            .get()
            .uri("/api/v1/shift-result/{id}/get/detailed", unknownId)
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Получение результатов смены за произвольный период (CUSTOM)")
    fun getResultsByPeriod_CUSTOM_returnsResultsInRange() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val inRangeDate1 = LocalDate.of(2025, 1, 5)
        val inRangeDate2 = LocalDate.of(2025, 1, 10)
        val outOfRangeDate = LocalDate.of(2025, 1, 20)

        saveShiftResult(company.id!!, inRangeDate1)
        saveShiftResult(company.id!!, inRangeDate2)
        saveShiftResult(company.id!!, outOfRangeDate)

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CUSTOM)
                    .queryParam("start", "2025-01-01")
                    .queryParam("end", "2025-01-15")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(2, response.content.size)
        assertTrue(response.content.all { it.date <= LocalDate.of(2025, 1, 15) })
    }

    @Test
    @DisplayName("Ошибка 400 при CUSTOM периоде без параметра start")
    fun getResultsByPeriod_CUSTOM_returns400_whenStartMissing() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CUSTOM)
                    .queryParam("end", "2025-01-15")
                    .build()
            }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 400 при CUSTOM периоде, если end раньше start")
    fun getResultsByPeriod_CUSTOM_returns400_whenEndBeforeStart() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CUSTOM)
                    .queryParam("start", "2025-01-20")
                    .queryParam("end", "2025-01-10")
                    .build()
            }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("CURRENT период — первая половина месяца (1–15)")
    fun getResultsByPeriod_CURRENT_firstHalfOfMonth() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 5))
        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 16))

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CURRENT)
                    .queryParam("now", "2025-01-10")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, response.content.size)
        assertEquals(LocalDate.of(2025, 1, 5), response.content.first().date)
    }

    @Test
    @DisplayName("CURRENT период — вторая половина месяца (16–конец)")
    fun getResultsByPeriod_CURRENT_secondHalfOfMonth() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 10))
        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 20))

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CURRENT)
                    .queryParam("now", "2025-01-20")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, response.content.size)
        assertEquals(LocalDate.of(2025, 1, 20), response.content.first().date)
    }

    @Test
    @DisplayName("Ошибка 400 при CURRENT периоде без параметра now")
    fun getResultsByPeriod_CURRENT_returns400_whenNowMissing() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CURRENT)
                    .build()
            }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Malformed pagination uses default page and size")
    fun getResultsByPeriodPage_usesDefaultsForMalformedPagination() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.CUSTOM)
                    .queryParam("start", "2025-01-01")
                    .queryParam("end", "2025-01-31")
                    .queryParam("page", "malformed")
                    .queryParam("size", "malformed")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(0, response.number)
        assertEquals(100, response.propertySize)
    }

    @Test
    @DisplayName("PREVIOUS период — now в первой половине месяца (16–конец прошлого месяца)")
    fun getResultsByPeriod_PREVIOUS_whenNowInFirstHalf() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        saveShiftResult(company.id!!, LocalDate.of(2024, 12, 20))
        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 5))

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.PREVIOUS)
                    .queryParam("now", "2025-01-10")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, response.content.size)
        assertEquals(LocalDate.of(2024, 12, 20), response.content.first().date)
    }

    @Test
    @DisplayName("PREVIOUS период — now во второй половине месяца (1–15 текущего месяца)")
    fun getResultsByPeriod_PREVIOUS_whenNowInSecondHalf() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 5))
        saveShiftResult(company.id!!, LocalDate.of(2025, 1, 20))

        val response = client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.PREVIOUS)
                    .queryParam("now", "2025-01-20")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody(ShiftResultPage::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, response.content.size)
        assertEquals(LocalDate.of(2025, 1, 5), response.content.first().date)
    }

    @Test
    @DisplayName("Ошибка 400 при PREVIOUS периоде без параметра now")
    fun getResultsByPeriod_PREVIOUS_returns400_whenNowMissing() {
        val (_, company) = saveNewUserEmployeeAndCompany()

        client
            .withUser()
            .get()
            .uri {
                it.path("/api/v1/shift-result/get/detailed/by-period/page")
                    .queryParam("companyId", company.id)
                    .queryParam("periodType", PeriodType.PREVIOUS)
                    .build()
            }
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Создание нового результата смены с MANUAL_OVERRIDE")
    fun saveResult_createsNewResultWithManualOverride() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        val payload = SaveShiftResultPayloadDtoSupplier.default(
            companyId = company.id!!,
            date = date,
            payments = listOf(
                PaymentPayloadDtoSupplier.default(employee.id!!)
            )
        )

        val response = client
            .withUser()
            .post()
            .uri("/api/v1/shift-result/save")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(SaveManualOverrideShiftResultResponse::class.java)
            .returnResult()
            .responseBody!!

        val saved = shiftResultRepository.findById(response.resultId).block()!!

        assertEquals(CalculationSource.MANUAL_OVERRIDE, saved.calculationSource)
        assertEquals(company.id, saved.companyId)
        assertEquals(date, saved.date)
    }

    @Test
    @DisplayName("Перезапись существующего результата смены при overwrite = true")
    fun saveResult_overwritesExistingResult_whenOverwriteTrue() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        val existing = saveShiftResult(company.id!!, date)
        savePayment(existing.id!!, employee.id!!)

        val payload = SaveShiftResultPayloadDtoSupplier.default(
            companyId = company.id!!,
            date = date,
            overwrite = true,
            payments = listOf(
                PaymentPayloadDtoSupplier.default(employee.id!!, tips = 500)
            )
        )

        val newResult = client
            .withUser()
            .post()
            .uri("/api/v1/shift-result/save")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(SaveManualOverrideShiftResultResponse::class.java)
            .returnResult()
            .responseBody!!

        val payments = paymentRepository.findAllByShiftResultId(newResult.resultId).collectList().block()!!

        assertEquals(1, payments.size)
        assertEquals(500, payments.first().tips)
    }

    @Test
    @DisplayName("Перезапись существующего результата смены при overwrite = true с изменением даты")
    fun saveResult_overwritesExistingResultWithDifferentDate_whenOverwriteTrue() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val targetDate = LocalDate.of(2025, 1, 10)
        val oldDate = LocalDate.of(2025, 1, 9)

        val forOverwrite = saveShiftResult(company.id!!, targetDate)
        val existing = saveShiftResult(company.id!!, oldDate)

        savePayment(forOverwrite.id!!, employee.id!!)
        savePayment(existing.id!!, employee.id!!)

        val payload = SaveShiftResultPayloadDtoSupplier.default(
            replacementId = existing.id,
            companyId = company.id!!,
            date = targetDate,
            overwrite = true,
            payments = listOf(
                PaymentPayloadDtoSupplier.default(employee.id!!, tips = 500)
            )
        )

        val newResult = client
            .withUser()
            .post()
            .uri("/api/v1/shift-result/save")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody(SaveManualOverrideShiftResultResponse::class.java)
            .returnResult()
            .responseBody!!

        val savedResult = shiftResultRepository.findById(newResult.resultId).block()

        assertNotNull(savedResult)

        val oldResult = shiftResultRepository.findById(existing.id!!).block()
        val replacedResult = shiftResultRepository.findById(forOverwrite.id!!).block()

        assertNull(oldResult)
        assertNull(replacedResult)

        val payments = paymentRepository.findAllByShiftResultId(newResult.resultId).collectList().block()!!

        assertEquals(1, payments.size)
        assertEquals(500, payments.first().tips)
    }

    @Test
    @DisplayName("Ошибка 409 при сохранении результата, если он уже существует и overwrite = false")
    fun saveResult_returns409_whenResultExistsAndOverwriteFalse() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        saveShiftResult(company.id!!, date)

        val payload = SaveShiftResultPayloadDtoSupplier.default(
            companyId = company.id!!,
            date = date,
            payments = listOf(
                PaymentPayloadDtoSupplier.default(employee.id!!)
            )
        )

        client
            .withUser()
            .post()
            .uri("/api/v1/shift-result/save")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Ошибка 409 при сохранении результата, если в указаны несколько выплат для одного работника")
    fun saveResult_returns409_whenPaymentsHasDuplicatesEmployees() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        val payload = SaveShiftResultPayloadDtoSupplier.default(
            companyId = company.id!!,
            date = date,
            payments = listOf(
                PaymentPayloadDtoSupplier.default(employee.id!!),
                PaymentPayloadDtoSupplier.default(employee.id!!)
            )
        )

        client
            .withUser()
            .post()
            .uri("/api/v1/shift-result/save")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("Удаление результата смены вместе с платежами")
    fun deleteResult_deletesResultAndPayments() {
        val (employee, company) = saveNewUserEmployeeAndCompany()
        val date = LocalDate.of(2025, 1, 10)

        val result = saveShiftResult(company.id!!, date)
        savePayment(result.id!!, employee.id!!)

        client
            .withUser()
            .delete()
            .uri("/api/v1/shift-result/{id}/delete", result.id)
            .exchange()
            .expectStatus().isNoContent

        assertNull(shiftResultRepository.findById(result.id!!).block())
        assertTrue(paymentRepository.findAllByShiftResultId(result.id!!).collectList().block()!!.isEmpty())
    }




    private fun saveShiftResult(
        companyId: UUID,
        date: LocalDate,
        calculationSource: CalculationSource = CalculationSource.MANUAL_OVERRIDE,
        sessionId: UUID? = null
    ): ShiftResultDbEntity {
        val entity = ShiftResultEntityFactory.create(
            companyId = companyId,
            date = date,
            calculationSource = calculationSource,
            sessionId = sessionId
        )
        return shiftResultRepository.save(entity).block()!!
    }

    private fun savePayment(
        shiftResultId: UUID,
        employeeId: UUID
    ): PaymentDbEntity {
        val payment = PaymentEntityFactory.create(
            shiftResultId = shiftResultId,
            employeeId = employeeId
        )
        return paymentRepository.save(payment).block()!!
    }

}
