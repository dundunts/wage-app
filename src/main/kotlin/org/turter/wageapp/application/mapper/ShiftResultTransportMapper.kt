package org.turter.wageapp.application.mapper

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import org.turter.wageapp.domain.shift.CheckpointMetricRecord
import org.turter.wageapp.domain.shift.CheckpointType
import org.turter.wageapp.domain.shift.ConfirmDraftResponse
import org.turter.wageapp.domain.shift.PaymentDraft
import org.turter.wageapp.domain.shift.SaveShiftResultPayload
import org.turter.wageapp.domain.shift.SaveShiftResultResponse
import org.turter.wageapp.domain.shift.ShiftResultDetailed
import org.turter.wageapp.domain.shift.ShiftSession
import org.turter.wageapp.transport.model.CalculationSource
import org.turter.wageapp.transport.model.CheckpointEmployee
import org.turter.wageapp.transport.model.CheckpointMetricDestination
import org.turter.wageapp.transport.model.FinalCheckpoint
import org.turter.wageapp.transport.model.ManualOverridePaymentRequest
import org.turter.wageapp.transport.model.Payment
import org.turter.wageapp.transport.model.PaymentEmployee
import org.turter.wageapp.transport.model.RegularCheckpoint
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultRequest
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultResponse
import org.turter.wageapp.transport.model.ShiftResult
import org.turter.wageapp.transport.model.ShiftResultDraft
import org.turter.wageapp.transport.model.ShiftResultDraftEmployee
import org.turter.wageapp.transport.model.ShiftResultDraftPayment
import org.turter.wageapp.transport.model.ShiftResultPage
import org.turter.wageapp.transport.model.ShiftResultResponse
import org.turter.wageapp.transport.model.ShiftSessionStatus
import org.turter.wageapp.transport.model.Checkpoint as TransportCheckpoint
import org.turter.wageapp.transport.model.CheckpointMetricRecord as TransportCheckpointMetricRecord
import org.turter.wageapp.transport.model.ConfirmShiftResultDraftResponse as TransportConfirmDraftResponse
import org.turter.wageapp.transport.model.ShiftSession as TransportShiftSession

@Component
class ShiftResultTransportMapper {

    fun toTransport(draft: org.turter.wageapp.domain.shift.ShiftResultDraft): ShiftResultDraft = ShiftResultDraft(
        id = draft.id,
        payments = draft.payments.map(::toTransport),
        date = draft.date,
        sessionId = draft.sessionId,
    )

    fun toTransport(response: ConfirmDraftResponse): TransportConfirmDraftResponse =
        TransportConfirmDraftResponse(resultId = response.resultId)

    fun toTransport(result: ShiftResultDetailed, session: ShiftSession?): ShiftResultResponse = ShiftResultResponse(
        shiftResult = toTransport(result),
        session = session?.let(::toTransport),
    )

    fun toTransport(page: Page<ShiftResultDetailed>): ShiftResultPage {
        val result = ShiftResultPage(
            content = page.content.map(::toTransport),
            number = page.number,
            propertySize = page.size,
            totalElements = page.totalElements,
        )

        result["content"] = result.content
        result["number"] = result.number
        result["size"] = result.propertySize
        result["totalElements"] = result.totalElements
        result["pageable"] = page.pageable
        result["last"] = page.isLast
        result["totalPages"] = page.totalPages
        result["first"] = page.isFirst
        result["numberOfElements"] = page.numberOfElements
        result["sort"] = page.sort
        result["empty"] = page.isEmpty
        return result
    }

    fun toDomain(request: SaveManualOverrideShiftResultRequest): SaveShiftResultPayload = SaveShiftResultPayload(
        replacementId = request.replacementId,
        companyId = request.companyId,
        overwrite = request.overwrite ?: false,
        payments = request.payments.map(::toDomain),
        date = request.date,
    )

    fun toTransport(response: SaveShiftResultResponse): SaveManualOverrideShiftResultResponse =
        SaveManualOverrideShiftResultResponse(resultId = response.resultId)

