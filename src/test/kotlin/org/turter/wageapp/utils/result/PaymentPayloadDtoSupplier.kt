package org.turter.wageapp.utils.result

import org.turter.wageapp.transport.model.ManualOverridePaymentRequest
import java.util.UUID

object PaymentPayloadDtoSupplier {

    fun default(
        employeeId: UUID,
        percentFromRevenue: Int = 10,
        tips: Int = 100,
        workSeconds: Long = 3600
    ): ManualOverridePaymentRequest {
        return ManualOverridePaymentRequest(
            employeeId = employeeId,
            percentFromRevenue = percentFromRevenue,
            tips = tips,
            workSeconds = workSeconds
        )
    }
}
