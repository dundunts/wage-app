package org.turter.wageapp.config

import com.redis.testcontainers.RedisContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.function.Supplier

@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient
@ActiveProfiles(profiles = ["test-it"])
@Testcontainers
open class CommonWageAppIT {

    @Autowired
    protected lateinit var client: WebTestClient

    companion object {
        @JvmStatic
        @Container
        val POSTGRES_SQL_CONTAINER: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")

//        @JvmStatic
//        @Container
//        val REDIS_CONTAINER: RedisContainer = RedisContainer(DockerImageName.parse("redis:6.2"))
//            .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { r2dbcUrl() }
            registry.add("spring.r2dbc.username") { POSTGRES_SQL_CONTAINER.username }
            registry.add("spring.r2dbc.password") { POSTGRES_SQL_CONTAINER.password }

            registry.add("spring.liquibase.url") { POSTGRES_SQL_CONTAINER.jdbcUrl }
            registry.add("spring.liquibase.user") { POSTGRES_SQL_CONTAINER.username }
            registry.add("spring.liquibase.password") { POSTGRES_SQL_CONTAINER.password }

//            registry.add("spring.data.redis.host") { REDIS_CONTAINER.host }
//            registry.add(
//                "spring.data.redis.port"
//            ) { REDIS_CONTAINER.getMappedPort(6379).toString() }
        }

        @JvmStatic
        fun r2dbcUrl() =
            "r2dbc:postgresql://${POSTGRES_SQL_CONTAINER.host}:" +
                    "${POSTGRES_SQL_CONTAINER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/" +
                    POSTGRES_SQL_CONTAINER.databaseName
    }
}