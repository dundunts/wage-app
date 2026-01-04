package org.turter.wageapp.service.shift

import org.turter.wageapp.domain.shift.ConfirmDraftResponse
import org.turter.wageapp.domain.shift.ShiftResultDraft
import java.util.UUID

interface CalculationService {

    suspend fun getOrCalculateDraft(sessionId: UUID, userId: String): ShiftResultDraft

    suspend fun confirmDraft(draftId: UUID, userId: String): ConfirmDraftResponse

    suspend fun deleteDraft(draftId: UUID, userId: String)

}
