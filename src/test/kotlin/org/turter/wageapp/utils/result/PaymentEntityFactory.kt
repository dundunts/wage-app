package org.turter.wageapp.utils.result

import org.turter.wageapp.application.data.shift.PaymentDbEntity
import java.util.UUID

object PaymentEntityFactory {

    fun create(
        shiftResultId: UUID,
        employeeId: UUID,
        percentFromRevenue: Int = 10,
        tips: Int = 100,
        workSeconds: Long = 3600
    ): PaymentDbEntity {
        return PaymentDbEntity().apply {
            id = null
            this.shiftResultId = shiftResultId
            this.employeeId = employeeId
            this.percentFromRevenue = percentFromRevenue
            this.tips = tips
            this.workSeconds = workSeconds
        }
    }
}
