package org.turter.wageapp.utils.result

import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import java.time.LocalDate
import java.util.UUID

object SaveShiftResultPayloadDtoSupplier {

    fun default(
        replacementId: UUID? = null,
        companyId: UUID,
        date: LocalDate,
        payments: List<SaveShiftResultPayload.PaymentPayload>,
        overwrite: Boolean = false
    ): SaveShiftResultPayload {
        return SaveShiftResultPayload(
            replacementId = replacementId,
            companyId = companyId,
            overwrite = overwrite,
            payments = payments,
            date = date
        )
    }
}
