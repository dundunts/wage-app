package org.turter.wageapp.domain.shift

import java.time.LocalDate
import java.util.*

// Data
class ShiftResultFromDraft {
    val payments: List<Payment>
    val companyId: UUID
    val date: LocalDate
    val sessionId: UUID
    val calculationSource: CalculationSource

    constructor(draft: ShiftResultDraft, companyId: UUID) {
        this.payments = draft.payments.map { paymentDraft ->
            Payment(
                paymentDraft.employee.id,
                paymentDraft.percentFromRevenue,
                paymentDraft.tips,
                paymentDraft.workSeconds
            )
        }
        this.companyId = companyId
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
    val sessionId: UUID?,
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

// Save
data class SaveShiftResultPayload(
    val replacementId: UUID? = null,
    val companyId: UUID,
    val overwrite: Boolean = false,
    val payments: List<PaymentPayload>,
    val date: LocalDate
) {
    val calculationSource = CalculationSource.MANUAL_OVERRIDE

    data class PaymentPayload(
        val employeeId: UUID,
        val percentFromRevenue: Int,
        val tips: Int,
        val workSeconds: Long
    )
}

data class SaveShiftResultResponse(val resultId: UUID)
