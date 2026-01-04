package org.turter.wageapp.data.company

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("wage_app.companies")
class CompanyDbEntity {

    @Id
    var id: UUID? = null

    @Column("title")
    var title: String = ""

    //TODO сделать отдельный класс для коэффициента и написать соответствующий r2dbc converter
    @Column("k_from_revenue")
    var employeeWageCoefficientFromRevenue: Int = 0

    @Column("default_shift_start_time")
    var defaultShiftStartTime: String = "00:00"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompanyDbEntity

        if (employeeWageCoefficientFromRevenue != other.employeeWageCoefficientFromRevenue) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (defaultShiftStartTime != other.defaultShiftStartTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = employeeWageCoefficientFromRevenue
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + defaultShiftStartTime.hashCode()
        return result
    }

    override fun toString(): String {
        return "CompanyDbEntity(id=$id, title='$title', employeeWageCoefficientFromRevenue=$employeeWageCoefficientFromRevenue, defaultShiftStartTime='$defaultShiftStartTime')"
    }

}

