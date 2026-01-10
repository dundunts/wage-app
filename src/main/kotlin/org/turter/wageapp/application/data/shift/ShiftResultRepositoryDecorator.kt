package org.turter.wageapp.application.data.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Repository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.mapper.ShiftResultMapper
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import java.util.UUID

@Repository
class ShiftResultRepositoryDecorator(
    private val shiftResultRepository: ShiftResultRepository,
    private val paymentRepository: PaymentRepository,
    private val employeeRepository: EmployeeRepository,
    private val shiftResultMapper: ShiftResultMapper
) {

    suspend fun findDetailedById(id: UUID): ShiftResultDetailed? {
        val result = shiftResultRepository.findById(id).awaitSingleOrNull() ?: return null

        val payments = paymentRepository.findAllByShiftResultId(id)
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

}