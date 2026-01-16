package org.turter.wageapp.utils.draft

import org.turter.wageapp.domain.shift.PaymentDraft
import org.turter.wageapp.domain.shift.ShiftResultDraft
import java.time.LocalDate
import java.util.UUID

object ShiftResultDraftDtoSupplier {

    fun create(
        id: UUID = UUID.randomUUID(),
        sessionId: UUID,
        date: LocalDate = LocalDate.now(),
        payments: List<PaymentDraft> = emptyList()
    ): ShiftResultDraft {
        return ShiftResultDraft(
            id = id,
            sessionId = sessionId,
            date = date,
            payments = payments
        )
    }
}

