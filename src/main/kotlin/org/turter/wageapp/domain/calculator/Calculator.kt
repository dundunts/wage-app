package org.turter.wageapp.domain.calculator

import org.turter.wageapp.domain.shift.CheckpointType
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

data class PaymentDraftCalculation(
    var employeeId: UUID,
    var percentFromRevenue: Int,
    var tips: Int,
    var workSeconds: Long
)

data class CheckpointInfo(
    val id: UUID,
    val tips: Int,
    val revenue: Int,
    val employeeIds: List<UUID>,
    val dateTime: LocalDateTime,
    val type: CheckpointType,
)

data class CoefficientFromRevenue(
    val coefficientAsInt: Int
) {
    private val divider = 10_000

    val coefficientAsDouble = coefficientAsInt.toDouble() / divider
}

class PaymentDraftCalculator(
    checkpoints: List<CheckpointInfo>,
    private val coefficientFromRevenue: CoefficientFromRevenue,
    private val initialDateTime: LocalDateTime
) {
    private val checkpoints = checkpoints.sortedBy { it.dateTime }

    fun calculate(): Map<UUID, PaymentDraftCalculation> {
        val employeePaymentMap = ConcurrentHashMap<UUID, PaymentDraftCalculation>()

        for (i in 0 until checkpoints.size) {
            val prevRevenue = if (i == 0) 0 else checkpoints[i - 1].revenue

            val prevTips = if (i == 0) 0 else checkpoints[i - 1].tips

            val prevDateTime = if (i == 0) initialDateTime
            else checkpoints[i - 1].dateTime

            val current = checkpoints[i]

            val employeeCount = current.employeeIds.size

            if (employeeCount == 0) continue

            val k = coefficientFromRevenue.coefficientAsDouble

            val percentFromRevenue = ((current.revenue - prevRevenue) * k / employeeCount).roundToInt()

            val tips = (current.tips - prevTips) / employeeCount

            current.employeeIds.forEach { empId ->
                val record = employeePaymentMap[empId]

                if (record == null) {
                    employeePaymentMap[empId] = PaymentDraftCalculation(
                        employeeId = empId,
                        percentFromRevenue = percentFromRevenue,
                        tips = tips,
                        workSeconds = Duration.between(prevDateTime, current.dateTime).toSeconds()
                    )
                } else {
                    record.apply {
                        this@apply.percentFromRevenue += percentFromRevenue
                        this@apply.tips += tips
                        workSeconds += Duration.between(prevDateTime, current.dateTime).toSeconds()
                    }
                }
            }
        }

        return employeePaymentMap
    }
}
