package org.turter.wageapp.tips.config

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.turter.wageapp.tips.TipsBotClient
import org.turter.wageapp.tips.proto.TipsServiceGrpc
import java.net.URI

@Configuration
@EnableConfigurationProperties(TipsBotProperties::class)
class TipsBotClientConfig {

    @Bean(destroyMethod = "shutdownNow")
    fun tipsBotChannel(props: TipsBotProperties): ManagedChannel {
        val address = URI.create("grpc://${props.baseUrl}")
        require(
            address.host != null && address.port in 1..65535 &&
                address.rawUserInfo == null && address.rawPath.isNullOrEmpty() &&
                address.rawQuery == null && address.rawFragment == null
        ) { "tips.bot.base-url must be host:port without a scheme or path" }

        return ManagedChannelBuilder.forAddress(address.host, address.port)
            .usePlaintext()
            .disableRetry()
            .build()
    }

    @Bean
    fun tipsBotClient(tipsBotChannel: ManagedChannel, props: TipsBotProperties): TipsBotClient =
        TipsBotClient(TipsServiceGrpc.newFutureStub(tipsBotChannel), props.timeout, props.retries)
}
