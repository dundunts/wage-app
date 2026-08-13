package org.turter.wageapp.application.mapper

import org.springframework.stereotype.Component
import org.turter.wageapp.domain.shift.CheckpointCalcDestination as DomainCheckpointMetricDestination
import org.turter.wageapp.domain.shift.CheckpointMetricRecordPayload as DomainCheckpointMetricRecordPayload
import org.turter.wageapp.domain.shift.CheckpointType as DomainCheckpointType
import org.turter.wageapp.domain.shift.CreateRecalculatingShiftSessionPayload
import org.turter.wageapp.domain.shift.CreateRegularCheckpointPayload
import org.turter.wageapp.domain.shift.OpenNewShiftSessionPayload
import org.turter.wageapp.domain.shift.UpdateShiftCheckpointPayload
import org.turter.wageapp.domain.shift.UpdateShiftSessionStartWorkTimePayload
import org.turter.wageapp.transport.model.CheckpointMetricRecordRequest
import org.turter.wageapp.transport.model.CheckpointType
import org.turter.wageapp.transport.model.CreateCheckpointRequest
import org.turter.wageapp.transport.model.OpenShiftSessionRecalculationRequest
import org.turter.wageapp.transport.model.OpenShiftSessionRequest
import org.turter.wageapp.transport.model.UpdateCheckpointRequest
import org.turter.wageapp.transport.model.UpdateShiftSessionStartRequest
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class ShiftSessionCheckpointRequestMapper {

    fun toDomain(request: OpenShiftSessionRequest): OpenNewShiftSessionPayload =
        OpenNewShiftSessionPayload(
            companyId = request.companyId,
            startWorkAt = request.startWorkAt.toLocalDateTime("startWorkAt"),
        )

    fun toDomain(request: OpenShiftSessionRecalculationRequest): CreateRecalculatingShiftSessionPayload =
        CreateRecalculatingShiftSessionPayload(closedSessionId = request.closedSessionId)

    fun toDomain(request: UpdateShiftSessionStartRequest): UpdateShiftSessionStartWorkTimePayload =
        UpdateShiftSessionStartWorkTimePayload(
            sessionId = request.sessionId,
            startWorkTime = request.startWorkTime.toLocalTime("startWorkTime"),
        )

    fun toDomain(request: CreateCheckpointRequest): CreateRegularCheckpointPayload =
        CreateRegularCheckpointPayload(
            sessionId = request.sessionId,
            revenue = request.revenue,
            tips = request.tips,
            employeeIds = request.employeeIds,
            dateTime = request.dateTime.toLocalDateTime("dateTime"),
            type = request.type.toDomain(),
            fieldRecords = request.fieldRecords.map { it.toDomain() },
        )

    fun toDomain(request: UpdateCheckpointRequest): UpdateShiftCheckpointPayload =
        UpdateShiftCheckpointPayload(
            id = request.id,
            revenue = request.revenue,
            tips = request.tips,
            employeeIds = request.employeeIds,
            dateTime = request.dateTime.toLocalDateTime("dateTime"),
            type = request.type.toDomain(),
            fieldRecords = request.fieldRecords.map { it.toDomain() },
        )

    private fun CheckpointType.toDomain(): DomainCheckpointType = DomainCheckpointType.valueOf(name)

    private fun Any.toLocalDateTime(fieldName: String): LocalDateTime {
        val value = this as? String
            ?: throw IllegalArgumentException("$fieldName must be an ISO-8601 local date-time string")
        return value.toLocalDateTime(fieldName)
    }

    private fun String.toLocalDateTime(fieldName: String): LocalDateTime =
        parseTemporal(fieldName) { LocalDateTime.parse(this) }

    private fun String.toLocalTime(fieldName: String): LocalTime =
        parseTemporal(fieldName) { LocalTime.parse(this) }

    private fun <T> String.parseTemporal(fieldName: String, parse: () -> T): T =
        try {
            parse()
        } catch (exception: DateTimeException) {
            throw IllegalArgumentException("$fieldName has an invalid temporal value", exception)
        }

    private fun CheckpointMetricRecordRequest.toDomain(): DomainCheckpointMetricRecordPayload =
        DomainCheckpointMetricRecordPayload(
            label = label,
            destination = DomainCheckpointMetricDestination.valueOf(destination.name),
            value = value,
        )
}
