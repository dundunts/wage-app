package org.turter.wageapp.data.shift

import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
interface ShiftResultRepository : R2dbcRepository<ShiftResultDbEntity, UUID> {

    fun findByCompanyIdAndDate(companyId: UUID, date: LocalDate): Mono<ShiftResultDbEntity>

    fun findAllByCompanyIdAndDateBetweenOrderByDate(
        companyId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Flux<ShiftResultDbEntity>

    fun findAllByCompanyIdAndDateBetween(
        companyId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Flux<ShiftResultDbEntity>

    fun findAllByCompanyIdOrderByDate(companyId: UUID, pageable: Pageable): Flux<ShiftResultDbEntity>

    fun findAllByCompanyIdAndDateBetweenOrderByDate(
        companyId: UUID,
        start: LocalDate,
        end: LocalDate,
        pageable: Pageable
    ): Flux<ShiftResultDbEntity>

    fun countAllByCompanyIdAndDateBetween(
        companyId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Mono<Long>

}

@Repository
interface PaymentRepository : R2dbcRepository<PaymentDbEntity, UUID> {

    fun findByShiftResultIdAndEmployeeId(shiftResultId: UUID, employeeId: UUID): Mono<PaymentDbEntity>

    fun findAllByShiftResultId(shiftResultId: UUID): Flux<PaymentDbEntity>

    fun findAllByShiftResultIdIn(shiftResultIds: List<UUID>): Flux<PaymentDbEntity>

    fun deleteAllByShiftResultId(shiftResultId: UUID): Mono<Void>

}