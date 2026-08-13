package org.turter.wageapp.application.controller

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.controller.validation.PeriodRequestParamsValidator
import org.turter.wageapp.application.mapper.ShiftResultTransportMapper
import org.turter.wageapp.application.service.shift.SessionService
import org.turter.wageapp.application.service.shift.ShiftResultService
import org.turter.wageapp.domain.salary.PeriodType
import org.turter.wageapp.transport.api.ShiftResultApi
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultRequest
import org.turter.wageapp.transport.model.SaveManualOverrideShiftResultResponse
import org.turter.wageapp.transport.model.ShiftResultPage
import org.turter.wageapp.transport.model.ShiftResultResponse
import java.time.LocalDate
import java.util.UUID

@RestController
class ShiftResultController(
    private val shiftResultService: ShiftResultService,
    private val sessionService: SessionService,
    private val periodValidator: PeriodRequestParamsValidator,
    private val transportMapper: ShiftResultTransportMapper,
) : ShiftResultApi {

    override suspend fun getShiftResult(resultId: UUID): ResponseEntity<ShiftResultResponse> {
        val shiftResult = shiftResultService.getDetailed(resultId, currentUserId())
        val session = shiftResult.sessionId?.let { sessionId -> sessionService.getById(sessionId) }

        return ResponseEntity.ok(transportMapper.toTransport(shiftResult, session))
    }

    override suspend fun getShiftResultsPage(
        companyId: UUID,
        periodType: String,
        start: LocalDate?,
        end: LocalDate?,
        now: LocalDate?,
        page: String,
        size: String,
        sort: List<String>?,
    ): ResponseEntity<ShiftResultPage> {
        val pageable = PageRequest.of(
            page.toIntOrNull()?.takeIf { it >= 0 } ?: DEFAULT_PAGE,
            size.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(MAX_PAGE_SIZE) ?: DEFAULT_PAGE_SIZE,
            sort.toSpringSort(),
        )
        val results = shiftResultService.getDetailedPage(
            companyId,
            pageable,
            periodValidator.validateAndBuildPeriod(PeriodType.valueOf(periodType), start, end, now),
            currentUserId(),
        )

        return ResponseEntity.ok(transportMapper.toTransport(results))
    }

    override suspend fun saveManualOverrideShiftResult(
        saveManualOverrideShiftResultRequest: SaveManualOverrideShiftResultRequest,
    ): ResponseEntity<SaveManualOverrideShiftResultResponse> = ResponseEntity.status(201).body(
        transportMapper.toTransport(
            shiftResultService.save(
                transportMapper.toDomain(saveManualOverrideShiftResultRequest),
                currentUserId(),
            ),
        ),
    )

    override suspend fun deleteShiftResult(resultId: UUID): ResponseEntity<Unit> {
        shiftResultService.delete(resultId, currentUserId())
        return ResponseEntity.noContent().build()
    }

    private fun List<String>?.toSpringSort(): Sort {
        val orders = orEmpty().mapNotNull { criterion ->
            val parts = criterion.split(',')
            val property = parts.firstOrNull()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val direction = parts.getOrNull(1)?.let(Sort.Direction::fromOptionalString)
                ?.orElse(Sort.Direction.ASC) ?: Sort.Direction.ASC
            Sort.Order(direction, property)
        }
        return if (orders.isEmpty()) Sort.unsorted() else Sort.by(orders)
    }

    private companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_PAGE_SIZE = 2_000
    }
}
