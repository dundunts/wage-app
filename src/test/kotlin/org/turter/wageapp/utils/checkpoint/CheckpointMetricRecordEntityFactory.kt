package org.turter.wageapp.utils.checkpoint

import org.turter.wageapp.application.data.shift.CheckpointMetricRecordDbEntity
import org.turter.wageapp.domain.shift.CheckpointCalcDestination
import java.util.*

object CheckpointMetricRecordEntityFactory {

    fun create(
        id: UUID? = null,
        checkpointId: UUID,
        label: String = "label",
        destination: CheckpointCalcDestination = CheckpointCalcDestination.REVENUE,
        value: Int = 100
    ) = CheckpointMetricRecordDbEntity().apply {
        this.id = id
        this.checkpointId = checkpointId
        this.label = label
        this.destination = destination
        this.value = value
    }
}
