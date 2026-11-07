package com.example.boxspread

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Result of a box spread calculation.
 */
data class BoxSpreadResult(
    val underlying: String,
    val expiration: LocalDate,
    val k1: Double,
    val k2: Double,
    val width: Double,
    val netDebit: Double,
    val impliedRate: Double,
    val annualizedRate: Double,
    val daysToExpiration: Long,
    val isValid: Boolean,
    val isOpportunity: Boolean,
    val benchmarkRate: Double,
    val error: String?
)

/**
 * Pure box-spread calculation logic. No Android dependencies — fully testable on the JVM.
 *
 * A box spread consists of four legs, same expiration, K1 < K2:
 *   Long call K1 + Short call K2 + Long put K2 + Short put K1
 *
 * Payoff at expiration = K2 − K1 (constant width)
 * Net debit = (call K1 − call K2) + (put K2 − put K1)
 * Implied annualized rate = (width / netDebit), annualized by days to expiration
 */
object BoxSpreadCalculator {

    fun calculate(
        underlying: String,
        expiration: LocalDate,
        k1: Double,
        k2: Double,
        callK1Mid: Double,
        callK2Mid: Double,
        putK1Mid: Double,
        putK2Mid: Double,
        benchmarkRate: Double
    ): BoxSpreadResult {
        val width = k2 - k1
        val daysToExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expiration)

        // Net debit: (call K1 − call K2) + (put K2 − put K1)
        val netDebit = (callK1Mid - callK2Mid) + (putK2Mid - putK1Mid)

        if (k1 <= 0.0 || k2 <= k1) {
            return BoxSpreadResult(
                underlying = underlying,
                expiration = expiration,
                k1 = k1,
                k2 = k2,
                width = width,
                netDebit = netDebit,
                impliedRate = 0.0,
                annualizedRate = 0.0,
                daysToExpiration = daysToExpiration,
                isValid = false,
                isOpportunity = false,
                benchmarkRate = benchmarkRate,
                error = "Strikes must satisfy 0 < K1 < K2."
            )
        }

        if (netDebit <= 0.0) {
            return BoxSpreadResult(
                underlying = underlying,
                expiration = expiration,
                k1 = k1,
                k2 = k2,
                width = width,
                netDebit = netDebit,
                impliedRate = 0.0,
                annualizedRate = 0.0,
                daysToExpiration = daysToExpiration,
                isValid = false,
                isOpportunity = false,
                benchmarkRate = benchmarkRate,
                error = "Net debit must be positive (no arbitrage available at these quotes)."
            )
        }

        val impliedRate = width / netDebit
        val annualizedRate = if (daysToExpiration > 0) impliedRate * (365.0 / daysToExpiration.toDouble()) else 0.0
        val isOpportunity = annualizedRate > benchmarkRate

        return BoxSpreadResult(
            underlying = underlying,
            expiration = expiration,
            k1 = k1,
            k2 = k2,
            width = width,
            netDebit = netDebit,
            impliedRate = impliedRate,
            annualizedRate = annualizedRate,
            daysToExpiration = daysToExpiration,
            isValid = true,
            isOpportunity = isOpportunity,
            benchmarkRate = benchmarkRate,
            error = null
        )
    }
}
