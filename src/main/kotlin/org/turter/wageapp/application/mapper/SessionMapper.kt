package org.turter.wageapp.application.mapper

import org.mapstruct.Mapper
import org.turter.wageapp.application.data.shift.ShiftSessionDbEntity
import org.turter.wageapp.domain.shift.Checkpoint
import org.turter.wageapp.domain.shift.ShiftSession

@Mapper(
    componentModel = "spring",
    uses = [CheckpointMapper::class]
)
interface SessionMapper {

    fun toShiftSession(entity: ShiftSessionDbEntity, checkpoints: List<Checkpoint>): ShiftSession

}