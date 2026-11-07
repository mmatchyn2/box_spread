package com.example.boxspread

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Result of a reverse box-spread (box-spread loan) calculation.
 *
 * A reverse box consists of four legs, same expiration, K1 < K2:
 *   Short call K1 + Long call K2 + Short put K2 + Long put K1
 *
 * You receive a net CREDIT upfront and pay fixed width (K2 − K1) at expiration.
 * This is borrowing — you pay the implied annualized rate as interest.
 *
 * Net credit = call K1 bid − call K2 ask + put K2 bid − put K1 ask
 * Borrowing rate = width / netCredit, annualized by days to expiration
 * Lower rate = cheaper loan = better opportunity
 */
data class BoxSpreadResult(
    val underlying: String,
    val expiration: LocalDate,
    val k1: Double,
    val k2: Double,
    val width: Double,
    val netCredit: Double,
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
    // Net credit required to achieve the benchmark borrowing rate
    val neededNetCreditForBenchmark: Double,
    // How much current net credit exceeds the needed amount (positive = good)
    val creditAdjustmentForBenchmark: Double
)

/**
 * Pure reverse box-spread calculation logic. No Android dependencies — fully testable on the JVM.
 *
 * A reverse box (box-spread loan) consists of four legs, same expiration, K1 < K2:
 *   Short call K1 + Long call K2 + Short put K2 + Long put K1
 *
 * You receive net credit upfront and pay constant width (K2 − K1) at expiration.
 * This is borrowing — you pay the implied annualized rate as interest.
 *
 * Net credit = call K1 − call K2 + put K2 − put K1 (using mids)
 * Borrowing rate = width / netCredit, annualized by days to expiration
 *
 * Needed net credit for benchmark:
 *   neededCredit = width * 365 / (benchmarkRate * daysToExpiration)
 * Credit adjustment = current netCredit - neededCredit (positive = rate below benchmark)
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

        // Net credit received: (call K1 − call K2) + (put K2 − put K1)
        // For reverse box: positive = money received upfront (borrowing)
        val netCredit = (callK1Mid - callK2Mid) + (putK2Mid - putK1Mid)

        if (k1 <= 0.0 || k2 <= k1) {
            return BoxSpreadResult(
                underlying = underlying,
                expiration = expiration,
                k1 = k1,
                k2 = k2,
                width = width,
                netCredit = netCredit,
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
                neededNetCreditForBenchmark = 0.0,
                creditAdjustmentForBenchmark = 0.0
            )
        }

        if (netCredit <= 0.0) {
            return BoxSpreadResult(
                underlying = underlying,
                expiration = expiration,
                k1 = k1,
                k2 = k2,
                width = width,
                netCredit = netCredit,
                impliedRate = 0.0,
                annualizedRate = 0.0,
                daysToExpiration = daysToExpiration,
                isValid = false,
                isOpportunity = false,
                benchmarkRate = benchmarkRate,
                error = "Net credit must be positive (no loan available at these quotes).",
                callK1Bid = callK1Bid, callK1Ask = callK1Ask,
                callK2Bid = callK2Bid, callK2Ask = callK2Ask,
                putK1Bid = putK1Bid, putK1Ask = putK1Ask,
                putK2Bid = putK2Bid, putK2Ask = putK2Ask,
                neededNetCreditForBenchmark = 0.0,
                creditAdjustmentForBenchmark = 0.0
            )
        }

        val impliedRate = width / netCredit
        val annualizedRate = if (daysToExpiration > 0) impliedRate * (365.0 / daysToExpiration.toDouble()) else 0.0
        // For borrowing, LOWER rate is better — opportunity if below benchmark
        val isOpportunity = annualizedRate < benchmarkRate

        // Needed net credit to achieve benchmark rate:
        //   annualizedRate = width / netCredit * (365 / daysToExpiration)
        //   neededCredit = width * 365 / (benchmarkRate * daysToExpiration)
        val neededNetCreditForBenchmark = if (benchmarkRate > 0.0 && daysToExpiration > 0) {
            width * 365.0 / (benchmarkRate * daysToExpiration.toDouble())
        } else {
            0.0
        }
        val creditAdjustmentForBenchmark = netCredit - neededNetCreditForBenchmark

        return BoxSpreadResult(
            underlying = underlying,
            expiration = expiration,
            k1 = k1,
            k2 = k2,
            width = width,
            netCredit = netCredit,
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
            neededNetCreditForBenchmark = neededNetCreditForBenchmark,
            creditAdjustmentForBenchmark = creditAdjustmentForBenchmark
        )
    }
}
