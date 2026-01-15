package org.turter.wageapp.utils.checkpoint

import org.turter.wageapp.application.data.shift.CheckpointDbEntity
import org.turter.wageapp.domain.shift.CheckpointType
import java.time.LocalDateTime
import java.util.*

object CheckpointEntityFactory {

    fun create(
        id: UUID? = null,
        shiftSessionId: UUID,
        revenue: Int = 1000,
        tips: Int = 100,
        dateTime: LocalDateTime = LocalDateTime.now(),
        type: CheckpointType = CheckpointType.REGULAR
    ) = CheckpointDbEntity().apply {
        this.id = id
        this.shiftSessionId = shiftSessionId
        this.revenue = revenue
        this.tips = tips
        this.dateTime = dateTime
        this.type = type
    }
}
