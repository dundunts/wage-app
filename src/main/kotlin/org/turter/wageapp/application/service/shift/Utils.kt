package org.turter.wageapp.application.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.ShiftSessionClosedException
import org.turter.wageapp.domain.shift.ShiftSessionNotDraftException
import org.turter.wageapp.domain.shift.WrongCompanyIdException
import java.util.UUID

suspend fun validateUserCompanyBind(userId: String, companyId: UUID, repo: CompanyRepository) {
    val companies = repo.findAllForUserId(userId).collectList().awaitSingle()

    if (companies.none { c -> c.id == companyId })
        throw WrongCompanyIdException("Company ID {${companyId}} not included in user`s companies {$companies}.")
}

fun ShiftSessionDbEntity.validateSessionIsAvailableModifying() {
    when(status) {
        ShiftSession.Status.OPENED, ShiftSession.Status.RECALCULATING -> {}
        else -> throw ShiftSessionClosedException("Session with id {$id} is unable to modify")
    }
}

fun ShiftSessionDbEntity.validateSessionIsAvailableToConfirm() {
    when(status) {
        ShiftSession.Status.OPENED_DRAFT, ShiftSession.Status.RECALCULATING_DRAFT -> {}
        else -> throw ShiftSessionNotDraftException("Session with id {$id} is unable to confirm")
    }
}