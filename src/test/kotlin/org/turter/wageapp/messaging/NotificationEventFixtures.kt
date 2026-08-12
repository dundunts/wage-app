package org.turter.wageapp.messaging

import org.turter.wageapp.domain.notification.NotificationEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal fun sessionOpenedNotificationEvent(sessionId: UUID) = NotificationEvent.SessionOpened(
    meta = NotificationEvent.Meta(
        companyId = UUID.fromString("31345366-bb49-4123-ad98-70d3a749eb77"),
        createdAt = Instant.EPOCH
    ),
    sessionId = sessionId,
    startWorkTime = LocalTime.of(9, 0),
    date = LocalDate.of(2026, 8, 13)
)
