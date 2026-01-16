package org.turter.wageapp.utils.draft

import org.turter.wageapp.application.data.shift.ShiftResultDraftDbEntity
import java.time.LocalDate
import java.util.*

object ShiftResultDraftEntityFactory {

    fun create(
        id: UUID? = null,
        sessionId: UUID,
        date: LocalDate = LocalDate.now(),
    ): ShiftResultDraftDbEntity {
        val draft = ShiftResultDraftDbEntity()
        draft.id = id
        draft.sessionId = sessionId
        draft.date = date
        return draft
    }
}
