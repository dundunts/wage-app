package org.turter.wageapp.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import java.util.*

@Mapper(componentModel = "spring")
interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    fun toNewEmployeeDbEntity(payload: CreateEmployeePayload): EmployeeDbEntity

    @Mapping(target = "id", ignore = true)
    fun mergeToEmployeeDbEntity(
        payload: UpdateEmployeePayload,
        @MappingTarget entity: EmployeeDbEntity
    )

    @Mapping(target = "companyIds", source = "companyIds")
    fun toEmployee(
        entity: EmployeeDbEntity,
        companyIds: List<UUID>
    ): Employee
}
