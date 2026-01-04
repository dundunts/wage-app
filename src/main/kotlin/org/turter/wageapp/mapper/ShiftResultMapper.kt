package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.data.shift.PaymentDbEntity
import org.turter.wageapp.data.shift.ShiftResultDbEntity
import org.turter.wageapp.domain.shift.ShiftResultFromDraft
import java.util.UUID

@Mapper(componentModel = "spring")
interface ShiftResultMapper {

    fun toShiftResultDbEntity(shiftResult: ShiftResultFromDraft): ShiftResultDbEntity

    fun toPaymentDbEntity(payment: ShiftResultFromDraft.Payment, shiftResultId: UUID): PaymentDbEntity

}