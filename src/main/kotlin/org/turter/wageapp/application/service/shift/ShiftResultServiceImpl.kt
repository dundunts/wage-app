package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.shift.PaymentRepository
import org.turter.wageapp.application.data.shift.ShiftResultRepository
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.SaveShiftResultResponse
import org.turter.wageapp.domain.shift.ShiftResultConflictException
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import org.turter.wageapp.application.mapper.ShiftResultMapper
import org.turter.wageapp.application.service.ReferenceValidator
import org.turter.wageapp.application.service.mapDuplicateKey
import org.turter.wageapp.application.service.mapInvalidReference
import org.turter.wageapp.application.service.missingReferencesDetail
import org.turter.wageapp.application.service.shift.ShiftResultService
import org.turter.wageapp.application.service.shift.shiftResultConflictDetail
import org.turter.wageapp.application.service.shift.validateUserCompanyBind
import java.util.*

@Service
@Transactional
class ShiftResultServiceImpl(
    private val shiftResultRepository: ShiftResultRepository,
    private val paymentRepository: PaymentRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository,
    private val shiftResultMapper: ShiftResultMapper,
    private val referenceValidator: ReferenceValidator
) : ShiftResultService {

    override suspend fun getDetailed(
        resultId: UUID,
        userId: String
    ): ShiftResultDetailed {
        val result = shiftResultRepository.findById(resultId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Shift result not found: $resultId")

        validateUserCompanyBind(userId, result.companyId!!, companyRepository)

        val payments = paymentRepository.findAllByShiftResultId(resultId)
            .flatMap { payment ->
                employeeRepository.findById(payment.employeeId!!)
                    .map { employee ->
                        shiftResultMapper.toPayment(payment, employee)
                    }
            }
            .collectList()
            .awaitSingle()
        return shiftResultMapper.toDetailed(result, payments)
    }

    override suspend fun getDetailedPage(
        companyId: UUID,
        pageable: Pageable,
        period: Period,
        userId: String
    ): Page<ShiftResultDetailed> {
        validateUserCompanyBind(userId, companyId, companyRepository)

        val results = shiftResultRepository
            .findAllByCompanyIdAndDateBetweenOrderByDate(companyId, period.start, period.end, pageable)
            .flatMap { result ->
                paymentRepository.findAllByShiftResultId(result.id!!)
                    .flatMap { payment ->
                        employeeRepository.findById(payment.employeeId!!)
                            .map { employee ->
                                shiftResultMapper.toPayment(payment, employee)
                            }
                    }
                    .collectList()
                    .map { payments ->
                        shiftResultMapper.toDetailed(result, payments)
                    }
            }
            .collectList()
            .awaitSingle()

        return PageImpl(
            results,
            pageable,
            shiftResultRepository.countAllByCompanyIdAndDateBetween(companyId, period.start, period.end)
                .awaitSingle()
        )
    }

    override suspend fun save(
        payload: SaveShiftResultPayload,
        userId: String
    ): SaveShiftResultResponse {
        validateUserCompanyBind(userId, payload.companyId, companyRepository)

        if (hasDuplicateIds(payload.payments)) throw ShiftResultConflictException(
            "Payments contains duplicates employee IDs"
        )

        val paymentEmployeeIds = payload.payments.map { it.employeeId }
        referenceValidator.requireEmployees(paymentEmployeeIds)

        val existingByCompanyAndDate = shiftResultRepository
            .findByCompanyIdAndDate(payload.companyId, payload.date)
            .awaitSingleOrNull()

        if (existingByCompanyAndDate != null && !payload.overwrite) {
            throw ShiftResultConflictException(shiftResultConflictDetail(payload.companyId, payload.date))
        }

        payload.replacementId?.let { replacementId ->
            paymentRepository.deleteAllByShiftResultId(replacementId).awaitSingleOrNull()

            shiftResultRepository.deleteById(replacementId).awaitSingleOrNull()
        }

        existingByCompanyAndDate?.let { resultDbEntity ->
            paymentRepository.deleteAllByShiftResultId(resultDbEntity.id!!).awaitSingleOrNull()

            shiftResultRepository.delete(resultDbEntity).awaitSingleOrNull()
        }

        val savedResult = mapDuplicateKey(
            exception = { e ->
                ShiftResultConflictException(shiftResultConflictDetail(payload.companyId, payload.date), e)
            }
        ) {
            shiftResultRepository.save(
                shiftResultMapper.toNewShiftResultDbEntityFromPayload(payload)
            ).awaitSingle()
        }

        mapInvalidReference(
            exception = { e ->
                EntityNotFoundException(
                    missingReferencesDetail("Referenced Employees", paymentEmployeeIds),
                    e
                )
            }
        ) {
            paymentRepository.saveAll(
                payload.payments.map { paymentPayload ->
                    shiftResultMapper.toNewPaymentDbEntityFromPayload(paymentPayload, savedResult.id!!)
                }
            ).collectList().awaitSingle()
        }

        return SaveShiftResultResponse(savedResult.id!!)
    }

    override suspend fun delete(
        resultId: UUID,
        userId: String
    ) {
        val result = shiftResultRepository.findById(resultId).awaitSingleOrNull()
            ?: return

        validateUserCompanyBind(userId, result.companyId!!, companyRepository)

        paymentRepository.deleteAllByShiftResultId(resultId).awaitSingleOrNull()

        shiftResultRepository.deleteById(resultId).awaitSingleOrNull()
    }

    private fun hasDuplicateIds(payloads: List<SaveShiftResultPayload.PaymentPayload>): Boolean {
        val seenIds = mutableSetOf<UUID>()
        return payloads.any { !seenIds.add(it.employeeId) }
    }

}
