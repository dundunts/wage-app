package org.turter.wageapp.application.service.shift

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.data.employee.EmployeeRepository
import org.turter.wageapp.application.data.shift.*
import org.turter.wageapp.application.mapper.CheckpointMapper
import org.turter.wageapp.application.mapper.DraftMapper
import org.turter.wageapp.application.mapper.ShiftResultMapper
import org.turter.wageapp.domain.calculator.CoefficientFromRevenue
import org.turter.wageapp.domain.calculator.PaymentDraftCalculator
import org.turter.wageapp.domain.notification.NotificationEventPublisher
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@Service
class CalculationServiceImpl(
    private val companyRepository: CompanyRepository,
    private val draftRepository: ShiftResultDraftRepository,
    private val paymentDraftRepository: PaymentDraftRepository,
    private val sessionRepository: ShiftSessionRepository,
    private val employeeRepository: EmployeeRepository,
    private val checkpointRepository: CheckpointRepository,
    private val checkpointEmployeeRepository: CheckpointEmployeeRepository,
    private val shiftResultRepository: ShiftResultRepository,
    private val paymentRepository: PaymentRepository,
    private val draftMapper: DraftMapper,
    private val checkpointMapper: CheckpointMapper,
    private val shiftResultMapper: ShiftResultMapper,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val shiftResultRepositoryDecorator: ShiftResultRepositoryDecorator
) : CalculationService {

    //TODO move DB logic to repository
    override suspend fun getOrCalculateDraft(
        sessionId: UUID,
        userId: String
    ): ShiftResultDraft {
        val session = sessionRepository.findById(sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {$sessionId}")

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        if (!session.status.isDraftAvailableOrPresent())
            throw ShiftSessionClosedException("Session with id {$sessionId} is unable to modify")

        return try {
            calculateAndSaveResultsDraft(sessionId, session)
        } catch (e: DuplicateKeyException) {
            draftRepository.findBySessionId(sessionId)
                .awaitSingle()
                .convertToShiftResultDraft()
        }
    }

    @Transactional
    override suspend fun confirmDraft(
        draftId: UUID,
        userId: String
    ): ConfirmDraftResponse {
        val draft = draftRepository.findById(draftId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Draft not found for id: {$draftId}")

        val session = sessionRepository.findById(draft.sessionId!!).awaitSingle()

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        session.validateSessionIsAvailableToConfirm()

        val shiftResultFromDraft = ShiftResultFromDraft(draft.convertToShiftResultDraft(), session.companyId)

        val savedShiftResult =
            shiftResultRepository.save(shiftResultMapper.toNewShiftResultDbEntityFromDraft(shiftResultFromDraft))
                .awaitSingle()

        paymentRepository.saveAll(
            shiftResultFromDraft.payments.map { payment ->
                shiftResultMapper.toNewPaymentDbEntityFromDraft(payment, savedShiftResult.id!!)
            }
        ).collectList().awaitSingle()

        session.status = ShiftSession.Status.CLOSED

        sessionRepository.save(session).awaitSingle()

        val shiftResult = shiftResultRepositoryDecorator.findDetailedById(savedShiftResult.id!!)
            ?: throw EntityNotFoundException("Shift result not found for id: {${savedShiftResult.id!!}}")

        notificationEventPublisher.publish(
            shiftResultMapper.toShiftResultCreatedNotificationEvent(
                session.companyId,
                shiftResult
            )
        )

        draftRepository.deleteById(draftId).awaitSingleOrNull()

        return ConfirmDraftResponse(savedShiftResult.id!!)
    }

    @Transactional
    override suspend fun deleteDraft(draftId: UUID, userId: String) {
        val draft = draftRepository.findById(draftId).awaitSingleOrNull() ?: return

        val session = sessionRepository.findById(draft.sessionId!!).awaitSingle()

        validateUserCompanyBind(userId, session.companyId, companyRepository)

        session.status = session.status.cancelDraft()

        sessionRepository.save(session).awaitSingle()

        draftRepository.deleteById(draftId).awaitSingleOrNull()
    }

    private suspend fun ShiftResultDraftDbEntity.convertToShiftResultDraft(): ShiftResultDraft {
        val payments = paymentDraftRepository.findAllByShiftResultDraftId(id!!)
            .toPaymentDraftList()

        return draftMapper.toShiftResultDraft(this, payments)
    }

    @Transactional
    suspend fun calculateAndSaveResultsDraft(
        sessionId: UUID,
        session: ShiftSessionDbEntity
    ): ShiftResultDraft {
        val checkpoints = checkpointRepository.findAllByShiftSessionIdOrderByDateTimeAsc(sessionId)
            .flatMap { checkpoint ->
                checkpointEmployeeRepository.findAllByCheckpointId(checkpoint.id!!)
                    .collectList()
                    .map { employeeBinds ->
                        checkpointMapper.toCheckpointInfo(
                            checkpoint,
                            employeeBinds.mapNotNull { it.employeeId }
                        )
                    }
            }
            .collectList()
            .awaitSingle()

        val company = companyRepository.findById(session.companyId!!).awaitSingle()

        val calculator = PaymentDraftCalculator(
            checkpoints,
            CoefficientFromRevenue(company.employeeWageCoefficientFromRevenue),
            LocalDateTime.of(session.date, session.startWorkTime)
        )

        val savedResultDraft = draftRepository.save(ShiftResultDraftDbEntity().apply {
            this@apply.sessionId = sessionId
            date = session.date
        }).awaitSingle()

        val dbEntities = calculator.calculate().values.map { payment ->
            draftMapper.toNewPaymentDraftDbEntity(payment, savedResultDraft.id!!)
        }

        val savedPayments = paymentDraftRepository.saveAll(dbEntities).toPaymentDraftList()

        session.status = session.status.draft()

        sessionRepository.save(session).awaitSingle()

        return draftMapper.toShiftResultDraft(savedResultDraft, savedPayments)
    }

    private suspend fun Flux<PaymentDraftDbEntity>.toPaymentDraftList(): List<PaymentDraft> =
        this.flatMap { paymentDraftDbEntity ->
            employeeRepository.findById(paymentDraftDbEntity.employeeId!!)
                .zipWith(Mono.just(paymentDraftDbEntity))
        }
            .map { tuple -> draftMapper.toPaymentDraft(tuple.t2, tuple.t1) }
            .collectList()
            .awaitSingle()
}