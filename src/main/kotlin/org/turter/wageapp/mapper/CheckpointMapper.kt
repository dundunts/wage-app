package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.CheckpointDbEntity
import org.turter.wageapp.data.shift.CheckpointEmployeeDbEntity
import org.turter.wageapp.data.shift.CheckpointMetricRecordDbEntity
import org.turter.wageapp.domain.shift.CheckpointMetricRecord
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import java.util.*

@Mapper(componentModel = "spring")
interface CheckpointMapper {

    fun toShiftCheckpoint(
        entity: CheckpointDbEntity,
        metricRecords: List<CheckpointMetricRecordDbEntity>,
        employees: List<EmployeeDbEntity>
    ): Checkpoint

    fun toShiftCheckpointEmployeeInfo(entity: EmployeeDbEntity): Checkpoint.EmployeeInfo

    fun toShiftCheckpointEmployeeInfoList(entity: List<EmployeeDbEntity>): List<Checkpoint.EmployeeInfo>

    fun toNewCheckpointDbEntity(payload: ShiftCheckpointPayload, shiftSessionId: UUID): CheckpointDbEntity

    @Mapping(target = "id", ignore = true)
    fun mergeToCheckpointDbEntity(payload: ShiftCheckpointPayload, @MappingTarget entity: CheckpointDbEntity): CheckpointDbEntity

    fun toNewCheckpointEmployeeDbEntity(shiftSessionCheckpointId: UUID, employeeId: UUID): CheckpointEmployeeDbEntity

    fun toNewCheckpointEmployeeDbEntityList(shiftSessionCheckpointId: UUID, employeeId: List<UUID>): List<CheckpointEmployeeDbEntity>

    fun toCheckpointMetricRecord(entity: CheckpointMetricRecordDbEntity): CheckpointMetricRecord

    fun toCheckpointMetricRecordList(entity: List<CheckpointMetricRecordDbEntity>): List<CheckpointMetricRecord>

    fun toNewCheckpointMetricRecord(payload: CheckpointMetricRecordPayload, shiftSessionCheckpointId: UUID): CheckpointMetricRecordDbEntity

    fun toNewCheckpointMetricRecordList(payloads: List<CheckpointMetricRecordPayload>, shiftSessionCheckpointId: UUID): List<CheckpointMetricRecordDbEntity>

}