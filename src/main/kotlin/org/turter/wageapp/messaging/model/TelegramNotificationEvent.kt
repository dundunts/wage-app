package org.turter.wageapp.messaging.model

import java.util.UUID

data class TelegramNotificationEvent(
    val companyId: UUID,
    val messageText: String
)