package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.PaymentDbEntity
import org.turter.wageapp.data.shift.ShiftResultDbEntity
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import org.turter.wageapp.domain.shift.ShiftResultFromDraft
import java.util.UUID

@Mapper(componentModel = "spring")
interface ShiftResultMapper {

    fun toDetailed(
        shiftResult: ShiftResultDbEntity,
        payments: List<ShiftResultDetailed.Payment>
    ): ShiftResultDetailed

    fun toPayment(
        payment: PaymentDbEntity,
        employee: EmployeeDbEntity
    ): ShiftResultDetailed.Payment

    fun toPaymentEmployeeInfo(entity: EmployeeDbEntity): ShiftResultDetailed.Payment.EmployeeInfo

    fun toNewShiftResultDbEntityFromDraft(shiftResult: ShiftResultFromDraft): ShiftResultDbEntity

    fun toNewShiftResultDbEntityFromPayload(payload: SaveShiftResultPayload): ShiftResultDbEntity

    fun toNewPaymentDbEntityFromDraft(payment: ShiftResultFromDraft.Payment, shiftResultId: UUID): PaymentDbEntity

    fun toNewPaymentDbEntityFromPayload(payment: SaveShiftResultPayload.PaymentPayload, shiftResultId: UUID): PaymentDbEntity

}