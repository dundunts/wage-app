package org.turter.wageapp.data.shift

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import org.turter.wageapp.domain.shift.CheckpointType
import org.turter.wageapp.domain.shift.ShiftSession
import java.time.LocalDateTime
import java.util.*

@Table("wage_app.shift_sessions")
class ShiftSessionDbEntity {

    @Id
    var id: UUID? = null

    @Column("company_id")
    var companyId: UUID? = null

    @Column("status")
    var status: ShiftSession.Status? = null

    constructor(companyId: UUID?, status: ShiftSession.Status?) {
        this.companyId = companyId
        this.status = status
    }

    constructor()

    companion object {

        fun getNewOpened(companyId: UUID): ShiftSessionDbEntity =
            ShiftSessionDbEntity(companyId, ShiftSession.Status.OPENED)

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

@Table("wage_app.shift_session_checkpoints")
class ShiftSessionCheckpointDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_session_id")
    var shiftSessionId: UUID? = null

    @Column("tips")
    var tips: Int = 0

    @Column("revenue")
    var revenue: Int = 0

    @Column("date_time")
    var dateTime: LocalDateTime? = null

    @Column("tips")
    var type: CheckpointType? = null

    @Column("creator_user_id")
    var creatorUserId: String? = null

    @Column("created_at")
    var createdAt: LocalDateTime? = null

    @Column("updater_user_id")
    var updaterUserId: String? = null

    @Column("update_at")
    var updatedAt: LocalDateTime? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShiftSessionCheckpointDbEntity

        if (tips != other.tips) return false
        if (revenue != other.revenue) return false
        if (id != other.id) return false
        if (shiftSessionId != other.shiftSessionId) return false
        if (dateTime != other.dateTime) return false
        if (type != other.type) return false
        if (creatorUserId != other.creatorUserId) return false
        if (createdAt != other.createdAt) return false
        if (updaterUserId != other.updaterUserId) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tips
        result = 31 * result + revenue
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (shiftSessionId?.hashCode() ?: 0)
        result = 31 * result + (dateTime?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (creatorUserId?.hashCode() ?: 0)
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (updaterUserId?.hashCode() ?: 0)
        result = 31 * result + (updatedAt?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointDbEntity(id=$id, shiftSessionId=$shiftSessionId, tips=$tips, revenue=$revenue, dateTime=$dateTime, type=$type, creatorUserId=$creatorUserId, createdAt=$createdAt, updaterUserId=$updaterUserId, updatedAt=$updatedAt)"
    }

}

@Table("wage_app.shift_session_checkpoints_employees")
class ShiftSessionCheckpointEmployeeDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_session_checkpoint_id")
    var shiftSessionCheckpointId: UUID? = null

    @Column("employee_id")
    var employeeId: UUID? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShiftSessionCheckpointEmployeeDbEntity

        if (id != other.id) return false
        if (shiftSessionCheckpointId != other.shiftSessionCheckpointId) return false
        if (employeeId != other.employeeId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (shiftSessionCheckpointId?.hashCode() ?: 0)
        result = 31 * result + (employeeId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointEmployeeDbEntity(id=$id, shiftSessionCheckpointId=$shiftSessionCheckpointId, employeeId=$employeeId)"
    }

}

@Table("wage_app.shift_session_checkpoint_metric_records")
class ShiftSessionCheckpointMetricRecordDbEntity {

    @Id
    var id: UUID? = null

    @Column("shift_session_checkpoint_id")
    var shiftSessionCheckpointId: UUID? = null

    @Column("label")
    var label: String? = null

    @Column("destination")
    var destination: CheckpointCalcDestination? = null

    @Column("value")
    var value: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShiftSessionCheckpointMetricRecordDbEntity

        if (value != other.value) return false
        if (id != other.id) return false
        if (shiftSessionCheckpointId != other.shiftSessionCheckpointId) return false
        if (label != other.label) return false
        if (destination != other.destination) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (shiftSessionCheckpointId?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (destination?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShiftSessionCheckpointFieldRecordDbEntity(id=$id, shiftSessionCheckpointId=$shiftSessionCheckpointId, label=$label, destination=$destination, value=$value)"
    }

}