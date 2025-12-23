package org.turter.wageapp.domain.shared

import java.util.UUID

interface CompanyBindData<T> {
    val companyId: UUID
    val data: T
}