package org.turter.wageapp.domain.company

import java.util.*

//Data
data class Company(
    val id: UUID,
    val title: String,
    val employeeWageCoefficientFromRevenue: Int,
    val defaultShiftStartTime: String
)

// Create, Update
data class CompanyPayload(
    val title: String,
    val employeeWageCoefficientFromRevenue: Int,
    val defaultShiftStartTime: String
)
