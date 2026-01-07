package org.turter.wageapp.service.salary

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.shift.PaymentRepository
import org.turter.wageapp.application.data.shift.ShiftResultRepository
import org.turter.wageapp.domain.salary.Payroll
import org.turter.wageapp.domain.salary.PayrollAggregator
import org.turter.wageapp.domain.salary.Period
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.application.mapper.PayrollMapper
import org.turter.wageapp.application.service.salary.SalaryService
import org.turter.wageapp.application.service.shift.validateUserCompanyBind
import reactor.core.publisher.Mono
import java.util.*

@Service
class SalaryServiceImpl(
    private val shiftResultRepository: ShiftResultRepository,
    private val paymentRepository: PaymentRepository,
    private val companyRepository: CompanyRepository,
    private val employeeRepository: EmployeeRepository,
    private val payrollMapper: PayrollMapper,
    private val payrollAggregator: PayrollAggregator
) : SalaryService {
    override suspend fun getOwnPayroll(
        period: Period,
        companyId: UUID,
        userId: String
    ): Payroll {
        validateUserCompanyBind(userId, companyId, companyRepository)

        val userEmployee = employeeRepository.findByUserId(userId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Employee not found for user ID: {$userId}")

        val elements = shiftResultRepository.findAllByCompanyIdAndDateBetween(
            companyId,
            period.start,
            period.end
        )
            .flatMap { result ->
                paymentRepository.findByShiftResultIdAndEmployeeId(result.id!!, userEmployee.id!!)
                    .zipWith(Mono.just(result))
            }
            .collectList()
            .awaitSingle()
            .mapNotNull { tuple ->
                Payroll.Element(
                    date = tuple.t2.date!!,
                    payments = listOf(payrollMapper.toPayment(tuple.t1, userEmployee))
                )
            }
            .sortedBy { element -> element.date }

        return payrollAggregator.aggregate(elements)
    }

    override suspend fun getStaffPayroll(
        period: Period,
        companyId: UUID,
        userId: String
    ): Payroll {
        validateUserCompanyBind(userId, companyId, companyRepository)

        val elements = shiftResultRepository.findAllByCompanyIdAndDateBetween(
            companyId,
            period.start,
            period.end
        )
            .flatMap { result ->
                paymentRepository.findAllByShiftResultId(result.id!!)
                    .flatMap { payment ->
                        employeeRepository.findById(payment.employeeId!!)
                            .map { emp ->
                                payrollMapper.toPayment(payment, emp)
                            }
                    }
                    .collectList()
                    .zipWith(Mono.just(result))
            }
            .collectList()
            .awaitSingle()
            .mapNotNull { tuple ->
                Payroll.Element(
                    date = tuple.t2.date!!,
                    payments = tuple.t1
                )
            }
            .sortedBy { element -> element.date }

        return payrollAggregator.aggregate(elements)
    }
}