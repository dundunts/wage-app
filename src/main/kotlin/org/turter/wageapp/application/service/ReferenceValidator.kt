package org.turter.wageapp.application.service

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.domain.shared.EntityNotFoundException
import java.util.UUID

@Component
class ReferenceValidator(
    private val companyRepository: CompanyRepository,
    private val employeeRepository: EmployeeRepository
) {

    suspend fun requireCompanies(companyIds: Collection<UUID>) {
        val requestedIds = companyIds.toSet()
        if (requestedIds.isEmpty()) return

        val existingIds = companyRepository.findAllById(requestedIds)
            .map { requireNotNull(it.id) }
            .collectList()
            .awaitSingle()
            .toSet()

        requireReferences(ReferenceKind.COMPANY, requestedIds - existingIds)
    }

    suspend fun requireEmployees(employeeIds: Collection<UUID>) {
        val requestedIds = employeeIds.toSet()
        if (requestedIds.isEmpty()) return

        val existingIds = employeeRepository.findAllById(requestedIds)
            .map { requireNotNull(it.id) }
            .collectList()
            .awaitSingle()
            .toSet()

        requireReferences(ReferenceKind.EMPLOYEE, requestedIds - existingIds)
    }

    private fun requireReferences(referenceKind: ReferenceKind, missingIds: Set<UUID>) {
        if (missingIds.isNotEmpty()) {
            throw EntityNotFoundException(missingReferencesDetail(referenceKind, missingIds))
        }
    }
}

enum class ReferenceKind(val pluralName: String) {
    COMPANY("Companies"),
    EMPLOYEE("Employees")
}

fun missingReferencesDetail(referenceKind: ReferenceKind, ids: Collection<UUID>): String =
    "${referenceKind.pluralName} not found: ${ids.sortedBy(UUID::toString)}"

fun concurrentReferenceFailureDetail(referenceKind: ReferenceKind, ids: Collection<UUID>): String =
    "One or more referenced ${referenceKind.pluralName} no longer exist: ${ids.sortedBy(UUID::toString)}"
