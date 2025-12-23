package org.turter.wageapp.domain.company

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.intellij.lang.annotations.Pattern
import java.util.UUID

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
    val title: String,
    @field:NotNull(message = "Employee wage coefficient from revenue is required")
    val employeeWageCoefficientFromRevenue: Int,
    @field:NotEmpty(message = "Default shift start time is required")
    @field:Pattern("^([01]\\d|2[0-3]):[0-5]\\d$")
    val defaultShiftStartTime: String
)

data class BindUserToCompanyPayload(
    @field:NotNull(message = "User ID is required")
    val userId: UUID
)