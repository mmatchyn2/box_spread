package com.example.boxspread

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class BoxSpreadCalculatorTest {

    @Test
    fun `computes width as K2 minus K1`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertTrue(result.isValid)
        assertEquals(100.0, result.width, 0.001)
    }

    @Test
    fun `computes net debit as (callK1 - callK2) + (putK2 - putK1)`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertEquals(20.0, result.netDebit, 0.001)
    }

    @Test
    fun `computes implied rate as width / netDebit`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertEquals(5.0, result.impliedRate, 0.001)
    }

    @Test
    fun `annualized rate scales by 365 / daysToExpiration`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        val expectedAnnualized = 5.0 * (365.0 / 30.0)
        assertEquals(expectedAnnualized, result.annualizedRate, 0.01)
    }

    @Test
    fun `isOpportunity when annualized rate above benchmark`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 1.0
        )

        assertTrue(result.isOpportunity)
    }

    @Test
    fun `not an opportunity when annualized rate below benchmark`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 100.0
        )

        assertFalse(result.isOpportunity)
    }

    @Test
    fun `invalid when K2 <= K1 (width <= 0)`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4600.0,
            k2 = 4500.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }

    @Test
    fun `invalid when netDebit <= 0`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Mid = 10.0,
            callK2Mid = 100.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }

    @Test
    fun `invalid when K1 <= 0`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 0.0,
            k2 = 4600.0,
            callK1Mid = 100.0,
            callK2Mid = 90.0,
            putK1Mid = 10.0,
            putK2Mid = 20.0,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }
}
