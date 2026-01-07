package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.application.data.shift.PaymentDraftDbEntity
import org.turter.wageapp.application.data.shift.ShiftResultDraftDbEntity
import org.turter.wageapp.domain.calculator.PaymentDraftCalculation
import org.turter.wageapp.domain.shift.PaymentDraft
import org.turter.wageapp.domain.shift.ShiftResultDraft
import java.util.UUID

@Mapper(componentModel = "spring")
interface DraftMapper {

    fun toShiftResultDraft(entity: ShiftResultDraftDbEntity, payments: List<PaymentDraft>): ShiftResultDraft

    @Mapping(target = "id", source = "entity.id")
    fun toPaymentDraft(entity: PaymentDraftDbEntity, employee: EmployeeDbEntity): PaymentDraft

    fun toEmployeeInfo(entity: EmployeeDbEntity): PaymentDraft.EmployeeInfo

    @Mapping(target = "id", ignore = true)
    fun toNewPaymentDraftDbEntity(payment: PaymentDraftCalculation, shiftResultDraftId: UUID): PaymentDraftDbEntity

}