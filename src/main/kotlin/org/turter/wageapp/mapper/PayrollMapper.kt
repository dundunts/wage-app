package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.shift.PaymentDbEntity
import org.turter.wageapp.domain.salary.Payroll

@Mapper(componentModel = "spring")
interface PayrollMapper {

    fun toEmployeeInfo(entity: EmployeeDbEntity): Payroll.EmployeeInfo

    fun toPayment(entity: PaymentDbEntity, employee: EmployeeDbEntity): Payroll.Payment

}