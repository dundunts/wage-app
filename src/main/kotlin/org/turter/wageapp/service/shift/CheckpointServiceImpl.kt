package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.data.employee.EmployeeRepository
import org.turter.wageapp.data.shift.ShiftSessionCheckpointEmployeeRepository
import org.turter.wageapp.data.shift.ShiftSessionCheckpointMetricRecordRepository
import org.turter.wageapp.data.shift.ShiftSessionCheckpointRepository
import org.turter.wageapp.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.data.shift.ShiftSessionRepository
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.CreateFirstShiftCheckpointPayload
import org.turter.wageapp.domain.shift.CreateRegularShiftCheckpointPayload
import org.turter.wageapp.domain.shift.SeveralSessionsOpenedException
import org.turter.wageapp.domain.shift.ShiftCheckpoint
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.ShiftSessionClosedException
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import org.turter.wageapp.domain.shift.WrongCompanyIdException
import org.turter.wageapp.mapper.CheckpointMapper
import org.turter.wageapp.mapper.SessionMapper
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class CheckpointServiceImpl(
    private val sessionRepository: ShiftSessionRepository,
    private val checkpointRepository: ShiftSessionCheckpointRepository,
    private val checkpointEmployeeRepository: ShiftSessionCheckpointEmployeeRepository,
    private val metricRecordRepository: ShiftSessionCheckpointMetricRecordRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository,
    private val checkpointMapper: CheckpointMapper,
    private val sessionMapper: SessionMapper
) : CheckpointService {

    @Transactional
    override suspend fun createFirstCheckpoint(payload: CreateFirstShiftCheckpointPayload, userId: String): ShiftSession {
        validateUserCompanyBind(userId, payload.companyId)

        // get or create opened session
        val openedSessions = sessionRepository.findAllByCompanyIdAndStatus(payload.companyId, ShiftSession.Status.OPENED)
            .collectList()
            .awaitSingle()

        val session: ShiftSessionDbEntity = when(openedSessions.size) {
            1 -> openedSessions.first()
            0 -> sessionRepository.save(ShiftSessionDbEntity.Companion.getNewOpened(payload.companyId)).awaitSingle()
            else -> throw SeveralSessionsOpenedException("Several sessions opened for company. $openedSessions")
        }

        // save checkpoint
        saveNewCheckpoint(payload, session.id!!)

        // get actual ShiftSession
        return convertToShiftSession(session)
    }

    @Transactional
    override suspend fun createCheckpoint(payload: CreateRegularShiftCheckpointPayload, userId: String): ShiftSession {
        val session = sessionRepository.findById(payload.sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${payload.sessionId}}")

        validateUserCompanyBind(userId, session.companyId!!)

        validateSession(session)

        saveNewCheckpoint(payload, session.id!!)

        return convertToShiftSession(session)
    }

    @Transactional
    override suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): ShiftSession {
        val checkpointFromDb = checkpointRepository.findById(payload.id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Checkpoint not found for id: {${payload.id}}")

        val session = sessionRepository.findById(checkpointFromDb.shiftSessionId!!).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${checkpointFromDb.shiftSessionId}}")

        validateUserCompanyBind(userId, session.companyId!!)

        validateSession(session)

        removeEmployeeBindsAndMetricsForCheckpoint(checkpointFromDb.id!!)

        saveEmployeeBindsAndMetricsForCheckpoint(checkpointFromDb.id!!, payload)

        checkpointRepository.save(checkpointMapper.mergeToCheckpointDbEntity(payload, checkpointFromDb))
            .awaitSingle()

        return convertToShiftSession(session)
    }

    private suspend fun convertToShiftSession(session: ShiftSessionDbEntity): ShiftSession =
        sessionMapper.toShiftSession(session, getCheckpointsForSession(session.id!!))

    private suspend fun removeEmployeeBindsAndMetricsForCheckpoint(checkpointId: UUID) {
        checkpointEmployeeRepository.deleteAllByShiftSessionCheckpointId(checkpointId)
            .awaitSingleOrNull()

        metricRecordRepository.deleteAllByShiftSessionCheckpointId(checkpointId)
            .awaitSingleOrNull()
    }

    private fun validateSession(session: ShiftSessionDbEntity) {
        if (session.status == ShiftSession.Status.CLOSED)
            throw ShiftSessionClosedException("Session with id {${session.id}} is closed - unable to modify checkpoint")
    }

    private suspend fun getCheckpointsForSession(id: UUID): List<ShiftCheckpoint> {
        return checkpointRepository.findAllByShiftSessionId(id)
            .flatMap { dbEntity ->
                Mono.zip(
                    Mono.just(dbEntity),
                    checkpointEmployeeRepository.findAllByShiftSessionCheckpointId(dbEntity.id!!)
                        .flatMap { bindInfo -> employeeRepository.findById(bindInfo.employeeId!!) }
                        .collectList(),
                    metricRecordRepository.findAllByShiftSessionCheckpointId(dbEntity.id!!)
                        .collectList()
                )
            }
            .map { tuple ->
                checkpointMapper.toShiftCheckpoint(tuple.t1, tuple.t3, tuple.t2)
            }
            .collectList()
            .awaitSingle()
    }

    private suspend fun saveNewCheckpoint(
        payload: ShiftCheckpointPayload,
        sessionId: UUID
    ) {
        val savedCheckpoint = checkpointRepository.save(
            checkpointMapper.toNewCheckpointDbEntity(payload, sessionId)
        ).awaitSingle()

        saveEmployeeBindsAndMetricsForCheckpoint(savedCheckpoint.id!!, payload)
    }

    private suspend fun saveEmployeeBindsAndMetricsForCheckpoint(
        checkpointId: UUID,
        payload: ShiftCheckpointPayload
    ) {
        checkpointEmployeeRepository.saveAll(
            checkpointMapper.toNewCheckpointEmployeeDbEntityList(checkpointId, payload.employeeIds)
        ).collectList().awaitSingle()

        metricRecordRepository.saveAll(
            checkpointMapper.toNewCheckpointMetricRecordList(payload.fieldRecords, checkpointId)
        ).collectList().awaitSingle()
    }

    private suspend fun validateUserCompanyBind(
        userId: String,
        companyId: UUID
    ) {
        val companies = companyRepository.findAllForUserId(userId).collectList().awaitSingle()

        if (companies.none { c -> c.id == companyId })
            throw WrongCompanyIdException("Company ID {${companyId}} not included in user`s companies {$companies}.")
    }

}