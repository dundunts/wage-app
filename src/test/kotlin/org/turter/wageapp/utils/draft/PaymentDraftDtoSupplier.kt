package org.turter.wageapp.utils.draft

import org.turter.wageapp.domain.shift.PaymentDraft
import java.util.UUID

object PaymentDraftDtoSupplier {

    fun create(
        id: UUID = UUID.randomUUID(),
        employeeId: UUID,
        firstName: String = "first_name",
        lastName: String = "last_name",
        patronymic: String = "patronymic",
        simpleName: String? = "simple_name",
        percentFromRevenue: Int = 10,
        tips: Int = 100,
        workSeconds: Long = 3600
    ): PaymentDraft {
        return PaymentDraft(
            id = id,
            employee = PaymentDraft.EmployeeInfo(
                id = employeeId,
                firstName = firstName,
                lastName = lastName,
                patronymic = patronymic,
                simpleName = simpleName
            ),
            percentFromRevenue = percentFromRevenue,
            tips = tips,
            workSeconds = workSeconds
        )
    }
}