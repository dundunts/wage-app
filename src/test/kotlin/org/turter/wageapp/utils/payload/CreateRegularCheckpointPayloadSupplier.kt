package org.turter.wageapp.utils.payload

import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.CheckpointType
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import java.time.LocalDateTime
import java.util.UUID

object CreateRegularCheckpointPayloadSupplier {

    fun valid(
        sessionId: UUID,
        employeeIds: Set<UUID>,
        revenue: Int = 10_000,
        tips: Int = 1_000,
        dateTime: LocalDateTime = LocalDateTime.now(),
        type: CheckpointType = CheckpointType.REGULAR
    ): CreateRegularCheckpointPayload =
        CreateRegularCheckpointPayload(
            sessionId = sessionId,
            revenue = revenue,
            tips = tips,
            employeeIds = employeeIds,
            dateTime = dateTime,
            type = type,
            fieldRecords = listOf(
                CheckpointMetricRecordPayload(
                    label = "revenue",
                    destination = CheckpointCalcDestination.REVENUE,
                    value = revenue
                ),
                CheckpointMetricRecordPayload(
                    label = "tips",
                    destination = CheckpointCalcDestination.TIPS,
                    value = tips
                )
            )
        )
}
