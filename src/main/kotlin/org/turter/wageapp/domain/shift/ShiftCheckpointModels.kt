package org.turter.wageapp.domain.shift

import java.time.LocalDateTime
import java.util.*

// Data
data class Checkpoint(
    val id: UUID,
    val tips: Int,
    val revenue: Int,
    val employees: List<EmployeeInfo>,
    val dateTime: LocalDateTime,
    val type: CheckpointType,
    val metricRecords: List<CheckpointMetricRecord>,
) {
    data class EmployeeInfo(
        val id: UUID,
        val userId: String?,
        val firstName: String,
        val lastName: String,
        val patronymic: String,
        val simpleName: String?
    )
}

// Data класс для записи поля
data class CheckpointMetricRecord(
    val id: UUID,
    val label: String,
    val destination: CheckpointCalcDestination,
    val value: Int
)

// Enum классы
enum class CheckpointType {
    REGULAR, FINAL
}

enum class CheckpointCalcDestination {
    REVENUE, TIPS
}

// Create
interface ShiftCheckpointPayload {
    val revenue: Int
    val tips: Int
    val employeeIds: List<UUID>
    val dateTime: LocalDateTime
    val type: CheckpointType
    val fieldRecords: List<CheckpointMetricRecordPayload>
}

data class CreateFirstShiftCheckpointPayload(
    val companyId: UUID,
    override val revenue: Int,
    override val tips: Int,
    override val employeeIds: List<UUID>,
    override val dateTime: LocalDateTime,
    override val type: CheckpointType,
    override val fieldRecords: List<CheckpointMetricRecordPayload>
) : ShiftCheckpointPayload

data class CreateRegularCheckpointPayload(
    val sessionId: UUID,
    override val revenue: Int,
    override val tips: Int,
    override val employeeIds: List<UUID>,
    override val dateTime: LocalDateTime,
    override val type: CheckpointType,
    override val fieldRecords: List<CheckpointMetricRecordPayload>
) : ShiftCheckpointPayload

// Update
data class UpdateShiftCheckpointPayload(
    val id: UUID,
    override val revenue: Int,
    override val tips: Int,
    override val employeeIds: List<UUID>,
    override val dateTime: LocalDateTime,
    override val type: CheckpointType,
    override val fieldRecords: List<CheckpointMetricRecordPayload>
) : ShiftCheckpointPayload

// Payload версия без id
data class CheckpointMetricRecordPayload(
    val label: String,
    val destination: CheckpointCalcDestination,
    val value: Int
)

