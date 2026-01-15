package org.turter.wageapp.application.data.shift

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import org.turter.wageapp.domain.shift.CheckpointType
import org.turter.wageapp.domain.shift.ShiftSession
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Table("wage_app.shift_sessions")
class ShiftSessionDbEntity {

    @Id
    var id: UUID? = null

    @Column("company_id")
    lateinit var companyId: UUID // not null

    @Column("status")
    lateinit var status: ShiftSession.Status // not null

    @Column("start_work_time")
    lateinit var startWorkTime: LocalTime // not null

    @Column("date")
    lateinit var date: LocalDate // not null

    constructor(companyId: UUID, status: ShiftSession.Status, startWorkTime: LocalTime, date: LocalDate) {
        this.companyId = companyId
        this.status = status
        this.startWorkTime = startWorkTime
        this.date = date
    }

    constructor()

    companion object {

        fun getNewOpened(companyId: UUID, startWorkTime: LocalTime, date: LocalDate): ShiftSessionDbEntity =
            ShiftSessionDbEntity(companyId, ShiftSession.Status.OPENED, startWorkTime, date)

        fun getRecalculating(companyId: UUID, startWorkTime: LocalTime, date: LocalDate): ShiftSessionDbEntity =
            ShiftSessionDbEntity(companyId, ShiftSession.Status.RECALCULATING, startWorkTime, date)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShiftSessionDbEntity

        if (id != other.id) return false
        if (companyId != other.companyId) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (companyId?.hashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionDbEntity(id=$id, companyId=$companyId, status=$status)"
    }

}

@Table("wage_app.checkpoints")
class CheckpointDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_session_id")
    var shiftSessionId: UUID? = null // not null

    @Column("tips")
    var tips: Int = 0 // not null

    @Column("revenue")
    var revenue: Int = 0 // not null

    @Column("date_time")
    var dateTime: LocalDateTime? = null // not null

    @Column("type")
    var type: CheckpointType? = null // not null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CheckpointDbEntity

        if (tips != other.tips) return false
        if (revenue != other.revenue) return false
        if (id != other.id) return false
        if (shiftSessionId != other.shiftSessionId) return false
        if (dateTime != other.dateTime) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tips
        result = 31 * result + revenue
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (shiftSessionId?.hashCode() ?: 0)
        result = 31 * result + (dateTime?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointDbEntity(id=$id, shiftSessionId=$shiftSessionId, tips=$tips, revenue=$revenue, dateTime=$dateTime, type=$type)"
    }

}

@Table("wage_app.checkpoints_employees")
class CheckpointEmployeeDbEntity {

    @Id
    var id: UUID? = null

    @Column("checkpoint_id")
    var checkpointId: UUID? = null // not null

    @Column("employee_id")
    var employeeId: UUID? = null // not null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CheckpointEmployeeDbEntity

        if (id != other.id) return false
        if (checkpointId != other.checkpointId) return false
        if (employeeId != other.employeeId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (checkpointId?.hashCode() ?: 0)
        result = 31 * result + (employeeId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointEmployeeDbEntity(id=$id, shiftSessionCheckpointId=$checkpointId, employeeId=$employeeId)"
    }

    // unique(checkpointId,employeeId)

}

@Table("wage_app.checkpoint_metric_records")
class CheckpointMetricRecordDbEntity {

    @Id
    var id: UUID? = null

    @Column("checkpoint_id")
    var checkpointId: UUID? = null // not null

    @Column("label")
    var label: String? = null // not null

    @Column("destination")
    var destination: CheckpointCalcDestination? = null // not null

    @Column("value")
    var value: Int = 0 // not null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CheckpointMetricRecordDbEntity

        if (value != other.value) return false
        if (id != other.id) return false
        if (checkpointId != other.checkpointId) return false
        if (label != other.label) return false
        if (destination != other.destination) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (checkpointId?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (destination?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointFieldRecordDbEntity(id=$id, shiftSessionCheckpointId=$checkpointId, label=$label, destination=$destination, value=$value)"
    }

}