package com.adiansyah.pause

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAllowanceTest {
    @Test
    fun remainingTimeStopsAtZeroWhenAllowanceRunsOut() {
        val almostUsed = UsageAllowance(29 * 60_000L, 30)
        val used = UsageAllowance(30 * 60_000L, 30)
        val overUsed = UsageAllowance(31 * 60_000L, 30)

        assertEquals(60_000L, almostUsed.remainingMillis)
        assertFalse(almostUsed.isExhausted)
        assertEquals(0L, used.remainingMillis)
        assertTrue(used.isExhausted)
        assertEquals(0L, overUsed.remainingMillis)
        assertEquals(1f, overUsed.progress, 0f)
    }
}
