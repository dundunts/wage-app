package org.turter.wageapp.data.shift

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ShiftResultRepository : R2dbcRepository<ShiftResultDbEntity, UUID>

@Repository
interface PaymentRepository : R2dbcRepository<PaymentDbEntity, UUID>