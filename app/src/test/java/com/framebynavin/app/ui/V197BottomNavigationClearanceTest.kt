package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class V197BottomNavigationClearanceTest {
    @Test fun measuredNavKeepsLegacyMinimumOnOrdinaryPhone() {
        assertEquals(124f, V197BottomNavigationClearance.calculate(2400, 2100f, 3f), 0.001f)
    }

    @Test fun tallNavigationReceivesAdditionalBottomRoom() {
        // 510 physical pixels are obscured by the system bars, floating nav and margin.
        assertEquals(186f, V197BottomNavigationClearance.calculate(2400, 1890f, 3f), 0.001f)
    }

    @Test fun changeInDensityDoesNotChangePhysicalClearanceCalculation() {
        val standard = V197BottomNavigationClearance.calculate(2000, 1500f, 2.5f)
        val compact = V197BottomNavigationClearance.calculate(1600, 1200f, 2f)
        assertEquals(standard, compact, 0.001f)
    }

    @Test fun invalidMeasurementsStaySafe() {
        assertEquals(124f, V197BottomNavigationClearance.calculate(0, 0f, 3f), 0.001f)
        assertEquals(124f, V197BottomNavigationClearance.calculate(1800, Float.NaN, 3f), 0.001f)
        assertEquals(124f, V197BottomNavigationClearance.calculate(1800, 2000f, 3f), 0.001f)
        assertEquals(124f, V197BottomNavigationClearance.calculate(1800, 1200f, 0f), 0.001f)
    }
}
