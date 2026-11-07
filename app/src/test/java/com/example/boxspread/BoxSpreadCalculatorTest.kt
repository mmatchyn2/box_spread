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
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
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
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertEquals(20.0, result.netCredit, 0.001)
    }

    @Test
    fun `computes implied rate as width divided by netCredit`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertEquals(5.0, result.impliedRate, 0.001)
    }

    @Test
    fun `annualized rate scales by 365 over daysToExpiration`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        val expectedAnnualized = 5.0 * (365.0 / 30.0)
        assertEquals(expectedAnnualized, result.annualizedRate, 0.01)
    }

    @Test
    fun `not an opportunity when annualized rate above benchmark`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 1.0
        )

        assertFalse(result.isOpportunity)
    }

    @Test
    fun `is an opportunity when annualized rate below benchmark`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 100.0
        )

        assertTrue(result.isOpportunity)
    }

    @Test
    fun `invalid when K2 is less than or equal K1 width is zero`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4600.0,
            k2 = 4500.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }

    @Test
    fun `invalid when netCredit is less than or equal 0`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 9.8, callK1Ask = 10.2,
            callK2Bid = 98.0, callK2Ask = 102.0,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }

    @Test
    fun `invalid when K1 is less than or equal 0`() {
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 0.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertFalse(result.isValid)
        assertNotNull(result.error)
    }

    @Test
    fun `computes needed net credit and adjustment for benchmark rate`() {
        // width = 100, netCredit = 20, days = 30, benchmark = 5%
        // annualized = 100/20 * 365/30 = 5.0 * 12.1667 = 60.83%
        // neededCredit = 100 * 365 / (0.05 * 30) = 36500 / 1.5 = 24333.33
        // adjustment = 20 - 24333.33 = -24313.33 (credit short)
        val result = BoxSpreadCalculator.calculate(
            underlying = "SPX",
            expiration = LocalDate.now().plusDays(30),
            k1 = 4500.0,
            k2 = 4600.0,
            callK1Bid = 98.0, callK1Ask = 102.0,
            callK2Bid = 88.2, callK2Ask = 91.8,
            putK1Bid = 9.8, putK1Ask = 10.2,
            putK2Bid = 19.6, putK2Ask = 20.4,
            benchmarkRate = 5.0
        )

        assertTrue(result.isValid)
        assertTrue(result.neededNetCreditForBenchmark > 0.0)
        assertEquals(20.0 - result.neededNetCreditForBenchmark, result.creditAdjustmentForBenchmark, 0.01)
    }
}
