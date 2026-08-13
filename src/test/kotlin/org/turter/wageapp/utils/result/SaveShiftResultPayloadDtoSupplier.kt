package org.turter.wageapp.utils.result

import org.turter.wageapp.transport.model.ManualOverridePaymentRequest
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultRequest
import java.time.LocalDate
import java.util.UUID

object SaveShiftResultPayloadDtoSupplier {

    fun default(
        replacementId: UUID? = null,
        companyId: UUID,
        date: LocalDate,
        payments: List<ManualOverridePaymentRequest>,
        overwrite: Boolean = false
    ): SaveManualOverrideShiftResultRequest {
        return SaveManualOverrideShiftResultRequest(
            companyId = companyId,
            payments = payments,
            date = date,
            replacementId = replacementId,
            overwrite = overwrite,
        )
    }
}
