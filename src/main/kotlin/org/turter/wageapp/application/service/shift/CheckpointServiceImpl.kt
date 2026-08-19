package org.turter.wageapp.application.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.shift.*
import org.turter.wageapp.application.mapper.CheckpointMapper
import org.turter.wageapp.application.service.ReferenceValidator
import org.turter.wageapp.application.service.mapInvalidReference
import org.turter.wageapp.application.service.missingReferencesDetail
import org.turter.wageapp.domain.notification.NotificationEventPublisher
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.ShiftCheckpointPayload
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import java.util.*

@Service
class CheckpointServiceImpl(
    private val sessionRepository: ShiftSessionRepository,
    private val checkpointRepository: CheckpointRepository,
    private val checkpointEmployeeRepository: CheckpointEmployeeRepository,
    private val metricRecordRepository: CheckpointMetricRecordRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyRepository: CompanyRepository,
    private val checkpointMapper: CheckpointMapper,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val referenceValidator: ReferenceValidator
) : CheckpointService {

    @Transactional
    override suspend fun createCheckpoint(payload: CreateRegularCheckpointPayload, userId: String): Checkpoint {
        val session = sessionRepository.findById(payload.sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${payload.sessionId}}")

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        session.validateSessionIsAvailableModifying()
        referenceValidator.requireEmployees(payload.employeeIds)

        val checkpoint = saveCheckpoint(
            checkpointMapper.toNewCheckpointDbEntity(payload, session.id!!),
            payload
        )

        if (session.status == ShiftSession.Status.OPENED) {
            notificationEventPublisher.publish(
                checkpointMapper.toCheckpointSavedNotificationEvent(
                    checkpoint,
                    session.companyId
                )
            )
        }

        return checkpoint
    }

    @Transactional
    override suspend fun updateCheckpoint(payload: UpdateShiftCheckpointPayload, userId: String): Checkpoint {
        val checkpointFromDb = checkpointRepository.findById(payload.id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Checkpoint not found for id: {${payload.id}}")

        val session = sessionRepository.findById(checkpointFromDb.shiftSessionId!!).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${checkpointFromDb.shiftSessionId}}")

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        session.validateSessionIsAvailableModifying()
        referenceValidator.requireEmployees(payload.employeeIds)

        removeEmployeeBindsAndMetricsForCheckpoint(checkpointFromDb.id!!)

        val checkpoint = saveCheckpoint(checkpointMapper.mergeToCheckpointDbEntity(payload, checkpointFromDb), payload)

        notificationEventPublisher.publish(
            checkpointMapper.toCheckpointReplacedNotificationEvent(
                checkpoint,
                checkpointFromDb.id!!,
                session.companyId
            )
        )

        return checkpoint
    }

    override suspend fun deleteById(checkpointId: UUID, userId: String) {
        val checkpointFromDb = checkpointRepository.findById(checkpointId).awaitSingleOrNull()
            ?: return

        val session = sessionRepository.findById(checkpointFromDb.shiftSessionId!!).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {${checkpointFromDb.shiftSessionId}}")

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        session.validateSessionIsAvailableModifying()

        checkpointRepository.deleteById(checkpointId).awaitSingleOrNull()

        notificationEventPublisher.publish(
            checkpointMapper.toCheckpointDeletedNotificationEvent(
                checkpointId,
                session.companyId
            )
        )
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
        val employeeBinds = mapInvalidReference(
            exception = { e ->
                EntityNotFoundException(
                    missingReferencesDetail("Referenced Employees", payload.employeeIds),
                    e
                )
            }
        ) {
            checkpointEmployeeRepository.saveAll(
                checkpointMapper.toNewCheckpointEmployeeDbEntityList(checkpointId, payload.employeeIds.toList())
            ).collectList().awaitSingle()
        }

        val metricRecords = metricRecordRepository.saveAll(
            checkpointMapper.toNewCheckpointMetricRecordList(payload.fieldRecords, checkpointId)
        ).collectList().awaitSingle()

        return Pair(employeeBinds, metricRecords)
    }

}
