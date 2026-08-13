package org.turter.wageapp.utils.payload

import org.turter.wageapp.transport.model.CheckpointMetricDestination
import org.turter.wageapp.transport.model.CheckpointMetricRecordRequest
import org.turter.wageapp.transport.model.CheckpointType
import org.turter.wageapp.transport.model.CreateCheckpointRequest
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
    ): CreateCheckpointRequest =
        CreateCheckpointRequest(
            sessionId = sessionId,
            revenue = revenue,
            tips = tips,
            employeeIds = employeeIds,
            dateTime = dateTime.toString(),
            type = type,
            fieldRecords = listOf(
                CheckpointMetricRecordRequest(
                    label = "revenue",
                    destination = CheckpointMetricDestination.REVENUE,
                    value = revenue
                ),
                CheckpointMetricRecordRequest(
                    label = "tips",
                    destination = CheckpointMetricDestination.TIPS,
                    value = tips
                )
            )
        )
}
