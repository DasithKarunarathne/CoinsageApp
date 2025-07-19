package com.example.coinsage.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coinsage.R
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.databinding.FragmentHomeBinding
import com.example.coinsage.ui.budget.BudgetViewModel
import com.example.coinsage.ui.budget.BudgetViewModelFactory
import com.example.coinsage.ui.transactions.AddTransactionDialog
import com.example.coinsage.ui.transactions.TransactionAdapter
import com.example.coinsage.ui.transactions.DeleteConfirmationDialog
import com.example.coinsage.utils.PrefsHelper
import com.google.android.material.snackbar.Snackbar
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsHelper: PrefsHelper
    private lateinit var transactionAdapter: TransactionAdapter
    private val budgetViewModel: BudgetViewModel by activityViewModels {
        BudgetViewModelFactory(requireActivity().application, PrefsHelper(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefsHelper = PrefsHelper(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        setupUserGreeting()
        setupBalanceCard()
        setupIncomeExpensesCard()
        setupBudgetProgress()
        setupTransactionsList()
        setupFabActions()
        observeViewModel()
    }

    private fun observeViewModel() {
        budgetViewModel.budgetProgress.observe(viewLifecycleOwner) { progress ->
            updateBudgetProgress(progress)
        }

        budgetViewModel.currentBudget.observe(viewLifecycleOwner) { budget ->
            updateBudgetDisplay(budget)
        }

        // Observe transaction changes as a backup
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                budgetViewModel.transactionChanges.collect {
                    // Update all relevant UI components
                    setupBalanceCard()
                    setupIncomeExpensesCard()
                    updateTransactionsList()
                    budgetViewModel.calculateBudgetProgress()
                }
            }
        }
    }

    private fun setupUserGreeting() {
        val username = prefsHelper.getLoggedInUser()?.substringBefore('@') ?: "User"
        binding.tvGreeting.text = getString(R.string.welcome_message, username)
    }

    private fun setupBalanceCard() {
        val transactions = prefsHelper.getTransactions()
        val balance = calculateBalance(transactions)
        val currency = prefsHelper.getCurrency()
        val formattedBalance = formatCurrency(balance, currency)
        
        binding.tvBalance.text = formattedBalance
        binding.tvBalanceLabel.text = getString(R.string.current_balance)
    }

    private fun setupIncomeExpensesCard() {
        val transactions = prefsHelper.getTransactions()
        val currency = prefsHelper.getCurrency()
        
        val totalIncome = calculateTotalIncome(transactions)
        val totalExpenses = calculateTotalExpenses(transactions)
        
        binding.tvTotalIncome.text = formatCurrency(totalIncome, currency)
        binding.tvTotalExpenses.text = formatCurrency(totalExpenses, currency)
    }

    private fun setupBudgetProgress() {
        // Initial setup of the progress bar colors
        binding.progressBudget.progressTintList = resources.getColorStateList(R.color.success_green, null)
    }

    private fun updateBudgetProgress(progress: BigDecimal) {
        val progressInt = progress.toInt().coerceIn(0, 100)
        binding.progressBudget.progress = progressInt

        // Update progress bar color based on percentage
        binding.progressBudget.progressTintList = when {
            progressInt >= 100 -> resources.getColorStateList(R.color.error_red, null)
            progressInt >= 80 -> resources.getColorStateList(R.color.warning_yellow, null)
            else -> resources.getColorStateList(R.color.success_green, null)
        }
    }

    private fun updateBudgetDisplay(budget: BigDecimal) {
        val transactions = prefsHelper.getTransactions()
        val currentMonth = java.time.YearMonth.now()
        
        // Calculate current month's expenses
        val currentMonthExpenses = transactions
            .filter { transaction -> 
                val transactionMonth = java.time.YearMonth.from(
                    java.time.Instant.ofEpochMilli(transaction.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                )
                transaction.type == TransactionType.EXPENSE && transactionMonth == currentMonth
            }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }

        val currencyCode = prefsHelper.getCurrency()
        val numberFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currencyCode)
        }

        binding.tvBudgetProgress.text = getString(
            R.string.budget_progress_format,
            numberFormat.format(currentMonthExpenses),
            numberFormat.format(budget)
        )
    }

    private fun setupTransactionsList() {
        transactionAdapter = TransactionAdapter(
            currencyCode = prefsHelper.getCurrency(),
            onEditClick = { transaction ->
                showEditTransactionDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }

        updateTransactionsList()
    }

    private fun updateTransactionsList() {
        val recentTransactions = prefsHelper.getTransactions()
            .sortedByDescending { it.date }
            .take(5)
        
        transactionAdapter.submitList(recentTransactions)
    }

    private fun setupFabActions() {
        binding.fabAddIncome.setOnClickListener {
            showAddTransactionDialog(TransactionType.INCOME)
        }

        binding.fabAddExpense.setOnClickListener {
            showAddTransactionDialog(TransactionType.EXPENSE)
        }
    }

    private fun showAddTransactionDialog(type: TransactionType) {
        AddTransactionDialog.newInstance(
            transactionType = type,
            existingTransaction = null,
            onTransactionAdded = { transaction ->
                // Save transaction
                val transactions = prefsHelper.getTransactions().toMutableList()
                transactions.add(transaction)
                prefsHelper.saveTransactions(transactions)

                // Update UI immediately
                updateTransactionsList()
                setupBalanceCard()
                setupIncomeExpensesCard()
                
                // Update budget and check notifications immediately
                budgetViewModel.calculateBudgetProgress()

                // Show success message
                Snackbar.make(
                    binding.root,
                    getString(R.string.success_transaction_added),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        ).show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        AddTransactionDialog.newInstance(
            transactionType = transaction.type,
            existingTransaction = transaction,
            onTransactionAdded = { updatedTransaction ->
                // Update transaction in the list
                val transactions = prefsHelper.getTransactions().toMutableList()
                val index = transactions.indexOfFirst { it.id == transaction.id }
                if (index != -1) {
                    transactions[index] = updatedTransaction
                    prefsHelper.saveTransactions(transactions)
                    
                    // Update UI immediately
                    updateTransactionsList()
                    setupBalanceCard()
                    setupIncomeExpensesCard()
                    
                    // Update budget and check notifications immediately
                    budgetViewModel.calculateBudgetProgress()

                    Snackbar.make(
                        binding.root,
                        getString(R.string.success_transaction_updated),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        ).show(childFragmentManager, "EditTransactionDialog")
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete transaction from the list
                val transactions = prefsHelper.getTransactions().toMutableList()
                transactions.removeAll { it.id == transaction.id }
                prefsHelper.saveTransactions(transactions)
                
                // Update UI immediately
                updateTransactionsList()
                setupBalanceCard()
                setupIncomeExpensesCard()
                
                // Update budget and check notifications immediately
                budgetViewModel.calculateBudgetProgress()

                Snackbar.make(
                    binding.root,
                    getString(R.string.success_transaction_deleted),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateBalance(transactions: List<Transaction>): BigDecimal {
        return transactions.fold(BigDecimal.ZERO) { acc, transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> acc + transaction.amount
                TransactionType.EXPENSE -> acc - transaction.amount
            }
        }
    }

    private fun calculateTotalIncome(transactions: List<Transaction>): BigDecimal {
        return transactions
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
    }

    private fun calculateTotalExpenses(transactions: List<Transaction>): BigDecimal {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
    }

    private fun formatCurrency(amount: BigDecimal, currencyCode: String): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        return format.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 