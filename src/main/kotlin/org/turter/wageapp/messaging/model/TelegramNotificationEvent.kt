package org.turter.wageapp.messaging.model

import java.util.UUID

@Deprecated("Now for messaging used NotificationEvent")
data class TelegramNotificationEvent(
    val companyId: UUID,
    val messageText: String
)