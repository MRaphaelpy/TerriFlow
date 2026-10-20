package com.mraphaelpy.terriflow.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.concurrent.TimeUnit

class SyncPlannerTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = 10 * day

    @Test
    fun `primeira sincronizacao e completa`() {
        assertEquals(SyncPlan.Full, SyncPlanner.plan(null, null, day, now))
    }

    @Test
    fun `dentro do intervalo busca so o que mudou com margem`() {
        val last = now - TimeUnit.HOURS.toMillis(1)
        assertEquals(
            SyncPlan.Incremental(Date(last - SyncPlanner.SAFETY_MARGIN_MS)),
            SyncPlanner.plan(last, now - TimeUnit.HOURS.toMillis(5), day, now)
        )
    }

    @Test
    fun `apos o intervalo volta a fazer completa`() {
        assertEquals(SyncPlan.Full, SyncPlanner.plan(now - 1000, now - day, day, now))
    }
}
