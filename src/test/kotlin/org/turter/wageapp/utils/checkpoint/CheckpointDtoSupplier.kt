package org.turter.wageapp.utils.checkpoint

import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.CheckpointType
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import java.time.LocalDateTime
import java.util.*

object CheckpointDtoSupplier {

    fun createRegular(
        sessionId: UUID,
        revenue: Int = 1000,
        tips: Int = 100,
        employeeIds: Set<UUID> = emptySet(),
        dateTime: LocalDateTime = LocalDateTime.now(),
        type: CheckpointType = CheckpointType.REGULAR,
        fieldRecords: List<CheckpointMetricRecordPayload> = emptyList()
    ) = CreateRegularCheckpointPayload(
        sessionId = sessionId,
        revenue = revenue,
        tips = tips,
        employeeIds = employeeIds,
        dateTime = dateTime,
        type = type,
        fieldRecords = fieldRecords
    )

    fun update(
        id: UUID,
        revenue: Int = 1200,
        tips: Int = 150,
        employeeIds: Set<UUID> = emptySet(),
        dateTime: LocalDateTime = LocalDateTime.now(),
        type: CheckpointType = CheckpointType.REGULAR,
        fieldRecords: List<CheckpointMetricRecordPayload> = emptyList()
    ) = UpdateShiftCheckpointPayload(
        id = id,
        revenue = revenue,
        tips = tips,
        employeeIds = employeeIds,
        dateTime = dateTime,
        type = type,
        fieldRecords = fieldRecords
    )

    fun metricRecord(
        label: String = "label",
        destination: CheckpointCalcDestination = CheckpointCalcDestination.REVENUE,
        value: Int = 100
    ) = CheckpointMetricRecordPayload(label, destination, value)
}
