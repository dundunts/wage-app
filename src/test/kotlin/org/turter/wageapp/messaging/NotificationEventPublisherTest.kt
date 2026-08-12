package org.turter.wageapp.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.turter.wageapp.messaging.service.NotificationEventPublisherImpl
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationEventPublisherTest {

    @Test
    fun `buffer overflow is logged with notification event context`() = runTest {
        val publisher = NotificationEventPublisherImpl()
        val collectorBlocked = CompletableDeferred<Unit>()
        backgroundScope.launch {
            publisher.eventsFlow.collect {
                collectorBlocked.complete(Unit)
                CompletableDeferred<Unit>().await()
            }
        }
        runCurrent()

        publisher.publish(sessionOpenedNotificationEvent(UUID.fromString("075bf756-ef1a-4ca3-8f93-f583874b037e")))
        runCurrent()
        collectorBlocked.await()
        repeat(100) {
            publisher.publish(sessionOpenedNotificationEvent(UUID.randomUUID()))
        }

        val droppedEvent = sessionOpenedNotificationEvent(UUID.fromString("4f302dbc-e24d-44dd-adc3-b2def297e6fe"))
        val logger = LoggerFactory.getLogger(NotificationEventPublisherImpl::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        try {
            publisher.publish(droppedEvent)

            val warning = appender.list.single()
            assertEquals(Level.WARN, warning.level)
            assertEquals(
                "Dropped notification event because the buffer is full type={} companyId={}",
                warning.message
            )
            assertTrue(warning.argumentArray.contains("SessionOpened"))
            assertTrue(warning.argumentArray.contains(droppedEvent.meta.companyId))
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

}
