package com.example.coinsage.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.utils.PrefsHelper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsHelper = PrefsHelper(application)
    private val _categoryData = MutableLiveData<List<CategorySpending>>()
    val categoryData: LiveData<List<CategorySpending>> = _categoryData
    private var currentTimeFilter = TimeFilter.MONTH

    init {
        updateAnalysis()
    }

    fun setTimeFilter(filter: TimeFilter) {
        currentTimeFilter = filter
        updateAnalysis()
    }

    private fun updateAnalysis() {
        val transactions = prefsHelper.getTransactions()
        val filteredTransactions = filterTransactions(transactions)

        val totalExpenses = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val categorySpending = mutableMapOf<String, BigDecimal>()
        filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .forEach { transaction ->
                val currentAmount = categorySpending.getOrDefault(transaction.category.name, BigDecimal.ZERO)
                categorySpending[transaction.category.name] = currentAmount + transaction.amount
            }

        val categoryData = categorySpending.map { (category, amount) ->
            val percentage = if (totalExpenses > BigDecimal.ZERO) {
                amount.divide(totalExpenses, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            CategorySpending(category, amount, percentage.toDouble())
        }.sortedByDescending { it.amount }

        _categoryData.value = categoryData
    }

    private fun filterTransactions(transactions: List<Transaction>): List<Transaction> {
        val now = LocalDateTime.now()
        val startDateTime = when (currentTimeFilter) {
            TimeFilter.WEEK -> now.minus(7, ChronoUnit.DAYS)
            TimeFilter.MONTH -> now.minus(1, ChronoUnit.MONTHS)
            TimeFilter.YEAR -> now.minus(1, ChronoUnit.YEARS)
        }
        val startInstant = startDateTime.atZone(ZoneId.systemDefault()).toInstant()

        return transactions.filter { transaction ->
            Instant.ofEpochMilli(transaction.date.time).isAfter(startInstant)
        }
    }
}

enum class TimeFilter {
    WEEK, MONTH, YEAR
}

data class CategorySpending(
    val category: String,
    val amount: BigDecimal,
    val percentage: Double
) 