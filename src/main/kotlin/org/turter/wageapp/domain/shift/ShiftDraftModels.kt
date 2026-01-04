package org.turter.wageapp.domain.shift

import java.time.LocalDate
import java.util.*

data class ShiftResultDraft(
    val id: UUID,
    val payments: List<PaymentDraft>,
    val date: LocalDate,
    val sessionId: UUID,
)

data class PaymentDraft(
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

data class ConfirmDraftResponse(
    val resultId: UUID
)