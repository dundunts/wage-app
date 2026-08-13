package org.turter.wageapp.utils.session

import org.turter.wageapp.transport.model.UpdateShiftSessionStartRequest
import java.time.LocalTime
import java.util.UUID

object UpdateShiftSessionStartWorkTimePayloadSupplier {

    fun valid(
        sessionId: UUID,
        startWorkTime: LocalTime = LocalTime.of(10, 0)
    ): UpdateShiftSessionStartRequest =
        UpdateShiftSessionStartRequest(
            sessionId = sessionId,
            startWorkTime = startWorkTime.toString()
        )
}
