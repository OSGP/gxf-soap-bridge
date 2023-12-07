package org.gxf.soapbridge.monitoring

import io.micrometer.core.instrument.Gauge
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
        const val CACHE_SIZE_METRIC = "${METRIC_PREFIX}.cache.size"
        const val CONNECTION_TIMER_METRIC = "${METRIC_PREFIX}.cache.size"

        const val CONNECTION_TIMER_CONTEXT_TAG = "context"
        const val CONNECTION_TIMER_SUCCESSFUL_TAG = "successful"

    }

    /**
     * Creates a gauge to monitor the size of a cache.
     *
     * @param cache The cache to monitor, represented as a Map.
     * @return A Gauge object that measures the size of the cache.
     */
    fun monitorCacheSize(cache: Map<*, *>) =
        Gauge
            .builder(CACHE_SIZE_METRIC, cache) { it.size.toDouble() }
            .register(registry)

    /**
     * Records the connection time for a request.
     *
     * @param startTime The start time of the request.
     * @param context The context of the request.
     * @param successful Flag indicating if the request was successful.
     */
    fun recordConnectionTime(startTime: Instant, context: String, successful: Boolean) {
        val duration = Duration.between(startTime, Instant.now())

        Timer
            .builder(CONNECTION_TIMER_METRIC)
            .description("Counts the request time of a soap call")
            .tag(CONNECTION_TIMER_CONTEXT_TAG, context)
            .tag(CONNECTION_TIMER_SUCCESSFUL_TAG, successful.toString())
            .register(registry)
            .record(duration)
    }
}
