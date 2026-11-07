package com.example.boxspread

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class OptionQuote(
    val symbol: String,
    val strike: Double,
    val expiration: LocalDate,
    val optionType: String,
    val bid: Double,
    val ask: Double
) {
    val mid: Double get() = (bid + ask) / 2.0
}

interface QuoteService {
    suspend fun fetchQuote(symbol: String, strike: Double, expiration: String, optionType: String): OptionQuote?
    suspend fun fetchSpot(symbol: String): Double?
    suspend fun findBestBoxSpread(symbol: String, expiration: String, benchmarkRate: Double, maxPairs: Int = 200): BoxSpreadScan?
}

/**
 * Best box-spread scan result returned by [QuoteService.findBestBoxSpread].
 */
data class BoxSpreadScan(
    val k1: Double,
    val k2: Double,
    val callK1: OptionQuote,
    val callK2: OptionQuote,
    val putK1: OptionQuote,
    val putK2: OptionQuote,
    val annualizedRate: Double,
    val netCredit: Double,
    val width: Double,
    val isOpportunity: Boolean
)

object MockQuoteService : QuoteService {

    private val spotPrices = mapOf(
        "SPX" to 4550.0,
        "NDX" to 15200.0,
        "VIX" to 18.5
    )

    override suspend fun fetchSpot(symbol: String): Double? {
        return spotPrices[symbol]
    }

    override suspend fun fetchQuote(symbol: String, strike: Double, expiration: String, optionType: String): OptionQuote? {
        val spot = spotPrices[symbol] ?: 4000.0
        val moneyness = strike - spot

        // Calls: higher strike = lower price. Puts: higher strike = higher price.
        val mid = when (optionType) {
            "call" -> {
                val intrinsic = maxOf(spot - strike, 0.0)
                val timeValue = maxOf(10.0 - moneyness * 0.5, 0.5)
                intrinsic + timeValue
            }
            else -> {
                val intrinsic = maxOf(strike - spot, 0.0)
                val timeValue = maxOf(10.0 + moneyness * 0.5, 0.5)
                intrinsic + timeValue
            }
        }

        val spread = mid * 0.02
        val bid = maxOf(mid - spread, 0.05)
        val ask = mid + spread
        return OptionQuote(symbol, strike, LocalDate.parse(expiration), optionType, bid, ask)
    }

    /**
     * Scans strike pairs around spot to find the box spread with the lowest annualized rate.
     * Returns null if no valid pair found.
     */
    override suspend fun findBestBoxSpread(
        symbol: String,
        expiration: String,
        benchmarkRate: Double,
        maxPairs: Int
    ): BoxSpreadScan? {
        val spot = fetchSpot(symbol) ?: return null
        val exp = LocalDate.parse(expiration)
        val daysToExp = ChronoUnit.DAYS.between(LocalDate.now(), exp)
        if (daysToExp <= 0) return null

        val results = mutableListOf<BoxSpreadScan>()
        var pairsChecked = 0

        // K1 below spot, K2 above spot
        val k1Step = spot * 0.005
        val k2Step = spot * 0.005

        var k1 = spot * 0.85
        while (k1 < spot * 0.995 && pairsChecked < maxPairs) {
            var k2 = spot * 1.005
            while (k2 <= spot * 1.15 && pairsChecked < maxPairs) {
                pairsChecked++

                val callK1 = fetchQuote(symbol, k1, expiration, "call") ?: continue
                val callK2 = fetchQuote(symbol, k2, expiration, "call") ?: continue
                val putK1 = fetchQuote(symbol, k1, expiration, "put") ?: continue
                val putK2 = fetchQuote(symbol, k2, expiration, "put") ?: continue

                val width = k2 - k1
                val netCredit = (callK1.mid - callK2.mid) + (putK2.mid - putK1.mid)

                if (netCredit <= 0.0 || width <= 0.0) {
                    k2 += k2Step
                    continue
                }

                val impliedRate = width / netCredit
                val annualizedRate = impliedRate * (365.0 / daysToExp.toDouble())

                results.add(
                    BoxSpreadScan(
                        k1 = k1,
                        k2 = k2,
                        callK1 = callK1,
                        callK2 = callK2,
                        putK1 = putK1,
                        putK2 = putK2,
                        annualizedRate = annualizedRate,
                        netCredit = netCredit,
                        width = width,
                        isOpportunity = annualizedRate < benchmarkRate
                    )
                )
                k2 += k2Step
            }
            k1 += k1Step
        }

        return results.minByOrNull { it.annualizedRate }
    }
}
