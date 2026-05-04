package com.skeler.pulse.design.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollMotionTest {

    @Test
    fun `opposite drag relaxes positive overscroll toward zero`() {
        assertEquals(-30f, overscrollRelaxationDelta(currentOffset = 80f, dragDelta = -30f), 0.001f)
    }

    @Test
    fun `opposite drag does not relax beyond zero`() {
        assertEquals(-80f, overscrollRelaxationDelta(currentOffset = 80f, dragDelta = -140f), 0.001f)
    }

    @Test
    fun `same direction drag does not relax overscroll`() {
        assertEquals(0f, overscrollRelaxationDelta(currentOffset = 80f, dragDelta = 30f), 0.001f)
    }
}
