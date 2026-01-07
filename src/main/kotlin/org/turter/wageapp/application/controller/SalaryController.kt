package org.turter.wageapp.application.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.controller.validation.PeriodRequestParamsValidator
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

}