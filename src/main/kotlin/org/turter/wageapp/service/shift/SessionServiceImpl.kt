package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.data.employee.EmployeeRepository
import org.turter.wageapp.data.shift.*
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.*
import org.turter.wageapp.mapper.CheckpointMapper
import org.turter.wageapp.mapper.SessionMapper
import reactor.core.publisher.Mono
import java.util.*

@Service
class SessionServiceImpl(
    private val sessionRepository: ShiftSessionRepository,
    private val checkpointRepository: CheckpointRepository,
    private val checkpointEmployeeRepository: CheckpointEmployeeRepository,
    private val metricRecordRepository: CheckpointMetricRecordRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository,
    private val checkpointMapper: CheckpointMapper,
    private val sessionMapper: SessionMapper
) : SessionService {
    override suspend fun getById(sessionId: UUID): ShiftSession {
        val session = sessionRepository.findById(sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {$sessionId}")

        return convertToShiftSession(session)
    }

    override suspend fun getOpenedSessionForCompany(
        companyId: UUID,
        userId: String
    ): ShiftSession {
        validateUserCompanyBind(userId, companyId, companyRepository)

        val openedSessions =
            sessionRepository.findAllByCompanyIdAndStatusIn(
                companyId,
                listOf(ShiftSession.Status.OPENED, ShiftSession.Status.OPENED_DRAFT)
            )
                .collectList()
                .awaitSingle()

        val session: ShiftSessionDbEntity = when (openedSessions.size) {
            1 -> openedSessions.first()
            0 -> throw EntityNotFoundException("Opened session not exists for company with id {$companyId}")
            else -> throw SeveralSessionsOpenedException("Several sessions opened for company. $openedSessions")
        }

        return convertToShiftSession(session)
    }

    override suspend fun getAllAvailableSessions(
        companyId: UUID,
        userId: String
    ): List<ShiftSession> {
        val sessions = sessionRepository.findAllByCompanyIdAndStatusIn(
            companyId,
            listOf(ShiftSession.Status.OPENED, ShiftSession.Status.RECALCULATING)
        )
            .collectList()
            .awaitSingle()

        return sessions.map { convertToShiftSession(it) }.toList()
    }

    @Transactional
    override suspend fun openNewSession(
        payload: OpenNewShiftSessionPayload,
        userId: String
    ): ShiftSession {
        validateUserCompanyBind(userId, payload.companyId, companyRepository)

        val openedSessions =
            sessionRepository.findAllByCompanyIdAndStatus(payload.companyId, ShiftSession.Status.OPENED)
                .collectList()
                .awaitSingle()

        if (openedSessions.isNotEmpty())
            throw SeveralSessionsOpenedException("Several sessions opened for company. $openedSessions")

        val session = sessionRepository
            .save(
                ShiftSessionDbEntity.getNewOpened(
                    payload.companyId,
                    payload.startWorkAt.toLocalTime(),
                    payload.startWorkAt.toLocalDate()
                )
            )
            .awaitSingle()

        return convertToShiftSession(session)
    }

    @Transactional
    override suspend fun openRecalculatingSession(
        payload: CreateRecalculatingShiftSessionPayload,
        userId: String
    ): ShiftSession {
        val session = sessionRepository.findByIdAndStatus(payload.closedSessionId, ShiftSession.Status.CLOSED)
            .awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session with status CLOSED not found for id: {${payload.closedSessionId}}")

        validateUserCompanyBind(userId, session.companyId!!, companyRepository)

        session.status = ShiftSession.Status.RECALCULATING

        return convertToShiftSession(sessionRepository.save(session).awaitSingle())
    }

    @Transactional
    override suspend fun closeSession(sessionId: UUID, userId: String) {
        val session = sessionRepository.findById(sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {$sessionId}")

        validateUserCompanyBind(userId, session.companyId!!, companyRepository)

        session.status = ShiftSession.Status.CLOSED

        sessionRepository.save(session).awaitSingle()
    }

    @Transactional
    override suspend fun updateStartWorkTime(
        payload: UpdateShiftSessionStartWorkTimePayload,
        userId: String
    ) {
        val session = sessionRepository.findById(payload.sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${payload.sessionId}}")

        validateUserCompanyBind(userId, session.companyId!!, companyRepository)

        session.startWorkTime = payload.startWorkTime

        sessionRepository.save(session).awaitSingle()
    }

    private suspend fun convertToShiftSession(session: ShiftSessionDbEntity): ShiftSession =
        sessionMapper.toShiftSession(session, getCheckpointsForSession(session.id!!))

    private suspend fun getCheckpointsForSession(id: UUID): List<Checkpoint> {
        return checkpointRepository.findAllByShiftSessionId(id)
            .flatMap { dbEntity ->
                Mono.zip(
                    Mono.just(dbEntity),
                    checkpointEmployeeRepository.findAllByCheckpointId(dbEntity.id!!)
                        .flatMap { bindInfo -> employeeRepository.findById(bindInfo.employeeId!!) }
                        .collectList(),
                    metricRecordRepository.findAllByCheckpointId(dbEntity.id!!)
                        .collectList()
                )
            }
            .map { tuple ->
                checkpointMapper.toShiftCheckpoint(tuple.t1, tuple.t3, tuple.t2)
            }
            .collectList()
            .awaitSingle()
    }
}