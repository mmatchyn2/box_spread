package com.example.boxspread

import java.time.LocalDate

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
}

object MockQuoteService : QuoteService {
    override suspend fun fetchQuote(symbol: String, strike: Double, expiration: String, optionType: String): OptionQuote? {
        val mid = when (optionType) {
            "call" -> Math.max(100.0 - (strike - 4000.0) * 0.1, 1.0)
            else -> Math.max(5.0 + (strike - 4000.0) * 0.1, 1.0)
        }
        val bid = mid * 0.98
        val ask = mid * 1.02
        return OptionQuote(symbol, strike, LocalDate.parse(expiration), optionType, bid, ask)
    }
}
