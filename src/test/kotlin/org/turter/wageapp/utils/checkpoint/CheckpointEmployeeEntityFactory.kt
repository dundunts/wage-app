package org.turter.wageapp.utils.checkpoint

import org.turter.wageapp.application.data.shift.CheckpointEmployeeDbEntity
import java.util.*

object CheckpointEmployeeEntityFactory {

    fun create(
        id: UUID? = null,
        checkpointId: UUID,
        employeeId: UUID
    ) = CheckpointEmployeeDbEntity().apply {
        this.id = id
        this.checkpointId = checkpointId
        this.employeeId = employeeId
    }
}
