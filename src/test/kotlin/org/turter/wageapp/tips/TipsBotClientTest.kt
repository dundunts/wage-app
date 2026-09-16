package org.turter.wageapp.tips

import io.grpc.Context
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.turter.wageapp.tips.proto.GetTipsByDurationRequest
import org.turter.wageapp.tips.proto.GetTipsByDurationResponse
import org.turter.wageapp.tips.proto.TipsServiceGrpc
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class TipsBotClientTest {
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private val companyId = UUID.randomUUID()
    private val start = Instant.parse("2026-09-15T06:12:34.123456789Z")
    private val requests = CopyOnWriteArrayList<GetTipsByDurationRequest>()

    @AfterEach
    fun stop() {
        if (::channel.isInitialized) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        if (::server.isInitialized) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun client(
        timeout: Duration = Duration.ofSeconds(3),
        respond: (StreamObserver<GetTipsByDurationResponse>) -> Unit,
    ): TipsBotClient {
        val service = object : TipsServiceGrpc.TipsServiceImplBase() {
            override fun getTipsByDuration(
                request: GetTipsByDurationRequest,
                responseObserver: StreamObserver<GetTipsByDurationResponse>,
            ) {
                requests.add(request)
                respond(responseObserver)
            }
        }
        val name = InProcessServerBuilder.generateName()
        server = InProcessServerBuilder.forName(name).directExecutor().addService(service).build().start()
        channel = InProcessChannelBuilder.forName(name).directExecutor().disableRetry().build()
        return TipsBotClient(TipsServiceGrpc.newFutureStub(channel), timeout, 2)
    }

    @Test
    fun `sends required protobuf fields without losing timestamp precision`() = runBlocking {
        val client = client { response ->
            val deadlineMillis = Context.current().deadline.timeRemaining(TimeUnit.MILLISECONDS)
            assertTrue(deadlineMillis in 1..3000)
            response.onNext(GetTipsByDurationResponse.newBuilder().setTips(Long.MAX_VALUE).build())
            response.onCompleted()
        }

        assertEquals(Long.MAX_VALUE, client.getTipsInKopecks(companyId, start, Duration.ofHours(24)))
        val request = requests.single()
        assertEquals(companyId.toString(), request.companyId)
        assertTrue(request.hasStartTime())
        assertEquals(start.epochSecond, request.startTime.seconds)
        assertEquals(start.nano, request.startTime.nanos)
        assertTrue(request.hasDuration())
        assertEquals(86400, request.duration.seconds)
        assertEquals(0, request.duration.nanos)
    }

    @Test
    fun `retries temporary unavailability twice and returns third result`() = runBlocking {
        val client = client { response ->
            if (requests.size < 3) {
                response.onError(Status.UNAVAILABLE.asRuntimeException())
            } else {
                response.onNext(GetTipsByDurationResponse.newBuilder().setTips(12345).build())
                response.onCompleted()
            }
        }
        assertEquals(12345, client.getTipsInKopecks(companyId, start, Duration.ofHours(24)))
        assertEquals(3, requests.size)
        assertEquals(1, requests.distinct().size)
    }

    @Test
    fun `stops after three unavailable attempts`() = runBlocking {
        val client = client { it.onError(Status.UNAVAILABLE.withDescription("private upstream detail").asRuntimeException()) }
        val error = try {
            client.getTipsInKopecks(companyId, start, Duration.ofHours(24))
            error("Expected failure")
        } catch (e: TipsBotException) { e }
        assertFalse(error.timedOut)
        assertEquals("Tips bot request failed", error.message)
        assertEquals(3, requests.size)
    }

    @Test
    fun `does not retry permanent upstream errors`() = runBlocking {
        val client = client { it.onError(Status.INVALID_ARGUMENT.asRuntimeException()) }
        val error = try {
            client.getTipsInKopecks(companyId, start, Duration.ofHours(24))
            error("Expected failure")
        } catch (e: TipsBotException) { e }
        assertFalse(error.timedOut)
        assertEquals(1, requests.size)
    }

    @Test
    fun `actual deadlines cancel hanging attempts and retry twice`() = runBlocking {
        val cancellations = CopyOnWriteArrayList<Boolean>()
        val client = client(Duration.ofMillis(100)) {
            Context.current().addListener({ cancellations.add(true) }, Executor { it.run() })
        }
        val error = withTimeout(5000) {
            try {
                client.getTipsInKopecks(companyId, start, Duration.ofHours(24))
                error("Expected failure")
            } catch (e: TipsBotException) { e }
        }
        assertTrue(error.timedOut)
        assertEquals(3, requests.size)
        assertEquals(3, cancellations.size)
    }

    @Test
    fun `coroutine cancellation cancels grpc and does not retry`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val client = client {
            Context.current().addListener({ cancelled.complete(Unit) }, Executor { it.run() })
            started.complete(Unit)
        }
        val call = async { client.getTipsInKopecks(companyId, start, Duration.ofHours(24)) }
        withTimeout(5000) { started.await() }
        call.cancelAndJoin()
        withTimeout(5000) { cancelled.await() }
        assertEquals(1, requests.size)
    }
}
