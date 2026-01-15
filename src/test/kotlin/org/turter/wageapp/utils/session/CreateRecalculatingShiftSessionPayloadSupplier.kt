package org.turter.wageapp.utils.session

import org.turter.wageapp.domain.shift.CreateRecalculatingShiftSessionPayload
import java.util.UUID

object CreateRecalculatingShiftSessionPayloadSupplier {

    fun valid(closedSessionId: UUID): CreateRecalculatingShiftSessionPayload =
        CreateRecalculatingShiftSessionPayload(closedSessionId)
}
