package org.turter.wageapp.application.mapper

import org.turter.wageapp.transport.model.EmployeePayrollSummary
import org.turter.wageapp.transport.model.PayrollAggregation
import org.turter.wageapp.transport.model.PayrollElement
import org.turter.wageapp.transport.model.PayrollEmployee
import org.turter.wageapp.transport.model.PayrollPayment
import org.turter.wageapp.domain.salary.Payroll as DomainPayroll
import org.turter.wageapp.transport.model.Payroll as TransportPayroll

fun DomainPayroll.toTransport() = TransportPayroll(
    type = PayrollAggregation.valueOf(type.name),
    elements = elements.map { element ->
        PayrollElement(
            date = element.date,
            payments = element.payments.map { payment ->
                PayrollPayment(
                    employee = payment.employee.toTransport(),
                    percentFromRevenue = payment.percentFromRevenue,
                    tips = payment.tips,
                )
            },
        )
    },
    summaries = summaries.map { summary ->
        EmployeePayrollSummary(
            employee = summary.employee.toTransport(),
            totalPercentFromRevenue = summary.totalPercentFromRevenue,
            totalTips = summary.totalTips,
        )
    },
)

private fun DomainPayroll.EmployeeInfo.toTransport() = PayrollEmployee(
    id = id,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    simpleName = simpleName,
)
