package org.turter.wageapp.domain.shift

import org.turter.wageapp.domain.employee.Employee
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Data
class ShiftResultFromDraft {
    val payments: List<Payment>
    val date: LocalDate
    val sessionId: UUID
    val calculationSource: CalculationSource

    constructor(draft: ShiftResultDraft) {
        this.payments = draft.payments.map { paymentDraft ->
            Payment(
                paymentDraft.employee.id,
                paymentDraft.percentFromRevenue,
                paymentDraft.tips,
                paymentDraft.workSeconds
            )
        }
        this.date = draft.date
        this.sessionId = draft.sessionId
        this.calculationSource = CalculationSource.CHECKPOINTS
    }

    data class Payment(
        val employeeId: UUID,
        val percentFromRevenue: Int,
        val tips: Int,
        val workSeconds: Long
    )
}

data class ShiftResultDetailed(
    val id: UUID,
    val payments: List<Payment>,
    val date: LocalDate,
    val session: ShiftSession?,
    val calculationSource: CalculationSource,
) {
    data class Payment(
        val id: UUID,
        val employee: EmployeeInfo,
        val percentFromRevenue: Int,
        val tips: Int,
        val workSeconds: Long
    ) {
        data class EmployeeInfo(
            val id: UUID,
            val firstName: String,
            val lastName: String,
            val patronymic: String,
            val simpleName: String?
        )
    }
}

enum class CalculationSource {
    CHECKPOINTS,     // рассчитано строго из чекпоинтов
    MANUAL_OVERRIDE  // вручную исправлено админом
}

// Create
data class CreateShiftResultPayload(
    val payments: List<CreateEmployeePaymentPayload>,
    val date: LocalDateTime
)

data class CreateEmployeePaymentPayload(
    val employee: Employee,
    val percentFromRevenue: Int,
    val tips: Int,
    val startWorkAt: LocalDateTime,
    val endWorkAt: LocalDateTime
)

// Update
data class UpdateShiftResultDatePayload(
    val payments: List<UpdateEmployeePaymentPayload>,
    val date: LocalDateTime
)

data class UpdateEmployeePaymentPayload(
    val id: UUID,
    val percentFromRevenue: Int,
    val tips: Int,
    val startWorkAt: LocalDateTime,
    val endWorkAt: LocalDateTime
)