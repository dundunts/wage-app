package org.turter.wageapp.utils

import org.turter.wageapp.application.data.company.CompanyDbEntity
import java.util.*

object CompanyEntityFactory {

    fun create(
        title: String = "Test company",
        employeeWageCoefficientFromRevenue: Int = 10,
        defaultShiftStartTime: String = "09:00"
    ): CompanyDbEntity =
        CompanyDbEntity().apply {
            this.title = title
            this.employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue
            this.defaultShiftStartTime = defaultShiftStartTime
        }
}
