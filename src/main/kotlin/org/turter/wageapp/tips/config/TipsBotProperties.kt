package org.turter.wageapp.tips.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "tips.bot")
data class TipsBotProperties(
    @field:NotBlank val baseUrl: String,
    val timeout: Duration = Duration.ofSeconds(3),
    @field:Min(0) val retries: Int = 2,
) {
    init {
        require(!timeout.isNegative && !timeout.isZero) { "tips.bot.timeout must be positive" }
    }
}
