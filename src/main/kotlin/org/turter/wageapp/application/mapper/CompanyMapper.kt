package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.turter.wageapp.application.data.company.CompanyDbEntity
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload

@Mapper(componentModel = "spring")
interface CompanyMapper {

    @Mapping(target = "id", ignore = true)
    fun toNewCompanyDbEntity(payload: CompanyPayload): CompanyDbEntity

    @Mapping(target = "id", ignore = true)
    fun mergeToCompanyDbEntity(
        payload: CompanyPayload,
        @MappingTarget savedDbEntity: CompanyDbEntity
    ): CompanyDbEntity

    fun toCompanyDto(dbEntity: CompanyDbEntity): Company
}
