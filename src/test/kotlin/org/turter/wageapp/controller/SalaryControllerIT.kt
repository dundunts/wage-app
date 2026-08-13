package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ProblemDetail
import org.turter.wageapp.application.data.company.CompanyDbEntity
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.application.data.shift.*
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.transport.model.Payroll
import org.turter.wageapp.transport.model.PayrollAggregation
import org.turter.wageapp.utils.period.PeriodRequestParamsSupplier
import org.turter.wageapp.utils.result.PaymentEntityFactory
import org.turter.wageapp.utils.result.ShiftResultEntityFactory
import java.time.LocalDate
import java.util.*

@DisplayName("SalaryController — интеграционные тесты")
class SalaryControllerIT : CommonWageAppIT() {

    @Autowired
    private lateinit var shiftResultRepository: ShiftResultRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var shiftResultRepositoryDecorator: ShiftResultRepositoryDecorator

    @BeforeEach
    fun setup() {
        paymentRepository.deleteAll().block()
        shiftResultRepository.deleteAll().block()
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
    }

    // --- Вспомогательные методы ---
    private fun saveEmployeeAndCompany(): Pair<EmployeeDbEntity, CompanyDbEntity> {
        return saveNewUserEmployeeAndCompany()
    }

    private fun saveShiftResult(companyId: UUID, date: LocalDate): ShiftResultDbEntity {
        val shiftResult = ShiftResultEntityFactory.create(companyId, date)
        return shiftResultRepository.save(shiftResult).block()!!
    }

    private fun savePayment(shiftResultId: UUID, employeeId: UUID): PaymentDbEntity {
        val payment = PaymentEntityFactory.create(shiftResultId, employeeId)
        return paymentRepository.save(payment).block()!!
    }

    // --- Позитивные тесты ---

    @Test
    @DisplayName("getOwnSalary — CURRENT период возвращает корректный Payroll")
    fun getOwnSalary_current_returnsCorrectPayroll() {
        val (employee, company) = saveEmployeeAndCompany()
        val date = LocalDate.now()
        val shiftResult = saveShiftResult(company.id!!, date)
        savePayment(shiftResult.id!!, employee.id!!)

        val params = PeriodRequestParamsSupplier.current(company.id!!, date)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(1, response.elements.size)
        assertEquals(date, response.elements.first().date)
        val payment = response.elements.first().payments.first()
        assertEquals(employee.id, payment.employee.id)
        assertEquals(10, payment.percentFromRevenue)
        assertEquals(100, payment.tips)

        val summary = response.summaries.first()
        assertEquals(employee.id, summary.employee.id)
        assertEquals(10, summary.totalPercentFromRevenue)
        assertEquals(100, summary.totalTips)
    }

    @Test
    @DisplayName("getOwnSalary — PREVIOUS период возвращает корректный Payroll")
    fun getOwnSalary_previous_returnsCorrectPayroll() {
        val (employee, company) = saveEmployeeAndCompany()

        val currentDate = LocalDate.now()
        val period = Period.previous(currentDate)

        val date = period.end.minusDays(4)
        val shiftResult = saveShiftResult(company.id!!, date)
        savePayment(shiftResult.id!!, employee.id!!)

        val params = PeriodRequestParamsSupplier.previous(company.id!!, currentDate)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(1, response.elements.size)
        assertEquals(date, response.elements.first().date)
    }

    @Test
    @DisplayName("getOwnSalary — CUSTOM период возвращает корректный Payroll")
    fun getOwnSalary_custom_returnsCorrectPayroll() {
        val (employee, company) = saveEmployeeAndCompany()
        val start = LocalDate.now().minusDays(5)
        val end = LocalDate.now()
        (0..5).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            val shiftResult = saveShiftResult(company.id!!, date)
            savePayment(shiftResult.id!!, employee.id!!)
        }

