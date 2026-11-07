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
    val error: String?,
    // Individual option leg quotes (bid / ask)
    val callK1Bid: Double,
    val callK1Ask: Double,
    val callK2Bid: Double,
    val callK2Ask: Double,
    val putK1Bid: Double,
    val putK1Ask: Double,
    val putK2Bid: Double,
    val putK2Ask: Double,
    // Net debit required to achieve the benchmark annualized rate
    val neededNetDebitForBenchmark: Double,
    // How much the current net debit must decrease to hit the benchmark rate
    val debitAdjustmentForBenchmark: Double
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
 *
 * Needed net debit for benchmark:
 *   neededDebit = width * 365 / (benchmarkRate * daysToExpiration)
 * Debit adjustment = current netDebit - neededDebit (positive = debit must come down)
 */
object BoxSpreadCalculator {

    fun calculate(
        underlying: String,
        expiration: LocalDate,
        k1: Double,
        k2: Double,
        callK1Bid: Double,
        callK1Ask: Double,
        callK2Bid: Double,
        callK2Ask: Double,
        putK1Bid: Double,
        putK1Ask: Double,
        putK2Bid: Double,
        putK2Ask: Double,
        benchmarkRate: Double
    ): BoxSpreadResult {
        val callK1Mid = (callK1Bid + callK1Ask) / 2.0
        val callK2Mid = (callK2Bid + callK2Ask) / 2.0
        val putK1Mid = (putK1Bid + putK1Ask) / 2.0
        val putK2Mid = (putK2Bid + putK2Ask) / 2.0

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
                error = "Strikes must satisfy 0 < K1 < K2.",
                callK1Bid = callK1Bid, callK1Ask = callK1Ask,
                callK2Bid = callK2Bid, callK2Ask = callK2Ask,
                putK1Bid = putK1Bid, putK1Ask = putK1Ask,
                putK2Bid = putK2Bid, putK2Ask = putK2Ask,
                neededNetDebitForBenchmark = 0.0,
                debitAdjustmentForBenchmark = 0.0
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
                error = "Net debit must be positive (no arbitrage available at these quotes).",
                callK1Bid = callK1Bid, callK1Ask = callK1Ask,
                callK2Bid = callK2Bid, callK2Ask = callK2Ask,
                putK1Bid = putK1Bid, putK1Ask = putK1Ask,
                putK2Bid = putK2Bid, putK2Ask = putK2Ask,
                neededNetDebitForBenchmark = 0.0,
                debitAdjustmentForBenchmark = 0.0
            )
        }

        val impliedRate = width / netDebit
        val annualizedRate = if (daysToExpiration > 0) impliedRate * (365.0 / daysToExpiration.toDouble()) else 0.0
        val isOpportunity = annualizedRate > benchmarkRate

        // Needed net debit to achieve benchmark rate:
        //   annualizedRate = width / netDebit * (365 / daysToExpiration)
        //   neededDebit = width * 365 / (benchmarkRate * daysToExpiration)
        val neededNetDebitForBenchmark = if (benchmarkRate > 0.0 && daysToExpiration > 0) {
            width * 365.0 / (benchmarkRate * daysToExpiration.toDouble())
        } else {
            0.0
        }
        val debitAdjustmentForBenchmark = netDebit - neededNetDebitForBenchmark

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
            error = null,
            callK1Bid = callK1Bid, callK1Ask = callK1Ask,
            callK2Bid = callK2Bid, callK2Ask = callK2Ask,
            putK1Bid = putK1Bid, putK1Ask = putK1Ask,
            putK2Bid = putK2Bid, putK2Ask = putK2Ask,
            neededNetDebitForBenchmark = neededNetDebitForBenchmark,
            debitAdjustmentForBenchmark = debitAdjustmentForBenchmark
        )
    }
}
