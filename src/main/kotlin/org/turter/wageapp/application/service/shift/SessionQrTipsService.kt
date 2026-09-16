package org.turter.wageapp.application.service.shift

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.shift.ShiftSessionRepository
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.tips.TipsBotClient
import java.time.Duration
import java.time.ZoneId
import java.util.UUID

@Service
class SessionQrTipsService(
    private val sessionRepository: ShiftSessionRepository,
    private val companyRepository: CompanyRepository,
    private val tipsBotClient: TipsBotClient,
) {
    suspend fun getTipsInRubles(sessionId: UUID, userId: String): Long {
        val session = sessionRepository.findById(sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {$sessionId}")
        validateUserCompanyBind(userId, session.companyId, companyRepository)

        val startTime = session.date.atTime(session.startWorkTime)
            .atZone(ZoneId.of("Europe/Moscow"))
            .toInstant()
        return tipsBotClient.getTipsInKopecks(session.companyId, startTime, Duration.ofHours(24)) / 100
    }
}
