package org.turter.wageapp.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeCompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.USER_ID
import org.turter.wageapp.config.withUser
import org.turter.wageapp.utils.CompanyEntityFactory
import org.turter.wageapp.utils.CompanyPayloadDtoSupplier
import org.turter.wageapp.utils.EmployeeCompanyEntityFactory
import org.turter.wageapp.utils.EmployeeEntityFactory
import java.util.*

class CompanyControllerIT() : CommonWageAppIT() {

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var employeeCompanyRepository: EmployeeCompanyRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @BeforeEach
    fun setup() {
        companyRepository.deleteAll().block()
    }

    @Test
    @DisplayName("GET /company/get/{id} — успешно возвращает компанию по id")
    fun shouldReturnCompanyById() {
        val entity = companyRepository.save(CompanyEntityFactory.create(
            title = "My company",
            employeeWageCoefficientFromRevenue = 15,
            defaultShiftStartTime = "10:00"
        )).block()

        val companyId = entity?.id!!

        client.withUser()
            .get()
            .uri("/api/v1/company/get/{id}", companyId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo(companyId.toString())
            .jsonPath("$.title").isEqualTo("My company")
            .jsonPath("$.employeeWageCoefficientFromRevenue").isEqualTo(15)
            .jsonPath("$.defaultShiftStartTime").isEqualTo("10:00")
    }

    @Test
    @DisplayName("GET /company/get/{id} — возвращает 404, если компания не найдена")
    fun shouldReturn404WhenCompanyNotFound() {
        val companyId = UUID.randomUUID()

        client.withUser()
            .get()
            .uri("/api/v1/company/get/{id}", companyId)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
    }

    @Test
    @DisplayName("GET /company/get/for-user — возвращает список компаний пользователя")
    fun shouldReturnUserCompanies() {
        val company1 = companyRepository.save(CompanyEntityFactory.create(title = "Company 1")).block()!!
        val company2 = companyRepository.save(CompanyEntityFactory.create(title = "Company 2")).block()!!

        val employee = employeeRepository.save(EmployeeEntityFactory.create(userId = USER_ID)).block()!!

        val bind1 = EmployeeCompanyEntityFactory.create(employee.id!!, company1.id!!)
        val bind2 = EmployeeCompanyEntityFactory.create(employee.id!!, company2.id!!)

        employeeCompanyRepository.saveAll(listOf(bind1, bind2)).blockLast()

        client.withUser()
            .get()
            .uri("/api/v1/company/get/for-user")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.companies.length()").isEqualTo(2)
            .jsonPath("$.companies[0].title").isEqualTo("Company 1")
            .jsonPath("$.companies[1].title").isEqualTo("Company 2")
    }

    @Test
    @DisplayName("GET /company/get/for-user — возвращает пустой список, если компаний нет")
    fun shouldReturnEmptyCompaniesList() {
        client.withUser()
            .get()
            .uri("/api/v1/company/get/for-user")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.companies").isArray
            .jsonPath("$.companies.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("POST /company/create — успешно создаёт компанию")
    fun shouldCreateCompany() {
        val payload = CompanyPayloadDtoSupplier.valid(
            title = "Created company",
            employeeWageCoefficientFromRevenue = 20,
            defaultShiftStartTime = "08:30"
        )

        client.withUser()
            .post()
            .uri("/api/v1/company/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isNotEmpty
            .jsonPath("$.title").isEqualTo("Created company")
            .jsonPath("$.employeeWageCoefficientFromRevenue").isEqualTo(20)
            .jsonPath("$.defaultShiftStartTime").isEqualTo("08:30")

        val saved = companyRepository.findAll().collectList().block()!!
        assert(saved.size == 1)
        assert(saved.first().title == "Created company")
    }

    @Test
    @DisplayName("POST /company/create — возвращает 409 при создании компании с существующим title")
    fun shouldReturn409WhenTitleAlreadyExists() {
        companyRepository.save(CompanyEntityFactory.create(title = "Duplicate title")).block()

        val payload = CompanyPayloadDtoSupplier.valid(title = "Duplicate title")

        client.withUser()
            .post()
            .uri("/api/v1/company/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.status").isEqualTo(409)
    }

    @Test
    @DisplayName("POST /company/create — возвращает 400 при пустом title")
    fun shouldReturn400WhenTitleIsEmpty() {
        val payload = CompanyPayloadDtoSupplier.withEmptyTitle()

        client.withUser()
            .post()
            .uri("/api/v1/company/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
    }

    @Test
    @DisplayName("POST /company/create — возвращает 400 при некорректном формате времени начала смены")
    fun shouldReturn400WhenShiftStartTimeIsInvalid() {
        val payload = CompanyPayloadDtoSupplier.withInvalidShiftStartTime()

        client.withUser()
            .post()
            .uri("/api/v1/company/create")
            .bodyValue(payload)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
    }

    @Test
    @DisplayName("PUT /company/update/{id} — успешно обновляет компанию")
    fun shouldUpdateCompany() {
        val existing = companyRepository.save(CompanyEntityFactory.create(
            title = "Old title",
            employeeWageCoefficientFromRevenue = 10,
            defaultShiftStartTime = "09:00"
        )).block()

        val companyId = existing?.id!!

        val payload = CompanyPayloadDtoSupplier.valid(
            title = "Updated title",
            employeeWageCoefficientFromRevenue = 20,
            defaultShiftStartTime = "10:30"
        )

        client.withUser()
            .put()
            .uri("/api/v1/company/update/{id}", companyId)
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNoContent

        val updated = companyRepository.findById(companyId).block()!!
        assert(updated.title == "Updated title")
        assert(updated.employeeWageCoefficientFromRevenue == 20)
        assert(updated.defaultShiftStartTime == "10:30")
    }

    @Test
    @DisplayName("PUT /company/update/{id} — возвращает 404, если компания не найдена")
    fun shouldReturn404WhenUpdatingNonExistingCompany() {
        val payload = CompanyPayloadDtoSupplier.valid()

        client.withUser()
            .put()
            .uri("/api/v1/company/update/{id}", UUID.randomUUID())
            .bodyValue(payload)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
    }

    @Test
    @DisplayName("PUT /company/update/{id} — возвращает 409 при обновлении на существующий title")
    fun shouldReturn409WhenUpdatingWithDuplicateTitle() {
        val first = CompanyEntityFactory.create(title = "Company A")
        val second = CompanyEntityFactory.create(title = "Company B")

        companyRepository.saveAll(listOf(first, second)).collectList().block()

        val payload = CompanyPayloadDtoSupplier.valid(title = "Company A")

        client.withUser()
            .put()
            .uri("/api/v1/company/update/{id}", second.id!!)
            .bodyValue(payload)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.status").isEqualTo(409)
    }

    @Test
    @DisplayName("PUT /company/update/{id} — возвращает 400 при невалидном payload")
    fun shouldReturn400WhenUpdatePayloadIsInvalid() {
        val company = companyRepository.save(CompanyEntityFactory.create()).block()

        val payload = CompanyPayloadDtoSupplier.withEmptyTitle()

        client.withUser()
            .put()
            .uri("/api/v1/company/update/{id}", company?.id!!)
            .bodyValue(payload)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
    }

    @Test
    @DisplayName("DELETE /company/delete/{id} — успешно удаляет компанию")
    fun shouldDeleteCompany() {
        val company = companyRepository.save(CompanyEntityFactory.create()).block()

        client.withUser()
            .delete()
            .uri("/api/v1/company/delete/{id}", company?.id!!)
            .exchange()
            .expectStatus().isNoContent

        val exists = companyRepository.findById(company.id!!).block()
        assert(exists == null)
    }

}