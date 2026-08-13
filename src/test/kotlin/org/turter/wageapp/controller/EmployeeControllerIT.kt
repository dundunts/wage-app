package org.turter.wageapp.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.ProblemDetail
import org.turter.wageapp.config.CommonWageAppIT
import org.turter.wageapp.config.withUser
import org.turter.wageapp.transport.model.CompanyEmployees
import org.turter.wageapp.transport.model.Employee
import org.turter.wageapp.utils.employee.EmployeePayloadDtoSupplier
import java.util.*

class EmployeeControllerIT : CommonWageAppIT() {

    @BeforeEach
    fun setup() {
        employeeCompanyRepository.deleteAll().block()
        employeeRepository.deleteAll().block()
        companyRepository.deleteAll().block()
    }

    @Test
    @DisplayName("GET /employee/get/{id} — возвращает сотрудника без компаний")
    fun shouldReturnEmployeeWithoutCompanies() {
        val saved = saveNewEmployee()

        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/{id}", saved.id!!)
            .exchange()
            .expectStatus().isOk
            .expectBody(Employee::class.java)
            .returnResult()
            .responseBody!!

        assert(response.id == saved.id)
        assert(response.companyIds.isEmpty())
    }

    @Test
    @DisplayName("GET /employee/get/{id} — возвращает сотрудника с компаниями")
    fun shouldReturnEmployeeWithCompanies() {
//        val companyId1 = companyRepository.save(CompanyEntityFactory.create(title = "company1")).block()?.id!!
//        val companyId2 = companyRepository.save(CompanyEntityFactory.create(title = "company2")).block()?.id!!
//
//        val employee = employeeRepository
//            .save(EmployeeEntityFactory.create())
//            .block()!!
//
//        employeeCompanyRepository.saveAll(
//            listOf(
//                EmployeeCompanyEntityFactory.create(employee.id!!, companyId1),
//                EmployeeCompanyEntityFactory.create(employee.id!!, companyId2)
//            )
//        ).collectList().block()

        val (employee, companies) = saveNewUserEmployeeAndCompanies(companiesCount = 2)

        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/{id}", employee.id!!)
            .exchange()
            .expectStatus().isOk
            .expectBody(Employee::class.java)
            .returnResult()
            .responseBody!!

        assert(response.companyIds.size == 2)
        assert(response.companyIds.containsAll(companies.mapNotNull { company -> company.id }))
    }

