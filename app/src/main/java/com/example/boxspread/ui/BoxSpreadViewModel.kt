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
import java.time.LocalDate

class BoxSpreadViewModel : ViewModel() {

    private val quoteService: QuoteService = MockQuoteService

    // UI state
    var underlying by mutableStateOf("SPX")
        private set

    var expiration by mutableStateOf("")
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
                callK1Mid = callK1.mid,
                callK2Mid = callK2.mid,
                putK1Mid = putK1.mid,
                putK2Mid = putK2.mid,
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
}

private fun String.toDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        null
    }
}
