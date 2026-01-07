package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.application.data.shift.PaymentDbEntity
import org.turter.wageapp.domain.salary.Payroll

@Mapper(componentModel = "spring")
interface PayrollMapper {

    fun toEmployeeInfo(entity: EmployeeDbEntity): Payroll.EmployeeInfo

    fun toPayment(entity: PaymentDbEntity, employee: EmployeeDbEntity): Payroll.Payment

}