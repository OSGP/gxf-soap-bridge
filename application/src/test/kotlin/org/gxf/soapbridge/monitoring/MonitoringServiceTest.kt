package org.gxf.soapbridge.monitoring

import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.gxf.soapbridge.monitoring.MonitoringService.Companion.CACHE_SIZE_METRIC
import org.gxf.soapbridge.monitoring.MonitoringService.Companion.CONNECTION_TIMER_CONTEXT_TAG
import org.gxf.soapbridge.monitoring.MonitoringService.Companion.CONNECTION_TIMER_METRIC
import org.gxf.soapbridge.monitoring.MonitoringService.Companion.CONNECTION_TIMER_SUCCESSFUL_TAG
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.TimeUnit


class MonitoringServiceTest {

    private lateinit var meterRegistry: MeterRegistry
    private lateinit var monitoringService: MonitoringService

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        monitoringService = MonitoringService(meterRegistry)
    }

    @AfterEach
    fun tearDown() {
        meterRegistry.clear()
    }

    @Test
    fun `cache size monitor matches map size`() {
        // Meter should not exist before creating it
        assertNull(meterRegistry.find(CACHE_SIZE_METRIC).gauge())

        // Create cache map and gauge
        val cacheMap = mutableMapOf<String, String>()
        val gauge = monitoringService.monitorCacheSize(cacheMap)

        // Check if the meter exists and is 0
        assertNotNull(meterRegistry.find(CACHE_SIZE_METRIC).gauge())
        assertThat(gauge.value()).isEqualTo(0.0)

        // After adding an entry it should be 1
        cacheMap["key"] = "value"
        assertThat(gauge.value()).isEqualTo(1.0)

        // After reassigning the key it should stay at 1
        cacheMap["key"] = "new-value"
        assertThat(gauge.value()).isEqualTo(1.0)

        // After adding a second key it should be 2
        cacheMap["new-key"] = "new-value"
        assertThat(gauge.value()).isEqualTo(2.0)
    }

    @Test
    fun `connection timer creates multiple timers`() {
        val startTime = Instant.now()
        val contextOne = "test-context-one"
        val successfulOne = true

        val expectedTagsOne = listOf(
            ImmutableTag(CONNECTION_TIMER_CONTEXT_TAG, contextOne),
            ImmutableTag(CONNECTION_TIMER_SUCCESSFUL_TAG, successfulOne.toString())
        )

        val contextTwo = "test-context-two"
        val successfulTwo = false
        val expectedTagsTwo = listOf(
            ImmutableTag(CONNECTION_TIMER_CONTEXT_TAG, contextTwo),
            ImmutableTag(CONNECTION_TIMER_SUCCESSFUL_TAG, successfulTwo.toString())
        )
        monitoringService.recordConnectionTime(startTime, contextOne, successfulOne)
        monitoringService.recordConnectionTime(startTime, contextTwo, successfulTwo)

        val timerOne =
            Search.`in`(meterRegistry)
                .name(CONNECTION_TIMER_METRIC)
                .tags(expectedTagsOne)
                .timer()

        assertThat(timerOne).isNotNull()

        val timerTwo =
            Search.`in`(meterRegistry)
                .name(CONNECTION_TIMER_METRIC)
                .tags(expectedTagsTwo)
                .timer()
        assertThat(timerTwo).isNotNull()

    }

    @Test
    fun `connection timer records request times`() {
        val startTime = Instant.now()
        val context = "test-context"
        val successful = true

        monitoringService.recordConnectionTime(startTime, context, successful)

        // Find the timer by name and tags
        val expectedTags = listOf(
            ImmutableTag(CONNECTION_TIMER_CONTEXT_TAG, context),
            ImmutableTag(CONNECTION_TIMER_SUCCESSFUL_TAG, successful.toString())
        )
        val timer = Search.`in`(meterRegistry)
            .name(CONNECTION_TIMER_METRIC)
            .tags(expectedTags)
            .timer()
        assertNotNull(timer)
        check(timer != null)


        assertThat(timer.count()).isEqualTo(1)
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isNotEqualTo(0)
        assertThat(timer.max(TimeUnit.NANOSECONDS)).isNotEqualTo(0)
        assertThat(timer.mean(TimeUnit.NANOSECONDS)).isNotEqualTo(0)

        monitoringService.recordConnectionTime(startTime, context, successful)

        assertThat(timer.count()).isEqualTo(2)
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isNotEqualTo(0)
        assertThat(timer.max(TimeUnit.NANOSECONDS)).isNotEqualTo(0)
        assertThat(timer.mean(TimeUnit.NANOSECONDS)).isNotEqualTo(0)

    }

}
