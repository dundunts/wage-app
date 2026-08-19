package org.turter.wageapp.application.service.employee

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.application.data.employee.EmployeeCompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.employee.entity.EmployeeCompanyDbEntity
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.application.data.employee.entity.EmployeeWithCompanyRow
import org.turter.wageapp.application.service.ReferenceValidator
import org.turter.wageapp.application.service.ReferenceKind
import org.turter.wageapp.application.service.mapForeignKeyViolation
import org.turter.wageapp.application.service.mapDuplicateKey
import org.turter.wageapp.domain.employee.CompanyEmployeeInfo
import org.turter.wageapp.domain.employee.CompanyEmployeesResponse
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shared.NotUniqueValueException
import org.turter.wageapp.application.mapper.EmployeeMapper
import java.util.*

@Service
class EmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val employeeCompanyRepository: EmployeeCompanyRepository,
    private val mapper: EmployeeMapper,
    private val referenceValidator: ReferenceValidator
) : EmployeeService {

    override suspend fun getById(id: UUID): Employee {
        val entity = employeeRepository.findById(id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Employee $id not found")

        val companyIds = employeeCompanyRepository
            .findAllByEmployeeId(id)
            .collectList()
            .awaitSingle()
            .mapNotNull(EmployeeCompanyDbEntity::companyId)

        return mapper.toEmployee(entity, companyIds)
    }

    override suspend fun getAll(): List<Employee> =
        employeeRepository.findAll()
            .flatMap { entity ->
                employeeCompanyRepository.findAllByEmployeeId(entity.id!!)
                    .map { it.companyId!! }
                    .collectList()
                    .map { mapper.toEmployee(entity, it) }
            }
            .collectList()
            .awaitSingle()

    override suspend fun getGroupedByCompanies(companyIds: List<UUID>): List<CompanyEmployeesResponse> =
        employeeRepository.findEmployeesByCompanyIds(companyIds)
            .collectList().awaitSingle().toCompanyEmployeeResponse()

    override suspend fun getCoworkersByUserId(userId: String): List<CompanyEmployeesResponse> =
        employeeRepository.findCoworkersByUserId(userId).collectList().awaitSingle().toCompanyEmployeeResponse()

    @Transactional
    override suspend fun create(payload: CreateEmployeePayload): Employee {
        referenceValidator.requireCompanies(payload.companyIds)

        val saved = employeeRepository.save(mapper.toNewEmployeeDbEntity(payload)).awaitSingle()

        mapForeignKeyViolation(ReferenceKind.COMPANY, payload.companyIds) {
            employeeCompanyRepository.saveAll(
                payload.companyIds.map {
                    EmployeeCompanyDbEntity().apply { employeeId = saved.id; companyId = it }
                }
            ).collectList().awaitSingle()
        }

        return mapper.toEmployee(saved, payload.companyIds)
    }

    @Transactional
    override suspend fun update(id: UUID, payload: UpdateEmployeePayload): Employee {
        val entity = employeeRepository.findById(id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Employee $id not found")

        referenceValidator.requireCompanies(payload.companyIds)

        mapper.mergeToEmployeeDbEntity(payload, entity)
        mapDuplicateKey(
            exception = { e ->
                NotUniqueValueException(
                    "User '${payload.userId}' is already bound to another Employee",
                    e
                )
            }
        ) {
            employeeRepository.save(entity).awaitSingle()
        }

        val currentCompanies = employeeCompanyRepository.findAllByEmployeeId(id).collectList().awaitSingle()

        val newCompaniesSet = payload.companyIds.toMutableSet()

        employeeCompanyRepository.deleteAll(
            currentCompanies.filter { !newCompaniesSet.contains(it.companyId) }
        ).awaitSingleOrNull()

        currentCompanies.forEach {
            if (newCompaniesSet.contains(it.companyId)) newCompaniesSet.remove(it.companyId)
        }

        mapForeignKeyViolation(ReferenceKind.COMPANY, payload.companyIds) {
            employeeCompanyRepository.saveAll(
                newCompaniesSet.map { EmployeeCompanyDbEntity(id, it) }
            ).collectList().awaitSingle()
        }

        referenceValidator.requireCompanies(payload.companyIds)

        return mapper.toEmployee(entity, payload.companyIds)
    }

    override suspend fun delete(id: UUID) {
        employeeRepository.deleteById(id).awaitSingleOrNull()
    }

    override suspend fun bindUser(employeeId: UUID, userId: String) {
        val entity = employeeRepository.findById(employeeId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Employee $employeeId not found")

        entity.userId = userId
        mapDuplicateKey(
            exception = { e ->
                NotUniqueValueException(
                    "User '$userId' is already bound to another Employee",
                    e
                )
            }
        ) {
            employeeRepository.save(entity).awaitSingle()
        }
    }

    private fun List<EmployeeWithCompanyRow>.toCompanyEmployeeResponse(): List<CompanyEmployeesResponse> =
        groupBy { it.companyId }
            .map { (companyId, employees) ->
                CompanyEmployeesResponse(
                    companyId = companyId,
                    data = employees.map {
                        CompanyEmployeeInfo(
                            id = it.employeeId,
                            userId = it.userId,
                            firstName = it.firstName,
                            lastName = it.lastName,
                            patronymic = it.patronymic,
                            simpleName = it.simpleName,
                            position = it.position
                        )
                    }
                )
            }
}
