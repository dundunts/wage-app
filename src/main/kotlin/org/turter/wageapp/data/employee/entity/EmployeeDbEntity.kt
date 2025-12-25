package org.turter.wageapp.data.employee.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table(name = "wage_app.employees")
class EmployeeDbEntity {

    @Id
    @Column("id")
    var id: UUID? = null

    @Column("user_id")
    var userId: String? = null

    @Column("first_name")
    var firstName: String? = null

    @Column("last_name")
    var lastName: String? = null

    @Column("patronymic")
    var patronymic: String? = null

    @Column("simple_name")
    var simpleName: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmployeeDbEntity

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (patronymic != other.patronymic) return false
        if (simpleName != other.simpleName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        result = 31 * result + (patronymic?.hashCode() ?: 0)
        result = 31 * result + (simpleName?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "EmployeeDbEntity(id=$id, userId=$userId, firstName=$firstName, lastName=$lastName, patronymic=$patronymic, simpleName=$simpleName)"
    }

}