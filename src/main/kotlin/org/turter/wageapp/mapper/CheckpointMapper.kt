package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.ShiftSessionCheckpointDbEntity
import org.turter.wageapp.data.shift.ShiftSessionCheckpointEmployeeDbEntity
import org.turter.wageapp.data.shift.ShiftSessionCheckpointMetricRecordDbEntity
import org.turter.wageapp.domain.shift.CheckpointMetricRecord
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.ShiftCheckpoint
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import java.util.*

@Mapper(componentModel = "spring")
interface CheckpointMapper {

    fun toShiftCheckpoint(
        entity: ShiftSessionCheckpointDbEntity,
        metricRecords: List<ShiftSessionCheckpointMetricRecordDbEntity>,
        employees: List<EmployeeDbEntity>
    ): ShiftCheckpoint

    fun toShiftCheckpointEmployeeInfo(entity: EmployeeDbEntity): ShiftCheckpoint.EmployeeInfo

    fun toShiftCheckpointEmployeeInfoList(entity: List<EmployeeDbEntity>): List<ShiftCheckpoint.EmployeeInfo>

    fun toNewCheckpointDbEntity(payload: ShiftCheckpointPayload, shiftSessionId: UUID): ShiftSessionCheckpointDbEntity

    @Mapping(target = "id", ignore = true)
    fun mergeToCheckpointDbEntity(payload: ShiftCheckpointPayload, @MappingTarget entity: ShiftSessionCheckpointDbEntity): ShiftSessionCheckpointDbEntity

    fun toNewCheckpointEmployeeDbEntity(shiftSessionCheckpointId: UUID, employeeId: UUID): ShiftSessionCheckpointEmployeeDbEntity

    fun toNewCheckpointEmployeeDbEntityList(shiftSessionCheckpointId: UUID, employeeId: List<UUID>): List<ShiftSessionCheckpointEmployeeDbEntity>

    fun toCheckpointMetricRecord(entity: ShiftSessionCheckpointMetricRecordDbEntity): CheckpointMetricRecord

    fun toCheckpointMetricRecordList(entity: List<ShiftSessionCheckpointMetricRecordDbEntity>): List<CheckpointMetricRecord>

    fun toNewCheckpointMetricRecord(payload: CheckpointMetricRecordPayload, shiftSessionCheckpointId: UUID): ShiftSessionCheckpointMetricRecordDbEntity

    fun toNewCheckpointMetricRecordList(payloads: List<CheckpointMetricRecordPayload>, shiftSessionCheckpointId: UUID): List<ShiftSessionCheckpointMetricRecordDbEntity>

}