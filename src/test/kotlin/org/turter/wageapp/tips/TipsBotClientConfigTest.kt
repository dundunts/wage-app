package org.turter.wageapp.tips

import io.grpc.ManagedChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.turter.wageapp.tips.config.TipsBotClientConfig
import org.turter.wageapp.tips.config.TipsBotProperties
import java.time.Duration

class TipsBotClientConfigTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(TipsBotClientConfig::class.java)

    @Test
    fun `address is required at startup`() {
        runner.run { assertNotNull(it.startupFailure) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "localhost", "localhost:0", "localhost:65536", "http://localhost:50051", "localhost:50051/path", "user@localhost:50051"])
    fun `rejects invalid addresses at startup`(address: String) {
        runner.withPropertyValues("tips.bot.base-url=$address").run { assertNotNull(it.startupFailure) }
    }

    @Test
    fun `valid address starts without the bot and closes the channel on shutdown`() {
        lateinit var channel: ManagedChannel
        runner.withPropertyValues("tips.bot.base-url=localhost:1").run {
            assertNull(it.startupFailure)
            val properties = it.getBean(TipsBotProperties::class.java)
            assertEquals(Duration.ofSeconds(3), properties.timeout)
            assertEquals(2, properties.retries)
            channel = it.getBean(ManagedChannel::class.java)
        }
        assertTrue(channel.isShutdown)
    }
}
