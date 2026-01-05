package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.PaymentDbEntity
import org.turter.wageapp.data.shift.ShiftResultDbEntity
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import org.turter.wageapp.domain.shift.ShiftResultFromDraft
import org.turter.wageapp.domain.shift.ShiftSession
import java.util.UUID

@Mapper(componentModel = "spring")
interface ShiftResultMapper {

    @Mapping(target = "id", source = "shiftResult.id")
    @Mapping(target = "date", source = "shiftResult.date")
    fun toDetailed(
        shiftResult: ShiftResultDbEntity,
        payments: List<ShiftResultDetailed.Payment>,
        session: ShiftSession?
    ): ShiftResultDetailed

    @Mapping(target = "id", source = "payment.id")
    fun toPayment(
        payment: PaymentDbEntity,
        employee: EmployeeDbEntity
    ): ShiftResultDetailed.Payment

    fun toPaymentEmployeeInfo(entity: EmployeeDbEntity): ShiftResultDetailed.Payment.EmployeeInfo

    @Mapping(target = "id", ignore = true)
    fun toNewShiftResultDbEntityFromDraft(shiftResult: ShiftResultFromDraft): ShiftResultDbEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", ignore = true)
    fun toNewShiftResultDbEntityFromPayload(payload: SaveShiftResultPayload): ShiftResultDbEntity

    @Mapping(target = "id", ignore = true)
    fun toNewPaymentDbEntityFromDraft(payment: ShiftResultFromDraft.Payment, shiftResultId: UUID): PaymentDbEntity

    @Mapping(target = "id", ignore = true)
    fun toNewPaymentDbEntityFromPayload(payment: SaveShiftResultPayload.PaymentPayload, shiftResultId: UUID): PaymentDbEntity

}