package org.turter.wageapp.messaging.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "notification.telegram.bot")
class TelegramNotificationClientProps {
    lateinit var url: String
    lateinit var timeout: Duration
}
