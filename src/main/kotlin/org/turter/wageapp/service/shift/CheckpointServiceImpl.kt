package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.data.employee.EmployeeRepository
import org.turter.wageapp.data.shift.CheckpointDbEntity
import org.turter.wageapp.data.shift.CheckpointEmployeeDbEntity
import org.turter.wageapp.data.shift.CheckpointMetricRecordDbEntity
import org.turter.wageapp.data.shift.CheckpointEmployeeRepository
import org.turter.wageapp.data.shift.CheckpointMetricRecordRepository
import org.turter.wageapp.data.shift.CheckpointRepository
import org.turter.wageapp.data.shift.ShiftSessionRepository
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import org.turter.wageapp.mapper.CheckpointMapper
import org.turter.wageapp.mapper.SessionMapper
import java.util.UUID

@Service
class CheckpointServiceImpl(
    private val sessionRepository: ShiftSessionRepository,
    private val checkpointRepository: CheckpointRepository,
    private val checkpointEmployeeRepository: CheckpointEmployeeRepository,
    private val metricRecordRepository: CheckpointMetricRecordRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository,
    private val checkpointMapper: CheckpointMapper,
    private val sessionMapper: SessionMapper
) : CheckpointService {

//    @Transactional
//    override suspend fun createFirstCheckpoint(payload: CreateFirstShiftCheckpointPayload, userId: String): Checkpoint {
//        validateUserCompanyBind(userId, payload.companyId)
//
//        // get or create opened session
//        val openedSessions = sessionRepository.findAllByCompanyIdAndStatus(payload.companyId, ShiftSession.Status.OPENED)
//            .collectList()
//            .awaitSingle()
//
//        val session: ShiftSessionDbEntity = when(openedSessions.size) {
//            1 -> openedSessions.first()
//            0 -> sessionRepository.save(ShiftSessionDbEntity.Companion.getNewOpened(payload.companyId)).awaitSingle()
//            else -> throw SeveralSessionsOpenedException("Several sessions opened for company. $openedSessions")
//        }
//
//        // save checkpoint
//        saveNewCheckpoint(payload, session.id!!)
//
//        // get actual ShiftSession
//        return convertToShiftSession(session)
//    }

    @Transactional
    override suspend fun createCheckpoint(payload: CreateRegularCheckpointPayload, userId: String): Checkpoint {
        val session = sessionRepository.findById(payload.sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${payload.sessionId}}")

        validateUserCompanyBind(userId, session.companyId!!, companyRepository)

        session.validateSessionIsAvailableModifying()

        return saveCheckpoint(
            checkpointMapper.toNewCheckpointDbEntity(payload, session.id!!),
            payload
        )
    }

    @Transactional
    override suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): Checkpoint {
        val checkpointFromDb = checkpointRepository.findById(payload.id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Checkpoint not found for id: {${payload.id}}")

        val session = sessionRepository.findById(checkpointFromDb.shiftSessionId!!).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${checkpointFromDb.shiftSessionId}}")

        validateUserCompanyBind(userId, session.companyId!!, companyRepository)

        session.validateSessionIsAvailableModifying()

        removeEmployeeBindsAndMetricsForCheckpoint(checkpointFromDb.id!!)

        return saveCheckpoint(checkpointMapper.mergeToCheckpointDbEntity(payload, checkpointFromDb), payload)
    }

    private suspend fun removeEmployeeBindsAndMetricsForCheckpoint(checkpointId: UUID) {
        checkpointEmployeeRepository.deleteAllByCheckpointId(checkpointId)
            .awaitSingleOrNull()

        metricRecordRepository.deleteAllByCheckpointId(checkpointId)
            .awaitSingleOrNull()
    }

    private suspend fun saveCheckpoint(
        entity: CheckpointDbEntity,
        payload: ShiftCheckpointPayload
    ): Checkpoint {
        val savedCheckpoint = checkpointRepository.save(entity).awaitSingle()

        val pair = saveEmployeeBindsAndMetricsForCheckpoint(savedCheckpoint.id!!, payload)

        val employees = employeeRepository.findAllById(pair.first.mapNotNull { bind -> bind.employeeId })
            .collectList()
            .awaitSingle()

        return checkpointMapper.toShiftCheckpoint(savedCheckpoint, pair.second, employees)
    }

    private suspend fun saveEmployeeBindsAndMetricsForCheckpoint(
        checkpointId: UUID,
        payload: ShiftCheckpointPayload
    ): Pair<List<CheckpointEmployeeDbEntity>, List<CheckpointMetricRecordDbEntity>> {
        val employeeBinds = checkpointEmployeeRepository.saveAll(
            checkpointMapper.toNewCheckpointEmployeeDbEntityList(checkpointId, payload.employeeIds)
        ).collectList().awaitSingle()

        val metricRecords = metricRecordRepository.saveAll(
            checkpointMapper.toNewCheckpointMetricRecordList(payload.fieldRecords, checkpointId)
        ).collectList().awaitSingle()

        return Pair(employeeBinds, metricRecords)
    }

}