package org.turter.wageapp.tips

import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import org.turter.wageapp.tips.proto.GetTipsByDurationRequest
import org.turter.wageapp.tips.proto.TipsServiceGrpc
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class TipsBotClient(
    private val stub: TipsServiceGrpc.TipsServiceFutureStub,
    private val timeout: Duration,
    private val retries: Int,
) {
    suspend fun getTipsInKopecks(companyId: UUID, startTime: Instant, duration: Duration): Long {
        val request = GetTipsByDurationRequest.newBuilder()
            .setCompanyId(companyId.toString())
            .setStartTime(Timestamp.newBuilder().setSeconds(startTime.epochSecond).setNanos(startTime.nano))
            .setDuration(com.google.protobuf.Duration.newBuilder().setSeconds(duration.seconds).setNanos(duration.nano))
            .build()

        for (attempt in 0..retries) {
            currentCoroutineContext().ensureActive()
            try {
                return stub.withDeadlineAfter(timeout.toNanos(), TimeUnit.NANOSECONDS)
                    .getTipsByDuration(request)
                    .await()
                    .tips
            } catch (e: StatusRuntimeException) {
                val code = e.status.code
                val retryable = code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED
                if (!retryable || attempt == retries) {
                    throw TipsBotException(code == Status.Code.DEADLINE_EXCEEDED, e)
                }
                delay(100L * (attempt + 1))
            }
        }
        error("Tips bot retries must be non-negative")
    }
}
