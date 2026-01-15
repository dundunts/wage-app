package org.turter.wageapp.utils

import org.turter.wageapp.domain.shift.UpdateShiftSessionStartWorkTimePayload
import java.time.LocalTime
import java.util.UUID

object UpdateShiftSessionStartWorkTimePayloadSupplier {

    fun valid(
        sessionId: UUID,
        startWorkTime: LocalTime = LocalTime.of(10, 0)
    ): UpdateShiftSessionStartWorkTimePayload =
        UpdateShiftSessionStartWorkTimePayload(
            sessionId = sessionId,
            startWorkTime = startWorkTime
        )
}
