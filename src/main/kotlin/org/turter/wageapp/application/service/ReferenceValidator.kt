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

        requireReferences("Companies", requestedIds - existingIds)
    }

    suspend fun requireEmployees(employeeIds: Collection<UUID>) {
        val requestedIds = employeeIds.toSet()
        if (requestedIds.isEmpty()) return

        val existingIds = employeeRepository.findAllById(requestedIds)
            .map { requireNotNull(it.id) }
            .collectList()
            .awaitSingle()
            .toSet()

        requireReferences("Employees", requestedIds - existingIds)
    }

    private fun requireReferences(entityName: String, missingIds: Set<UUID>) {
        if (missingIds.isNotEmpty()) {
            throw EntityNotFoundException(missingReferencesDetail(entityName, missingIds))
        }
    }
}

fun missingReferencesDetail(entityName: String, ids: Collection<UUID>): String =
    "$entityName not found: ${ids.sortedBy(UUID::toString)}"
