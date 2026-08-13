package org.turter.wageapp.application.controller

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.controller.validation.PeriodRequestParamsValidator
import org.turter.wageapp.application.mapper.toTransport
import org.turter.wageapp.application.service.salary.ExcelExporterService
import org.turter.wageapp.application.service.salary.SalaryService
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.domain.salary.PeriodType
import org.turter.wageapp.transport.api.PayrollApi
import org.turter.wageapp.transport.model.Payroll
import java.time.LocalDate
import java.util.UUID

@RestController
class SalaryController(
    private val salaryService: SalaryService,
    private val excelExporterService: ExcelExporterService,
    private val periodValidator: PeriodRequestParamsValidator,
) : PayrollApi {

    override suspend fun getOwnPayroll(
        companyId: UUID,
        periodType: String,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?,
    ): ResponseEntity<Payroll> {
        val period = buildPeriod(periodType, start, end, now)

        return ResponseEntity.ok(salaryService.getOwnPayroll(period, companyId, currentUserId()).toTransport())
    }

    override suspend fun getStaffPayroll(
        companyId: UUID,
        periodType: String,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?,
    ): ResponseEntity<Payroll> {
        val period = buildPeriod(periodType, start, end, now)

        return ResponseEntity.ok(salaryService.getStaffPayroll(period, companyId, currentUserId()).toTransport())
    }

    override suspend fun exportStaffPayroll(
        companyId: UUID,
        periodType: String,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?,
    ): ResponseEntity<Resource> {
        val period = buildPeriod(periodType, start, end, now)
        val payroll = salaryService.getStaffPayroll(period, companyId, currentUserId())

        val excelBytes = excelExporterService.generatePayrollExcel(payroll)
        val resource = ByteArrayResource(excelBytes)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reports.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(resource.contentLength())
            .body(resource)
    }

    private fun buildPeriod(
        periodType: String,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?,
    ): Period = periodValidator.validateAndBuildPeriod(PeriodType.valueOf(periodType), start, end, now)
}
