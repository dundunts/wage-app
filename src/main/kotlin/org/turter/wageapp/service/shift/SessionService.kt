package org.turter.wageapp.service.shift

import org.turter.wageapp.domain.shift.CreateRecalculatingShiftSessionPayload
import org.turter.wageapp.domain.shift.OpenNewShiftSessionPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.UpdateShiftSessionStartWorkTimePayload
import java.util.UUID

interface SessionService {

    suspend fun getById(sessionId: UUID): ShiftSession

    suspend fun getOpenedSessionForCompany(companyId: UUID, userId: String): ShiftSession

    suspend fun getAllAvailableSessions(companyId: UUID, userId: String): List<ShiftSession>

    suspend fun openNewSession(payload: OpenNewShiftSessionPayload, userId: String): ShiftSession

    suspend fun openRecalculatingSession(payload: CreateRecalculatingShiftSessionPayload, userId: String): ShiftSession

    suspend fun closeSession(sessionId: UUID, userId: String)

    suspend fun updateStartWorkTime(payload: UpdateShiftSessionStartWorkTimePayload, userId: String)

}
