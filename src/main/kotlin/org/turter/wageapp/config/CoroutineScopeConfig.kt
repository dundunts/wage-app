package org.turter.wageapp.config

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Configuration
class CoroutineScopeConfig {

    @Bean
    fun applicationCoroutineScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("application-scope"))

}

@Component
class CoroutineShutdownListener(
    private val applicationCoroutineScope: CoroutineScope
) {

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        applicationCoroutineScope.cancel("Application context is shutting down")
    }

}
