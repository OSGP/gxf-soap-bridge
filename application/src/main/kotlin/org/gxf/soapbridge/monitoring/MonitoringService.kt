package org.gxf.soapbridge.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class MonitoringService(
    private val registry: MeterRegistry
) {

    companion object {
        private const val METRIC_PREFIX = "gxf.soap.bridge"
    }

    private val connectionsFailed: Counter = Counter
        .builder("${METRIC_PREFIX}.connection.failed")
        .description("Counts the successful requests to the Maki API")
        .register(registry)

    private val connectionsSuccessful: Counter = Counter
        .builder("${METRIC_PREFIX}.connection.successful")
        .description("Counts the successful requests to the Maki API")
        .register(registry)


    fun connectionClose(startTime: Instant, context: String, successful: Boolean) {
        val duration = Duration.between(startTime, Instant.now())

        if (successful) connectionsSuccessful.increment()
        else connectionsFailed.increment()

        Timer
            .builder("${METRIC_PREFIX}.connection.timer")
            .description("Counts the successful requests to the Maki API")
            .tag("context", context)
            .register(registry)
            .record(duration)
    }
}