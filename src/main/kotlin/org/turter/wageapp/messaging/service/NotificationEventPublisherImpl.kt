package org.turter.wageapp.messaging.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.domain.notification.NotificationEventPublisher

@Component
class NotificationEventPublisherImpl : NotificationEventPublisher {
    private val log = LoggerFactory.getLogger(NotificationEventPublisherImpl::class.java)

    private val _flow = MutableSharedFlow<NotificationEvent>(
        extraBufferCapacity = 100
    )

    override val eventsFlow = _flow.asSharedFlow()

    override fun publish(event: NotificationEvent) {
        if (!_flow.tryEmit(event)) {
            log.warn(
                "Dropped notification event because the buffer is full type={} companyId={}",
                event::class.simpleName,
                event.meta.companyId
            )
        }
    }

}
