package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.data.employee.EmployeeRepository
import org.turter.wageapp.data.shift.CheckpointEmployeeRepository
import org.turter.wageapp.data.shift.PaymentDraftRepository
import org.turter.wageapp.data.shift.ShiftResultDraftDbEntity
import org.turter.wageapp.data.shift.ShiftResultDraftRepository
import org.turter.wageapp.data.shift.CheckpointRepository
import org.turter.wageapp.data.shift.PaymentDraftDbEntity
import org.turter.wageapp.data.shift.PaymentRepository
import org.turter.wageapp.data.shift.ShiftResultRepository
import org.turter.wageapp.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.data.shift.ShiftSessionRepository
import org.turter.wageapp.domain.calculator.CoefficientFromRevenue
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shift.ConfirmDraftResponse
import org.turter.wageapp.domain.calculator.PaymentDraftCalculator
import org.turter.wageapp.domain.shift.PaymentDraft
import org.turter.wageapp.domain.shift.ShiftResultDraft
import org.turter.wageapp.domain.shift.ShiftResultFromDraft
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.mapper.CheckpointMapper
import org.turter.wageapp.mapper.DraftMapper
import org.turter.wageapp.mapper.ShiftResultMapper
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
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
    private val shiftResultMapper: ShiftResultMapper
) : CalculationService {
    override suspend fun getOrCalculateDraft(
        sessionId: UUID,
        userId: String
    ): ShiftResultDraft {
        val session = sessionRepository.findById(sessionId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Session not found for id: {$sessionId}")

        validateUserCompanyBind(userId, session.id!!, companyRepository)

        val isExists = draftRepository.existsBySessionId(sessionId).awaitSingle()

        return if (isExists)
            draftRepository.findBySessionId(sessionId).awaitSingle().convertToShiftResultDraft()
        else
            calculateAndSaveResultsDraft(sessionId, session)
    }

    override suspend fun confirmDraft(
        draftId: UUID,
        userId: String
    ): ConfirmDraftResponse {
        val draft = draftRepository.findById(draftId).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Draft not found for id: {$draftId}")

        val session = sessionRepository.findById(draft.sessionId!!).awaitSingle()

        validateUserCompanyBind(userId, session.id!!, companyRepository)

        session.validateSessionIsAvailableToConfirm()

        val shiftResult = ShiftResultFromDraft(draft.convertToShiftResultDraft(), session.companyId!!)

        val savedShiftResult = shiftResultRepository.save(shiftResultMapper.toShiftResultDbEntity(shiftResult))
            .awaitSingle()

        paymentRepository.saveAll(
            shiftResult.payments.map { payment ->
                shiftResultMapper.toPaymentDbEntity(payment, savedShiftResult.id!!)
            }
        ).collectList().awaitSingle()

        session.status = ShiftSession.Status.CLOSED

        sessionRepository.save(session).awaitSingle()

        return ConfirmDraftResponse(savedShiftResult.id!!)
    }

    suspend fun ShiftResultDraftDbEntity.convertToShiftResultDraft(): ShiftResultDraft {
        val payments = paymentDraftRepository.findAllByShiftResultDraftId(id!!)
            .toPaymentDraftList()

        return draftMapper.toShiftResultDraft(this, payments)
    }

    override suspend fun deleteDraft(draftId: UUID, userId: String) {
        val draft = draftRepository.findById(draftId).awaitSingleOrNull() ?: return

        val session = sessionRepository.findById(draft.sessionId!!).awaitSingle()

        validateUserCompanyBind(userId, session.id!!, companyRepository)

        session.status = session.status?.cancelDraft()

        sessionRepository.save(session).awaitSingle()

        draftRepository.deleteById(draftId).awaitSingleOrNull()
    }

    private suspend fun calculateAndSaveResultsDraft(
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
            draftMapper.toPaymentDraftDbEntity(payment, savedResultDraft.id!!)
        }

        val savedPayments = paymentDraftRepository.saveAll(dbEntities).toPaymentDraftList()

        session.status = session.status?.draft()

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