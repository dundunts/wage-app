package org.turter.wageapp.utils.session

import org.turter.wageapp.transport.model.OpenShiftSessionRequest
import java.time.LocalDateTime
import java.util.UUID

object OpenNewShiftSessionPayloadSupplier {

    fun valid(
        companyId: UUID,
        startWorkAt: LocalDateTime = LocalDateTime.now().withHour(9).withMinute(0)
    ): OpenShiftSessionRequest =
        OpenShiftSessionRequest(
            companyId = companyId,
            startWorkAt = startWorkAt.toString()
        )
}
