package org.turter.wageapp.data.shift

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("wage_app.shift_result_drafts")
class ShiftResultDraftDbEntity {

    @Id
    var id: UUID? = null

    @Column("session_id")
    var sessionId: UUID? = null // unique not null

    @Column("date")
    var date: LocalDate? = null // not null

}

@Table("wage_app.payment_drafts")
class PaymentDraftDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_result_draft_id")
    var shiftResultDraftId: UUID? = null // not null

    @Column("employee_id")
    var employeeId: UUID? = null // not null

    @Column("percent_from_revenue")
    var percentFromRevenue: Int? = null // not null

    @Column("tips")
    var tips: Int? = null // not null

    @Column("work_seconds")
    var workSeconds: Long = 0 // not null

    // unique(shiftResultDraftId, employeeId)

}