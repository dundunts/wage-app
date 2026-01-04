package org.turter.wageapp.service.salary

import org.turter.wageapp.domain.salary.Payroll
import org.turter.wageapp.domain.salary.Period
import java.util.UUID

interface SalaryService {

    suspend fun getOwnPayroll(period: Period, companyId: UUID, userId: String): Payroll

    suspend fun getStaffPayroll(period: Period, companyId: UUID, userId: String): Payroll

}