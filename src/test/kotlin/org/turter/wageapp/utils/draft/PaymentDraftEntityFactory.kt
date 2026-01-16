package org.turter.wageapp.utils.draft

import org.turter.wageapp.application.data.shift.PaymentDraftDbEntity
import java.util.UUID

object PaymentDraftEntityFactory {

    fun create(
        id: UUID? = null,
        shiftResultDraftId: UUID? = null,
        employeeId: UUID,
        percentFromRevenue: Int = 10,
        tips: Int = 100,
        workSeconds: Long = 3600
    ): PaymentDraftDbEntity {
        val payment = PaymentDraftDbEntity()
        payment.id = id
        payment.shiftResultDraftId = shiftResultDraftId
        payment.employeeId = employeeId
        payment.percentFromRevenue = percentFromRevenue
        payment.tips = tips
        payment.workSeconds = workSeconds
        return payment
    }
}
