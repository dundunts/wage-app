package org.turter.wageapp.data.shift

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface ShiftResultDraftRepository : R2dbcRepository<ShiftResultDraftDbEntity, UUID> {

    fun existsBySessionId(sessionId: UUID): Mono<Boolean>

    fun findBySessionId(sessionId: UUID): Mono<ShiftResultDraftDbEntity>

}

@Repository
interface PaymentDraftRepository : R2dbcRepository<PaymentDraftDbEntity, UUID> {

    fun findAllByShiftResultDraftId(shiftResultDraftId: UUID): Flux<PaymentDraftDbEntity>

}