package org.turter.wageapp.utils.session

import org.turter.wageapp.transport.model.OpenShiftSessionRecalculationRequest
import java.util.UUID

object CreateRecalculatingShiftSessionPayloadSupplier {

    fun valid(closedSessionId: UUID): OpenShiftSessionRecalculationRequest =
        OpenShiftSessionRecalculationRequest(closedSessionId)
}
