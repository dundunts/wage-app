package org.turter.wageapp.utils.company

import org.turter.wageapp.transport.model.CompanyCreateOrUpdateRequest

object CompanyPayloadDtoSupplier {

    fun valid(
        title: String = "New company",
        employeeWageCoefficientFromRevenue: Int = 10,
        defaultShiftStartTime: String = "09:00"
    ): CompanyCreateOrUpdateRequest =
        CompanyCreateOrUpdateRequest(
            title = title,
            employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue,
            defaultShiftStartTime = defaultShiftStartTime
        )

    fun withEmptyTitle(): CompanyCreateOrUpdateRequest =
        valid(title = "")

    fun withInvalidShiftStartTime(): CompanyCreateOrUpdateRequest =
        valid(defaultShiftStartTime = "25:99")
}
