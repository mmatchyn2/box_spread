package com.example.boxspread.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boxspread.BoxSpreadCalculator
import com.example.boxspread.BoxSpreadResult
import com.example.boxspread.MockQuoteService
import com.example.boxspread.QuoteService
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class BoxSpreadViewModel : ViewModel() {

    private val quoteService: QuoteService = MockQuoteService

    // UI state
    var underlying by mutableStateOf("SPX")
        private set

    var expiration by mutableStateOf(nextOptionExpiration().toString())
        private set

    var k1 by mutableStateOf("")
        private set

    var k2 by mutableStateOf("")
        private set

    var benchmarkRate by mutableStateOf("5.0")
        private set

    var result by mutableStateOf<BoxSpreadResult?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun updateUnderlying(value: String) {
        underlying = value
    }

    fun updateExpiration(value: String) {
        expiration = value
    }

    fun updateK1(value: String) {
        k1 = value
    }

    fun updateK2(value: String) {
        k2 = value
    }

    fun updateBenchmarkRate(value: String) {
        benchmarkRate = value
    }

    fun calculate() {
        error = null
        result = null
        isLoading = true

        viewModelScope.launch {
            val exp = expiration.toDateOrNull()
            if (exp == null) {
                error = "Please enter a valid expiration date (YYYY-MM-DD)."
                isLoading = false
                return@launch
            }

            val k1Val = k1.toDoubleOrNull()
            val k2Val = k2.toDoubleOrNull()
            val bench = benchmarkRate.toDoubleOrNull() ?: 0.0

            if (k1Val == null || k2Val == null || k1Val <= 0 || k2Val <= k1Val) {
                error = "Please enter valid strikes with 0 < K1 < K2."
                isLoading = false
                return@launch
            }

            val callK1 = quoteService.fetchQuote(underlying, k1Val, expiration, "call")
            val callK2 = quoteService.fetchQuote(underlying, k2Val, expiration, "call")
            val putK1 = quoteService.fetchQuote(underlying, k1Val, expiration, "put")
            val putK2 = quoteService.fetchQuote(underlying, k2Val, expiration, "put")

            if (callK1 == null || callK2 == null || putK1 == null || putK2 == null) {
                error = "Unable to fetch quotes for all legs. Check the underlying and expiration."
                isLoading = false
                return@launch
            }

            val calcResult = BoxSpreadCalculator.calculate(
                underlying = underlying,
                expiration = exp,
                k1 = k1Val,
                k2 = k2Val,
                callK1Bid = callK1.bid,
                callK1Ask = callK1.ask,
                callK2Bid = callK2.bid,
                callK2Ask = callK2.ask,
                putK1Bid = putK1.bid,
                putK1Ask = putK1.ask,
                putK2Bid = putK2.bid,
                putK2Ask = putK2.ask,
                benchmarkRate = bench
            )

            if (!calcResult.isValid) {
                error = calcResult.error
            } else {
                result = calcResult
            }
            isLoading = false
        }
    }

    fun clearResults() {
        result = null
        error = null
    }

    fun findBest() {
        error = null
        result = null
        isLoading = true

        viewModelScope.launch {
            val exp = expiration.toDateOrNull()
            if (exp == null) {
                error = "Please enter a valid expiration date (YYYY-MM-DD)."
                isLoading = false
                return@launch
            }

            val bench = benchmarkRate.toDoubleOrNull() ?: 0.0
            val scan = quoteService.findBestBoxSpread(underlying, expiration, bench)

            if (scan == null) {
                error = "Unable to find a valid box spread for ${underlying} expiring $expiration."
                isLoading = false
                return@launch
            }

            // Set the strike fields so the user sees what was selected
            k1 = scan.k1.toString()
            k2 = scan.k2.toString()

            val calcResult = BoxSpreadCalculator.calculate(
                underlying = underlying,
                expiration = exp,
                k1 = scan.k1,
                k2 = scan.k2,
                callK1Bid = scan.callK1.bid,
                callK1Ask = scan.callK1.ask,
                callK2Bid = scan.callK2.bid,
                callK2Ask = scan.callK2.ask,
                putK1Bid = scan.putK1.bid,
                putK1Ask = scan.putK1.ask,
                putK2Bid = scan.putK2.bid,
                putK2Ask = scan.putK2.ask,
                benchmarkRate = bench
            )

            if (!calcResult.isValid) {
                error = calcResult.error
            } else {
                result = calcResult
            }
            isLoading = false
        }
    }
}

private fun String.toDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        null
    }
}

/**
 * Returns the next actual monthly option expiration date (third Friday) that is
 * at least 60 days in the future. US index options (SPX, NDX, VIX) expire on the
 * third Friday of each month.
 */
private fun nextOptionExpiration(): LocalDate {
    val minDate = LocalDate.now().plusDays(60)
    var search = minDate
    // Scan forward up to 12 months for a valid third-Friday expiration
    repeat(12) {
        val candidate = thirdFriday(search.year, search.month)
        if (candidate >= minDate) {
            return candidate
        }
        search = search.plusMonths(1)
    }
    return minDate // fallback if no third Friday found in range
}

/**
 * Returns the third Friday of the given month (standard US monthly option expiration).
 */
private fun thirdFriday(year: Int, month: Month): LocalDate {
    val firstOfMonth = LocalDate.of(year, month, 1)
    val daysToFirstFriday = (DayOfWeek.FRIDAY.ordinal - firstOfMonth.dayOfWeek.ordinal + 7) % 7
    val firstFriday = firstOfMonth.plusDays(daysToFirstFriday.toLong())
    return firstFriday.plusWeeks(2) // third Friday
}
