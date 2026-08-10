package org.turter.wageapp.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.turter.wageapp.domain.notification.NotificationEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class NotificationEventContractTest {

    @Test
    fun `notification HTTP payload nests company id in metadata`() {
        val companyId = UUID.fromString("6b457e51-773b-4d21-a2f0-8b9812481a91")
        val event = NotificationEvent.SessionOpened(
            meta = NotificationEvent.Meta(companyId, Instant.EPOCH),
            sessionId = UUID.fromString("d5c28ff4-5311-44fe-9fc8-7c16a6757683"),
            startWorkTime = LocalTime.of(9, 0),
            date = LocalDate.of(2026, 8, 11)
        )

        val payload = jacksonObjectMapper().findAndRegisterModules().valueToTree<com.fasterxml.jackson.databind.JsonNode>(event)

        assertEquals(companyId.toString(), payload.at("/meta/companyId").asText())
        assertNull(payload.get("companyId"))
        assertNull(payload.get("messageText"))
    }
}
