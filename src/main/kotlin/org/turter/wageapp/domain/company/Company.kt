package org.turter.wageapp.domain.company

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.*

//Data
data class Company(
    val id: UUID,
    val title: String,
    val employeeWageCoefficientFromRevenue: Int,
    val defaultShiftStartTime: String
)

data class UserCompaniesResponse(
    val companies: List<Company>
)

// Create, Update
data class CompanyPayload(
    @field:NotEmpty(message = "Title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,
    @field:NotNull(message = "Employee wage coefficient from revenue is required")
    val employeeWageCoefficientFromRevenue: Int,
    @field:Pattern("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
    val defaultShiftStartTime: String
)
