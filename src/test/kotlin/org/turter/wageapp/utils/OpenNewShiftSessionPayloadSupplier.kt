package org.turter.wageapp.utils

import org.turter.wageapp.domain.shift.OpenNewShiftSessionPayload
import java.time.LocalDateTime
import java.util.UUID

object OpenNewShiftSessionPayloadSupplier {

    fun valid(
        companyId: UUID,
        startWorkAt: LocalDateTime = LocalDateTime.now().withHour(9).withMinute(0)
    ): OpenNewShiftSessionPayload =
        OpenNewShiftSessionPayload(
            companyId = companyId,
            startWorkAt = startWorkAt
        )
}
