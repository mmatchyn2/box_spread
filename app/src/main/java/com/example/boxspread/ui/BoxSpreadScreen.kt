package com.example.boxspread.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boxspread.BoxSpreadResult
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxSpreadScreen(modifier: Modifier = Modifier) {
    val viewModel: BoxSpreadViewModel = viewModel()

    val dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
    val percentFormat = NumberFormat.getPercentInstance()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Box Spread Finder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Underlying input
        OutlinedTextField(
            value = viewModel.underlying,
            onValueChange = { newValue: String -> viewModel.updateUnderlying(newValue) },
            label = { Text("Underlying (index)") },
            placeholder = { Text("e.g. SPX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expiration input
        OutlinedTextField(
            value = viewModel.expiration,
            onValueChange = { newValue: String -> viewModel.updateExpiration(newValue) },
            label = { Text("Expiration (YYYY-MM-DD)") },
            placeholder = { Text("e.g. 2024-12-20") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lower strike K1
        OutlinedTextField(
            value = viewModel.k1,
            onValueChange = { newValue: String -> viewModel.updateK1(newValue) },
            label = { Text("Lower strike K1") },
            placeholder = { Text("e.g. 4500") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upper strike K2
        OutlinedTextField(
            value = viewModel.k2,
            onValueChange = { newValue: String -> viewModel.updateK2(newValue) },
            label = { Text("Upper strike K2") },
            placeholder = { Text("e.g. 4600") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Benchmark rate
        OutlinedTextField(
            value = viewModel.benchmarkRate,
            onValueChange = { newValue: String -> viewModel.updateBenchmarkRate(newValue) },
            label = { Text("Benchmark rate (%)") },
            placeholder = { Text("e.g. 5.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Calculate button
        Button(
            onClick = { viewModel.calculate() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Calculate Box Spread")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        viewModel.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        // Results section
        viewModel.result?.let { result ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ResultRow("Underlying", result.underlying)
                ResultRow("Expiration", result.expiration.format(dateFormatter))
                ResultRow("Strikes", "${result.k1} / ${result.k2}")
                ResultRow("Payoff width (K2 − K1)", result.width.formatCurrency())
                ResultRow("Net debit", result.netDebit.formatCurrency())
                ResultRow("Implied rate", "${result.impliedRate * 100.0}%")
                ResultRow("Annualized rate", "${result.annualizedRate * 100.0}%")
                ResultRow("Days to expiration", result.daysToExpiration.toString())

                // Leg quotes: bid / ask for all four options
                Text(
                    text = "Leg Quotes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                ResultRow("Call K1 bid / ask", "${result.callK1Bid.formatCurrency()} / ${result.callK1Ask.formatCurrency()}")
                ResultRow("Call K2 bid / ask", "${result.callK2Bid.formatCurrency()} / ${result.callK2Ask.formatCurrency()}")
                ResultRow("Put K1 bid / ask", "${result.putK1Bid.formatCurrency()} / ${result.putK1Ask.formatCurrency()}")
                ResultRow("Put K2 bid / ask", "${result.putK2Bid.formatCurrency()} / ${result.putK2Ask.formatCurrency()}")

                // What is needed to reach the benchmark rate
                if (result.benchmarkRate > 0.0 && result.daysToExpiration > 0) {
                    Text(
                        text = "To reach benchmark rate (${result.benchmarkRate * 100.0}%)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    ResultRow("Needed net debit", result.neededNetDebitForBenchmark.formatCurrency())
                    val adjustment = result.debitAdjustmentForBenchmark
                    if (adjustment > 0.0) {
                        ResultRow("Debit must come down by", adjustment.formatCurrency())
                    } else if (adjustment < 0.0) {
                        ResultRow("Debit can rise by", (-adjustment).formatCurrency())
                    } else {
                        ResultRow("Debit adjustment", "0.00")
                    }
                }

                if (result.isOpportunity) {
                    Text(
                        text = "OPPORTUNITY: annualized rate above benchmark",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
                    )
                } else {
                    Text(
                        text = "Below benchmark rate — not an opportunity at current quotes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // European-style disclaimer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "European-style disclaimer",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Most free quote APIs do not reliably expose whether a contract is American or European style. " +
                    "US equity/ETF options are typically American-style; index options (SPX, NDX, VIX) are European-style " +
                    "and cash-settled. Treat European-style as a user-controlled filter: select index underlyings only. " +
                    "This app does not verify exercise style from the API.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

private fun Double.formatCurrency(): String {
    return NumberFormat.getCurrencyInstance().format(this)
}
