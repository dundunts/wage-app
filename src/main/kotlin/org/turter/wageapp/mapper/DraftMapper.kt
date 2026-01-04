package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.PaymentDraftDbEntity
import org.turter.wageapp.data.shift.ShiftResultDraftDbEntity
import org.turter.wageapp.domain.calculator.PaymentDraftCalculation
import org.turter.wageapp.domain.shift.PaymentDraft
import org.turter.wageapp.domain.shift.ShiftResultDraft
import java.util.UUID

@Mapper(componentModel = "spring")
interface DraftMapper {

    fun toShiftResultDraft(entity: ShiftResultDraftDbEntity, payments: List<PaymentDraft>): ShiftResultDraft

    fun toPaymentDraft(entity: PaymentDraftDbEntity, employee: EmployeeDbEntity): PaymentDraft

    fun toEmployeeInfo(entity: EmployeeDbEntity): PaymentDraft.EmployeeInfo

    fun toPaymentDraftDbEntity(payment: PaymentDraftCalculation, shiftResultDraftId: UUID): PaymentDraftDbEntity

}