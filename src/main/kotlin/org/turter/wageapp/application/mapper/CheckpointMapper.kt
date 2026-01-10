package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.application.data.shift.CheckpointDbEntity
import org.turter.wageapp.application.data.shift.CheckpointEmployeeDbEntity
import org.turter.wageapp.application.data.shift.CheckpointMetricRecordDbEntity
import org.turter.wageapp.domain.shift.CheckpointMetricRecord
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.calculator.CheckpointInfo
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import java.time.Instant
import java.util.*

@Mapper(componentModel = "spring")
abstract class CheckpointMapper {

    abstract fun toShiftCheckpoint(
        entity: CheckpointDbEntity,
        metricRecords: List<CheckpointMetricRecordDbEntity>,
        employees: List<EmployeeDbEntity>
    ): Checkpoint

    abstract fun toCheckpointInfo(entity: CheckpointDbEntity, employeeIds: List<UUID>): CheckpointInfo

    abstract fun toShiftCheckpointEmployeeInfo(entity: EmployeeDbEntity): Checkpoint.EmployeeInfo

    abstract fun toShiftCheckpointEmployeeInfoList(entity: List<EmployeeDbEntity>): List<Checkpoint.EmployeeInfo>

    @Mapping(target = "id", ignore = true)
    abstract fun toNewCheckpointDbEntity(payload: ShiftCheckpointPayload, shiftSessionId: UUID): CheckpointDbEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shiftSessionId", ignore = true)
    abstract fun mergeToCheckpointDbEntity(
        payload: ShiftCheckpointPayload,
        @MappingTarget entity: CheckpointDbEntity
    ): CheckpointDbEntity

    @Mapping(target = "id", ignore = true)
    abstract fun toNewCheckpointEmployeeDbEntity(checkpointId: UUID, employeeId: UUID): CheckpointEmployeeDbEntity

    fun toNewCheckpointEmployeeDbEntityList(
        shiftSessionCheckpointId: UUID,
        employeeIds: List<UUID>
    ): List<CheckpointEmployeeDbEntity> =
        employeeIds.map { empId -> toNewCheckpointEmployeeDbEntity(shiftSessionCheckpointId, empId) }

    abstract fun toCheckpointMetricRecord(entity: CheckpointMetricRecordDbEntity): CheckpointMetricRecord

    abstract fun toCheckpointMetricRecordList(entity: List<CheckpointMetricRecordDbEntity>): List<CheckpointMetricRecord>

    @Mapping(target = "id", ignore = true)
    abstract fun toNewCheckpointMetricRecord(
        payload: CheckpointMetricRecordPayload,
        checkpointId: UUID
    ): CheckpointMetricRecordDbEntity

    fun toNewCheckpointMetricRecordList(
        payloads: List<CheckpointMetricRecordPayload>,
        checkpointId: UUID
    ): List<CheckpointMetricRecordDbEntity> =
        payloads.map { payload -> toNewCheckpointMetricRecord(payload, checkpointId) }

    fun toCheckpointReplacedNotificationEvent(
        checkpoint: Checkpoint,
        replacedCheckpointId: UUID,
        companyId: UUID
    ): NotificationEvent.CheckpointReplaced =
        NotificationEvent.CheckpointReplaced(
            meta = NotificationEvent.Meta(companyId = companyId, Instant.now()),
            replacedCheckpointId = replacedCheckpointId,
            checkpoint = checkpoint
        )

    fun toCheckpointSavedNotificationEvent(
        checkpoint: Checkpoint,
        companyId: UUID
    ): NotificationEvent.CheckpointSaved =
        NotificationEvent.CheckpointSaved(
            meta = NotificationEvent.Meta(companyId = companyId, Instant.now()),
            checkpoint = checkpoint
        )

    fun toCheckpointDeletedNotificationEvent(
        deletedId: UUID,
        companyId: UUID
    ): NotificationEvent.CheckpointDeleted =
        NotificationEvent.CheckpointDeleted(
            meta = NotificationEvent.Meta(companyId = companyId, Instant.now()),
            deletedId = deletedId
        )

}