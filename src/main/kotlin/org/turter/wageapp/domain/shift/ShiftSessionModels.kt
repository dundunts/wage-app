package org.turter.wageapp.domain.shift

import java.util.UUID

data class ShiftSession(
    val id: UUID,
    val companyId: UUID,
    val status: Status,
    val checkpoints: List<ShiftCheckpoint>
) {
    enum class Status {
        OPENED, CLOSED, RECALCULATING
    }
}