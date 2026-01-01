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
interface ShiftSessionCheckpointRepository : R2dbcRepository<ShiftSessionCheckpointDbEntity, UUID> {

    fun findAllByShiftSessionId(shiftSessionId: UUID): Flux<ShiftSessionCheckpointDbEntity>

}

@Repository
interface ShiftSessionCheckpointEmployeeRepository : R2dbcRepository<ShiftSessionCheckpointEmployeeDbEntity, UUID> {

    fun findAllByShiftSessionCheckpointId(shiftSessionCheckpointId: UUID): Flux<ShiftSessionCheckpointEmployeeDbEntity>

    fun deleteAllByShiftSessionCheckpointId(checkpointId: UUID): Mono<Void>

}

@Repository
interface ShiftSessionCheckpointMetricRecordRepository : R2dbcRepository<ShiftSessionCheckpointMetricRecordDbEntity, UUID> {

    fun findAllByShiftSessionCheckpointId(shiftSessionCheckpointId: UUID): Flux<ShiftSessionCheckpointMetricRecordDbEntity>

    fun deleteAllByShiftSessionCheckpointId(checkpointId: UUID): Mono<Void>

}