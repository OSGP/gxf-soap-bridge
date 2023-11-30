package org.gxf.soapbridge.monitoring

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

    fun recordConnectionTime(startTime: Instant, context: String, successful: Boolean) {
        val duration = Duration.between(startTime, Instant.now())

        Timer
            .builder("${METRIC_PREFIX}.connection.timer")
            .description("Counts the successful requests to the Maki API")
            .tag("context", context)
            .tag("successful", successful.toString())
            .register(registry)
            .record(duration)
    }
}
