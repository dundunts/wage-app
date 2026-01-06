package org.turter.wageapp.data.shift

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.turter.wageapp.domain.shift.CalculationSource
import java.time.LocalDate
import java.util.UUID

@Table("wage_app.shift_results")
class ShiftResultDbEntity {

    @Id
    var id: UUID? = null

    @Column("company_id")
    var companyId: UUID? = null // not null

    @Column("date")
    var date: LocalDate? = null // not null

    @Column("session_id")
    var sessionId: UUID? = null

    @Column("calculation_source")
    var calculationSource: CalculationSource? = null // not null

    // unique(companyId, date)

}

@Table("wage_app.payments")
class PaymentDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_result_id")
    var shiftResultId: UUID? = null // not null

    @Column("employee_id")
    var employeeId: UUID? = null // not null

    @Column("percent_from_revenue")
    var percentFromRevenue: Int? = null // not null

    @Column("tips")
    var tips: Int? = null // not null

    @Column("work_seconds")
    var workSeconds: Long? = null // not null

    // unique(shiftResultId, employeeId)

}