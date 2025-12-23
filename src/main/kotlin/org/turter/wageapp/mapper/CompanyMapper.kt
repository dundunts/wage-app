package org.turter.wageapp.mapper

import org.mapstruct.*
import org.turter.wageapp.data.company.CompanyDbEntity
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload

@Mapper(componentModel = "spring")
interface CompanyMapper {

    fun toNewCompanyDbEntity(payload: CompanyPayload): CompanyDbEntity

    @Mapping(target = "id", ignore = true)
    fun mergeToCompanyDbEntity(
        payload: CompanyPayload,
        @MappingTarget savedDbEntity: CompanyDbEntity
    ): CompanyDbEntity

    fun toCompanyDto(dbEntity: CompanyDbEntity): Company
}
