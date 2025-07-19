package com.example.coinsage.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.coinsage.utils.PrefsHelper
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.utils.NotificationHelper
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetViewModel(
    application: Application,
    private val prefsHelper: PrefsHelper
) : AndroidViewModel(application) {
    private val notificationHelper = NotificationHelper(application)
    private val _currentBudget = MutableLiveData<BigDecimal>()
    val currentBudget: LiveData<BigDecimal> = _currentBudget

    private val _budgetProgress = MutableLiveData<BigDecimal>()
    val budgetProgress: LiveData<BigDecimal> = _budgetProgress

    private val _transactionChanges = MutableStateFlow(Unit)
    val transactionChanges: StateFlow<Unit> = _transactionChanges.asStateFlow()

    init {
        loadCurrentBudget()
    }

    fun notifyTransactionChanged() {
        _transactionChanges.value = Unit
    }

    fun removeTransaction(transaction: Transaction) {
        val transactions = prefsHelper.getTransactions().toMutableList()
        transactions.removeAll { it.id == transaction.id }
        prefsHelper.saveTransactions(transactions)
        calculateBudgetProgress()
        checkAndSendNotifications()
    }

    fun checkAndSendNotifications() {
        val transactions = prefsHelper.getTransactions()
        val currentMonth = java.time.YearMonth.now()
        
        // Filter transactions for current month only
        val currentMonthExpenses = transactions
            .filter { transaction -> 
                val transactionMonth = java.time.YearMonth.from(
                    java.time.Instant.ofEpochMilli(transaction.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                )
                transaction.type == TransactionType.EXPENSE && transactionMonth == currentMonth
            }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
        
        val budget = _currentBudget.value ?: BigDecimal.ZERO
        
        // Check if expenses exceed budget
        if (currentMonthExpenses > budget && budget > BigDecimal.ZERO) {
            notificationHelper.checkBudgetThreshold(currentMonthExpenses, budget)
        }
    }

    private fun loadCurrentBudget() {
        _currentBudget.value = BigDecimal(prefsHelper.getMonthlyBudget().toString())
        calculateBudgetProgress()
    }

    fun updateBudget(newBudget: BigDecimal) {
        prefsHelper.setMonthlyBudget(newBudget.toDouble())
        _currentBudget.value = newBudget
        notificationHelper.resetNotifications() // Reset notifications when budget is updated
        calculateBudgetProgress()
    }

    fun calculateBudgetProgress() {
        val transactions = prefsHelper.getTransactions()
        val currentMonth = java.time.YearMonth.now()
        
        // Filter transactions for current month only
        val currentMonthExpenses = transactions
            .filter { transaction -> 
                val transactionMonth = java.time.YearMonth.from(
                    java.time.Instant.ofEpochMilli(transaction.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                )
                transaction.type == TransactionType.EXPENSE && transactionMonth == currentMonth
            }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
        
        val budget = _currentBudget.value ?: BigDecimal.ZERO
        
        val progress = if (budget > BigDecimal.ZERO) {
            (currentMonthExpenses.multiply(BigDecimal(100))).divide(budget, 2, BigDecimal.ROUND_HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        _budgetProgress.value = progress
        
        // Always check and send notifications when budget progress is calculated
        checkAndSendNotifications()
    }
} 