        val params = PeriodRequestParamsSupplier.custom(company.id!!, start, end)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(6, response.elements.size)
        assertEquals(start, response.elements.first().date)
        assertEquals(end, response.elements.last().date)
    }

    // --- Вспомогательная функция для преобразования Map в query string ---
    private fun Map<String, String>.toQueryString(): String =
        entries.joinToString("&") { "${it.key}=${it.value}" }

    @Test
    @DisplayName("getOwnSalary — невалидный период выбрасывает IllegalArgumentException с 400")
    fun getOwnSalary_invalidPeriod_throwsBadRequest() {
        val (employee, company) = saveEmployeeAndCompany()
        val params = mapOf(
            "companyId" to company.id.toString(),
            "periodType" to "CUSTOM",
            "start" to LocalDate.now().toString(),
            "end" to LocalDate.now().minusDays(1).toString() // start > end
        )

        client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.detail").exists()
    }

    @Test
    @DisplayName("getOwnSalary — компания не найдена выбрасывает EntityNotFoundException с 404")
    fun getOwnSalary_companyNotFound_throwsNotFound() {
        val invalidCompanyId = UUID.randomUUID()
        val params = PeriodRequestParamsSupplier.current(invalidCompanyId, LocalDate.now())

        client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.detail").exists()
    }

    @Test
    @DisplayName("getOwnSalary — пользователь не привязан к компании выбрасывает ConflictDataException с 409")
    fun getOwnSalary_userNotLinkedToCompany_throwsConflict() {
        // Создаем компанию, но не привязываем к пользователю
        val company = saveNewCompany()
        val params = PeriodRequestParamsSupplier.current(company.id!!, LocalDate.now())

        client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.status").isEqualTo(409)
            .jsonPath("$.detail").exists()
    }

    @Test
    @DisplayName("getOwnSalary — отсутствуют shiftResults возвращает пустой Payroll")
    fun getOwnSalary_noShiftResults_returnsEmptyPayroll() {
        val (employee, company) = saveEmployeeAndCompany()
        val params = PeriodRequestParamsSupplier.current(company.id!!, LocalDate.now())

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertTrue(response.elements.isEmpty())
        assertTrue(response.summaries.isEmpty())
    }

    private fun saveEmployeeAndCompany(count: Int = 1): Pair<List<EmployeeDbEntity>, CompanyDbEntity> {
        val (employee, company) = saveEmployeeAndCompany()
        val employees = (1 until count).map { i ->
            val emp = company.addEmployee(firstName = "Employee$i")
            emp
        }.toMutableList()

        employees.addFirst(employee)

        return employees to company
    }

    @Test
    @DisplayName("getStaffSalary — период CURRENT возвращает Payroll по сотрудникам")
    fun getStaffSalary_currentPeriod_returnsPayroll() {
        val (employees, company) = saveEmployeeAndCompany(2)
        val today = LocalDate.of(2026, 1, 17)

        // Создаем ShiftResults и Payments для сотрудников
        val shiftResult = shiftResultRepository.save(
            ShiftResultEntityFactory.create(company.id!!, today)
        ).block()!!

        employees.forEach { employee ->
            paymentRepository.save(
                PaymentEntityFactory.create(shiftResult.id!!, employee.id!!)
            ).block()
        }

        val params = PeriodRequestParamsSupplier.current(company.id!!, today)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(1, response.elements.size)
        assertEquals(2, response.summaries.size)
        assertTrue(response.elements.first().payments.all { it.percentFromRevenue == 10 && it.tips == 100 })
    }

    @Test
    @DisplayName("getStaffSalary — период PREVIOUS возвращает Payroll по сотрудникам")
    fun getStaffSalary_previousPeriod_returnsPayroll() {
        val (employees, company) = saveEmployeeAndCompany(2)
        val date = LocalDate.of(2026, 1, 17)
        val prevPeriod = Period.previous(date)

        // создаем ShiftResults в прошлом периоде
        val shiftResult = shiftResultRepository.save(
            ShiftResultEntityFactory.create(company.id!!, prevPeriod.start)
        ).block()!!

        employees.forEach { employee ->
            paymentRepository.save(
                PaymentEntityFactory.create(shiftResult.id!!, employee.id!!)
            ).block()
        }

        val params = PeriodRequestParamsSupplier.previous(company.id!!, date)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(1, response.elements.size)
        assertEquals(2, response.summaries.size)
    }

    @Test
    @DisplayName("getStaffSalary — CUSTOM период возвращает агрегированный Payroll")
    fun getStaffSalary_customPeriod_returnsPayroll() {
        val (employees, company) = saveEmployeeAndCompany(3)
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 31)

        // создаем ShiftResults каждый день для всех сотрудников
        for (date in start.datesUntil(end.plusDays(1))) {
            val shiftResult = shiftResultRepository.save(
                ShiftResultEntityFactory.create(company.id!!, date)
            ).block()!!

            employees.forEach { employee ->
                paymentRepository.save(
                    PaymentEntityFactory.create(shiftResult.id!!, employee.id!!)
                ).block()
            }
        }

        val params = PeriodRequestParamsSupplier.custom(company.id!!, start, end)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        // проверяем агрегирование элементов и суммарные данные
        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(31, response.elements.size)
        assertEquals(3, response.summaries.size)
        response.elements.forEach { element ->
            assertEquals(3, element.payments.size)
        }
    }

    @Test
    @DisplayName("getStaffSalary — неверный период возвращает 400 IllegalArgumentException")
    fun getStaffSalary_invalidPeriod_throwsBadRequest() {
        val company = saveNewCompany()
        val params = mapOf(
            "companyId" to company.id.toString(),
            "periodType" to "CUSTOM",
            "start" to "2026-01-10",
            "end" to "2026-01-01" // конец раньше начала
        )

        client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
    }

    @Test
    @DisplayName("getStaffSalary — компания не найдена возвращает 404 EntityNotFoundException")
    fun getStaffSalary_companyNotFound_throwsNotFound() {
        val nonExistentCompanyId = UUID.randomUUID()
        val params = PeriodRequestParamsSupplier.current(nonExistentCompanyId, LocalDate.now())

        client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
    }

    @Test
    @DisplayName("getStaffSalary — пользователь не привязан к компании возвращает 409")
    fun getStaffSalary_userNotLinked_throwsConflict() {
        val company = saveNewCompany()
        val params = PeriodRequestParamsSupplier.current(company.id!!, LocalDate.now())

        client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("getOwnSalary — период CUSTOM с некорректной датой возвращает 400")
    fun getOwnSalary_customInvalidPeriod_throwsBadRequest() {
        val company = saveNewCompany()
        val params = mapOf(
            "companyId" to company.id.toString(),
            "periodType" to "CUSTOM",
            "start" to "2026-01-10",
            "end" to "2026-01-05" // конец раньше начала
        )

        client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
    }

    @Test
    @DisplayName("getOwnSalary — пользователь не привязан к компании возвращает 409")
    fun getOwnSalary_userNotLinked_throwsConflict() {
        val company = saveNewCompany()
        val params = PeriodRequestParamsSupplier.current(company.id!!, LocalDate.now())

        client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("getOwnSalary — сотрудник с несколькими компаниями получает данные только своей компании")
    fun getOwnSalary_multipleCompanies_returnsOnlySelectedCompany() {
        val (employee, companies) = saveNewUserEmployeeAndCompanies(companiesCount = 2)
        val targetCompany = companies.first()
        val today = LocalDate.now()

        val shiftResult = shiftResultRepository.save(
            ShiftResultEntityFactory.create(targetCompany.id!!, today)
        ).block()!!
        paymentRepository.save(
            PaymentEntityFactory.create(shiftResult.id!!, employee.id!!)
        ).block()

        val params = PeriodRequestParamsSupplier.current(targetCompany.id!!, today)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, response.elements.size)
        assertEquals(employee.id, response.elements.first().payments.first().employee.id)
    }

    @Test
    @DisplayName("getOwnSalary — агрегация BY_DAY для небольшого числа дней")
    fun getOwnSalary_aggregateByDay_returnsDailyPayroll() {
        val today = LocalDate.now()

        val (employee, company) = saveEmployeeAndCompany()

        val shiftResult1 = shiftResultRepository.save(
            ShiftResultEntityFactory.create(company.id!!, today)
        ).block()!!
        paymentRepository.save(PaymentEntityFactory.create(shiftResult1.id!!, employee.id!!)).block()

        val shiftResult2 = shiftResultRepository.save(
            ShiftResultEntityFactory.create(company.id!!, today.minusDays(1))
        ).block()!!
        paymentRepository.save(PaymentEntityFactory.create(shiftResult2.id!!, employee.id!!, percentFromRevenue = 20))
            .block()

        val params = PeriodRequestParamsSupplier.custom(company.id!!, today.minusDays(2), today)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_DAY, response.type)
        assertEquals(2, response.elements.size)
        assertTrue(response.elements.all { it.payments.any { p -> p.employee.id == employee.id } })
    }

    @Test
    @DisplayName("getOwnSalary — агрегация BY_MONTH для >31 дня")
    fun getOwnSalary_aggregateByMonth_returnsMonthlyPayroll() {
        val startDate = LocalDate.now().minusDays(40)
        val endDate = LocalDate.now()

        val (employee, company) = saveEmployeeAndCompany()

        for (i in 0..40) {
            val date = startDate.plusDays(i.toLong())
            val shiftResult = shiftResultRepository.save(ShiftResultEntityFactory.create(company.id!!, date)).block()!!
            paymentRepository.save(PaymentEntityFactory.create(shiftResult.id!!, employee.id!!, percentFromRevenue = i))
                .block()
        }

        val params = PeriodRequestParamsSupplier.custom(company.id!!, startDate, endDate)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_MONTH, response.type)
        assertTrue(response.elements.size <= 2) // два месяца данных
        assertTrue(response.elements.all { it.payments.any { p -> p.employee.id == employee.id } })
    }

    @Test
    @DisplayName("getOwnSalary — агрегация BY_YEAR для >3 лет")
    fun getOwnSalary_aggregateByYear_returnsYearlyPayroll() {
        val startDate = LocalDate.now().minusYears(4)
        val endDate = LocalDate.now()

        val (employee, company) = saveEmployeeAndCompany()

        for (i in 0..1500) { // >3 лет
            val date = startDate.plusDays(i.toLong())
            val shiftResult = shiftResultRepository.save(ShiftResultEntityFactory.create(company.id!!, date)).block()!!
            paymentRepository.save(PaymentEntityFactory.create(shiftResult.id!!, employee.id!!, percentFromRevenue = 1))
                .block()
        }

        val params = PeriodRequestParamsSupplier.custom(company.id!!, startDate, endDate)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/own/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(PayrollAggregation.BY_YEAR, response.type)
        assertTrue(response.elements.size <= 5) // не больше количества лет
        assertTrue(response.elements.all { it.payments.any { p -> p.employee.id == employee.id } })
    }

    @Test
    @DisplayName("getStaffSalary — агрегация учитывает всех сотрудников")
    fun getStaffSalary_aggregateMultipleEmployees_returnsAll() {
        val (employee, company) = saveEmployeeAndCompany()
        val otherEmployee = company.addEmployee("other_employee")

        saveNewBind(otherEmployee.id!!, company.id!!)

        val today = LocalDate.now()
        val shiftResult = shiftResultRepository.save(ShiftResultEntityFactory.create(company.id!!, today)).block()!!
        paymentRepository.save(PaymentEntityFactory.create(shiftResult.id!!, employee.id!!, percentFromRevenue = 10))
            .block()
        paymentRepository.save(
            PaymentEntityFactory.create(
                shiftResult.id!!,
                otherEmployee.id!!,
                percentFromRevenue = 20
            )
        ).block()

        val params = PeriodRequestParamsSupplier.current(company.id!!, today)

        val response = client.withUser()
            .get()
            .uri("/api/v1/salary/staff/get?${params.toQueryString()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(Payroll::class.java)
            .returnResult()
            .responseBody!!

        val allEmployeeIds = response.elements.flatMap { it.payments }.map { it.employee.id }
        assertTrue(allEmployeeIds.contains(employee.id))
        assertTrue(allEmployeeIds.contains(otherEmployee.id))
    }
}
