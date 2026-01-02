package org.turter.wageapp.data.shift

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import org.turter.wageapp.domain.shift.ShiftSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface ShiftSessionRepository : R2dbcRepository<ShiftSessionDbEntity, UUID> {

    fun findAllByCompanyIdAndStatus(
        companyId: UUID,
        status: ShiftSession.Status
    ): Flux<ShiftSessionDbEntity>

}

@Repository
interface ShiftSessionCheckpointRepository : R2dbcRepository<CheckpointDbEntity, UUID> {

    fun findAllByShiftSessionId(shiftSessionId: UUID): Flux<CheckpointDbEntity>

}

@Repository
interface ShiftSessionCheckpointEmployeeRepository : R2dbcRepository<CheckpointEmployeeDbEntity, UUID> {

    fun findAllByCheckpointId(shiftSessionCheckpointId: UUID): Flux<CheckpointEmployeeDbEntity>

    fun deleteAllByCheckpointId(checkpointId: UUID): Mono<Void>

}

@Repository
interface ShiftSessionCheckpointMetricRecordRepository : R2dbcRepository<CheckpointMetricRecordDbEntity, UUID> {

    fun findAllByCheckpointId(shiftSessionCheckpointId: UUID): Flux<CheckpointMetricRecordDbEntity>

    fun deleteAllByCheckpointId(checkpointId: UUID): Mono<Void>

}