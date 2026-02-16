package org.turter.wageapp.application.controller

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.controller.validation.PeriodRequestParamsValidator
import org.turter.wageapp.application.service.salary.ExcelExporterService
import org.turter.wageapp.domain.salary.Payroll
import org.turter.wageapp.domain.salary.PeriodType
import org.turter.wageapp.application.service.salary.SalaryService
import java.security.Principal
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/salary")
class SalaryController(
    private val salaryService: SalaryService,
    private val excelExporterService: ExcelExporterService,
    private val periodValidator: PeriodRequestParamsValidator
) {

    @GetMapping("/own/get")
    suspend fun getOwnSalary(
        @RequestParam companyId: UUID,
        @RequestParam periodType: PeriodType,
        @RequestParam(required = false) start: LocalDate,
        @RequestParam(required = false) end: LocalDate,
        @RequestParam(required = false) now: LocalDate,
        principal: Principal
    ): ResponseEntity<Payroll> {
        val period = periodValidator.validateAndBuildPeriod(periodType, start, end, now)

        return ResponseEntity.ok(salaryService.getOwnPayroll(period, companyId, principal.name))
    }

    @GetMapping("/staff/get")
    suspend fun getStaffSalary(
        @RequestParam companyId: UUID,
        @RequestParam periodType: PeriodType,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?,
        @RequestParam(required = false) now: LocalDate?,
        principal: Principal
    ): ResponseEntity<Payroll> {
        val period = periodValidator.validateAndBuildPeriod(periodType, start, end, now)

        return ResponseEntity.ok(salaryService.getStaffPayroll(period, companyId, principal.name))
    }

    @GetMapping("/reports-table")
    suspend fun exportReportsToExcel(
        @RequestParam companyId: UUID,
        @RequestParam periodType: PeriodType,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?,
        @RequestParam(required = false) now: LocalDate?,
        principal: Principal
    ): ResponseEntity<Resource> {
        // 1. Валидация и получение данных (бизнес-логика)
        val period = periodValidator.validateAndBuildPeriod(periodType, start, end, now)
        val payroll = salaryService.getStaffPayroll(period, companyId, principal.name)

        // 2. Генерация файла (передаем в сервис, он сам переключится на IO)
        val excelBytes = excelExporterService.generatePayrollExcel(payroll)
        val resource = ByteArrayResource(excelBytes)

        // 3. Формирование ответа
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reports.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(resource.contentLength())
            .body(resource)
    }

}