package org.turter.wageapp.utils.company

import org.turter.wageapp.domain.company.CompanyPayload

object CompanyPayloadDtoSupplier {

    fun valid(
        title: String = "New company",
        employeeWageCoefficientFromRevenue: Int = 10,
        defaultShiftStartTime: String = "09:00"
    ): CompanyPayload =
        CompanyPayload(
            title = title,
            employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue,
            defaultShiftStartTime = defaultShiftStartTime
        )

    fun withEmptyTitle(): CompanyPayload =
        valid(title = "")

    fun withInvalidShiftStartTime(): CompanyPayload =
        valid(defaultShiftStartTime = "25:99")
}