    @Test
    @DisplayName("GET /employee/get/{id} — возвращает 404 если сотрудник не найден")
    fun shouldReturn404WhenEmployeeNotFound() {
        client.withUser()
            .get()
            .uri("/api/v1/employee/get/{id}", UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("GET /employee/get/all — возвращает список сотрудников")
    fun shouldReturnAllEmployees() {
//        val e1 = employeeRepository.save(EmployeeEntityFactory.create(firstName = "A")).block()!!
//        val e2 = employeeRepository.save(EmployeeEntityFactory.create(firstName = "B")).block()!!

        val e1 = saveNewEmployee(firstName = "A")
        val e2 = saveNewEmployee(firstName = "B")

        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/all")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Employee::class.java)
            .returnResult()
            .responseBody!!

        assert(response.size == 2)
        assert(response.map { it.id }.containsAll(listOf(e1.id, e2.id)))
    }

    @Test
    @DisplayName("GET /employee/get/all — возвращает пустой список если сотрудников нет")
    fun shouldReturnEmptyEmployeesList() {
        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/all")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Employee::class.java)
            .returnResult()
            .responseBody!!

        assert(response.isEmpty())
    }

    @Test
    @DisplayName("GET /employee/get/by-companies — возвращает сотрудников одной компании")
    fun shouldReturnEmployeesGroupedBySingleCompany() {
        val company = saveNewCompany()

        company.addEmployee(firstName = "Ivan")
        company.addEmployee(firstName = "Petr")

        val companyId = company.id!!

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/employee/get/by-companies")
                    .queryParam("companyIds", companyId)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assert(response.size == 1)
        assert(response.first().companyId == companyId)
        assert(response.first().data.size == 2)
    }

    @Test
    @DisplayName("GET /employee/get/by-companies — возвращает сотрудников, сгруппированных по компаниям")
    fun shouldReturnEmployeesGroupedByMultipleCompanies() {
        val companyA = saveNewCompany(title = "companyA")
        val companyB = saveNewCompany(title = "companyB")

        companyA.addEmployee()
        companyB.addEmployee()

        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/employee/get/by-companies")
                    .queryParam("companyIds", companyA.id, companyB.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assert(response.size == 2)
        assert(response.map { it.companyId }.containsAll(listOf(companyA, companyB).mapNotNull { it.id }))
    }

    @Test
    @DisplayName("GET /employee/get/by-companies — возвращает пустой список если сотрудников нет")
    fun shouldReturnEmptyListWhenNoEmployeesForCompanies() {
        val response = client.withUser()
            .get()
            .uri {
                it.path("/api/v1/employee/get/by-companies")
                    .queryParam("companyIds", UUID.randomUUID())
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assert(response.isEmpty())
    }

    @Test
    @DisplayName("GET /employee/get/by-companies — возвращает 400 при пустом списке companyIds")
    fun shouldReturn400WhenCompanyIdsEmpty() {
        client.withUser()
            .get()
            .uri("/api/v1/employee/get/by-companies")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("GET /employee/get/coworkers — возвращает коллег пользователя")
    fun shouldReturnCoworkersForUser() {
        val (me, company) = saveNewUserEmployeeAndCompany()
        val coworker = company.addEmployee()
        val companyId = company.id!!

        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/coworkers")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assert(response.size == 1)
        assert(response.first().data.size == 2)
    }

    @Test
    @DisplayName("GET /employee/get/coworkers — возвращает список c собственным работником если коллег нет")
    fun shouldReturnEmptyCoworkersList() {
        val (me, company) = saveNewUserEmployeeAndCompany()

        val result = client.withUser()
            .get()
            .uri("/api/v1/employee/get/coworkers")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.size)
        assertEquals(1, result[0].data.size)
        assertEquals(me.id, result[0].data[0].id)
    }

    @Test
    @DisplayName("GET /employee/get/coworkers — возвращает пустой список если пользователь не привязан")
    fun shouldReturnEmptyWhenUserNotBoundToEmployee() {
        val response = client.withUser()
            .get()
            .uri("/api/v1/employee/get/coworkers")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyEmployees::class.java)
            .returnResult()
            .responseBody!!

        assert(response.isEmpty())
    }

    @Test
    @DisplayName("POST /employee/create — успешно создаёт сотрудника")
    fun shouldCreateEmployee() {
        val request = EmployeePayloadDtoSupplier.validForCreate()

        val response = client.withUser()
            .post()
            .uri("/api/v1/employee/create")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(Employee::class.java)
            .returnResult()
            .responseBody!!

        assert(response.firstName == request.firstName)
        assert(response.lastName == request.lastName)
    }

    @Test
    @DisplayName("POST /employee/create — 400 если имя пустое")
    fun shouldFailWhenFirstNameBlank() {
        val request = EmployeePayloadDtoSupplier.validForCreate(firstName = "")

        client.withUser()
            .post()
            .uri("/api/v1/employee/create")
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("POST /employee/create — 400 если companyIds равен null")
    fun shouldFailWhenCompanyIdsIsNull() {
        val request = mapOf(
            "companyIds" to null,
            "firstName" to "Ivan",
            "lastName" to "Ivanov",
            "patronymic" to "Ivanovich",
            "position" to "WAITER_ACTIVE",
        )

        client.withUser()
            .post()
            .uri("/api/v1/employee/create")
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("PUT /employee/update — успешно обновляет сотрудника")
    fun shouldUpdateEmployee() {
        val employee = saveNewEmployee()

        val request = EmployeePayloadDtoSupplier.validForUpdate()

        client.withUser()
            .put()
            .uri("/api/v1/employee/update/{id}", employee.id!!)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNoContent

        val actualDbEntity = employeeRepository.findById(employee.id!!).block()!!

        assertEquals(request.firstName, actualDbEntity.firstName)
    }

    @Test
    @DisplayName("PUT /employee/update — 404 если сотрудник не найден")
    fun updateShouldReturn404WhenEmployeeNotFound() {
        val request = EmployeePayloadDtoSupplier.validForUpdate()

        client.withUser()
            .put()
            .uri("/api/v1/employee/update/{id}", UUID.randomUUID())
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("PUT /employee/update — 409 если пользователь уже привязан к другому сотруднику")
    fun updateShouldReturn409WhenUserAlreadyBound() {
        saveNewEmployee(userId = "user-123")
        val employee = saveNewEmployee()
        val request = EmployeePayloadDtoSupplier.validForUpdate(userId = "user-123")

        client.withUser()
            .put()
            .uri("/api/v1/employee/update/{id}", employee.id!!)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("PUT /employee/bind-user — успешно привязывает пользователя")
    fun shouldBindUserToEmployee() {
        val employee = saveNewEmployee()

        val userId = "user-123"

        client.withUser()
            .put()
            .uri { builder ->
                builder.path("/api/v1/employee/bind-user/{id}")
                    .queryParam("userId", userId)
                    .build(employee.id)
            }
            .exchange()
            .expectStatus().isNoContent

        val updated = employeeRepository.findById(employee.id!!).block()!!
        assertEquals(userId, updated.userId)
    }

    @Test
    @DisplayName("PUT /employee/bind-user — 404 если сотрудник не найден")
    fun bindUserShouldReturn404WhenEmployeeNotFound() {
        client.withUser()
            .put()
            .uri { builder ->
                builder.path("/api/v1/employee/bind-user/{id}")
                    .queryParam("userId", "user-123")
                    .build(UUID.randomUUID())
            }
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("PUT /employee/bind-user — 409 если пользователь уже привязан к другому сотруднику")
    fun bindUserShouldReturn409WhenUserAlreadyBound() {
        saveNewEmployee(userId = "user-123")
        val employee = saveNewEmployee()

        client.withUser()
            .put()
            .uri { builder ->
                builder.path("/api/v1/employee/bind-user/{id}")
                    .queryParam("userId", "user-123")
                    .build(employee.id)
            }
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail::class.java)
    }

    @Test
    @DisplayName("DELETE /employee/delete — успешно удаляет сотрудника")
    fun shouldDeleteEmployee() {
        val employee = saveNewEmployee()

        client.withUser()
            .delete()
            .uri("/api/v1/employee/delete/${employee.id}")
            .exchange()
            .expectStatus().isNoContent

        val exists = employeeRepository.findById(employee.id!!).block()
        assert(exists == null)
    }

}