    private fun toTransport(payment: PaymentDraft): ShiftResultDraftPayment = ShiftResultDraftPayment(
        id = payment.id,
        employee = ShiftResultDraftEmployee(
            id = payment.employee.id,
            firstName = payment.employee.firstName,
            lastName = payment.employee.lastName,
            patronymic = payment.employee.patronymic,
            simpleName = payment.employee.simpleName,
        ),
        percentFromRevenue = payment.percentFromRevenue,
        tips = payment.tips,
        workSeconds = payment.workSeconds,
    )

    private fun toTransport(result: ShiftResultDetailed): ShiftResult = ShiftResult(
        id = result.id,
        payments = result.payments.map(::toTransport),
        date = result.date,
        sessionId = result.sessionId,
        calculationSource = CalculationSource.valueOf(result.calculationSource.name),
    )

    private fun toTransport(payment: ShiftResultDetailed.Payment): Payment = Payment(
        id = payment.id,
        employee = PaymentEmployee(
            id = payment.employee.id,
            firstName = payment.employee.firstName,
            lastName = payment.employee.lastName,
            patronymic = payment.employee.patronymic,
            simpleName = payment.employee.simpleName,
        ),
        percentFromRevenue = payment.percentFromRevenue,
        tips = payment.tips,
        workSeconds = payment.workSeconds,
    )

    private fun toDomain(payment: ManualOverridePaymentRequest): SaveShiftResultPayload.PaymentPayload =
        SaveShiftResultPayload.PaymentPayload(
            employeeId = payment.employeeId,
            percentFromRevenue = payment.percentFromRevenue,
            tips = payment.tips,
            workSeconds = payment.workSeconds,
        )

    fun toTransport(session: ShiftSession): TransportShiftSession = TransportShiftSession(
        id = session.id,
        companyId = session.companyId,
        startWorkTime = session.startWorkTime.toString(),
        date = session.date,
        status = ShiftSessionStatus.valueOf(session.status.name),
        checkpoints = session.checkpoints.toTransportCheckpoints(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun List<Checkpoint>.toTransportCheckpoints(): List<TransportCheckpoint> =
        // The generator models oneOf as an interface but does not declare its named DTOs as implementers.
        // The list-level bridge is safe because every element is one of those generated DTOs.
        map(::toTransportCheckpoint) as List<TransportCheckpoint>

    @Suppress("UNCHECKED_CAST")
    fun toTransportResponse(
        status: HttpStatus,
        checkpoint: Checkpoint,
    ): ResponseEntity<TransportCheckpoint> =
        // The container-level bridge is safe because the body is a generated oneOf variant.
        ResponseEntity.status(status).body(toTransportCheckpoint(checkpoint)) as ResponseEntity<TransportCheckpoint>

    private fun toTransportCheckpoint(checkpoint: Checkpoint): Any {
        val employees = checkpoint.employees.map {
            CheckpointEmployee(it.id, it.userId, it.firstName, it.lastName, it.patronymic, it.simpleName)
        }
        val metricRecords = checkpoint.metricRecords.map(::toTransport)
        return when (checkpoint.type) {
            CheckpointType.REGULAR -> RegularCheckpoint(
                checkpoint.id,
                checkpoint.tips,
                checkpoint.revenue,
                employees,
                checkpoint.dateTime.toString(),
                RegularCheckpoint.Type.REGULAR,
                metricRecords,
            )
            CheckpointType.FINAL -> FinalCheckpoint(
                checkpoint.id,
                checkpoint.tips,
                checkpoint.revenue,
                employees,
                checkpoint.dateTime.toString(),
                FinalCheckpoint.Type.FINAL,
                metricRecords,
            )
        }
    }

    private fun toTransport(record: CheckpointMetricRecord): TransportCheckpointMetricRecord =
        TransportCheckpointMetricRecord(
            id = record.id,
            label = record.label,
            destination = when (record.destination) {
                CheckpointCalcDestination.REVENUE -> CheckpointMetricDestination.REVENUE
                CheckpointCalcDestination.TIPS -> CheckpointMetricDestination.TIPS
            },
            value = record.value,
        )
}
