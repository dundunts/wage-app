package org.turter.wageapp.utils.checkpoint

import org.turter.wageapp.transport.model.CheckpointMetricDestination
import org.turter.wageapp.transport.model.CheckpointMetricRecordRequest
import org.turter.wageapp.transport.model.CheckpointType
import org.turter.wageapp.transport.model.UpdateCheckpointRequest
import java.time.LocalDateTime
import java.util.*

object CheckpointDtoSupplier {

    fun update(
        id: UUID,
        revenue: Int = 1200,
        tips: Int = 150,
        employeeIds: Set<UUID> = emptySet(),
        dateTime: LocalDateTime = LocalDateTime.now(),
        type: CheckpointType = CheckpointType.REGULAR,
        fieldRecords: List<CheckpointMetricRecordRequest> = emptyList()
    ) = UpdateCheckpointRequest(
        id = id,
        revenue = revenue,
        tips = tips,
        employeeIds = employeeIds,
        dateTime = dateTime.toString(),
        type = type,
        fieldRecords = fieldRecords
    )

    fun metricRecord(
        label: String = "label",
        destination: CheckpointMetricDestination = CheckpointMetricDestination.REVENUE,
        value: Int = 100
    ) = CheckpointMetricRecordRequest(label, destination, value)
}
