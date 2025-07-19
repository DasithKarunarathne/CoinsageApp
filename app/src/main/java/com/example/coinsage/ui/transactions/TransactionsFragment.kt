package com.example.coinsage.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coinsage.R
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.data.model.Category
import com.example.coinsage.databinding.FragmentTransactionsBinding
import com.example.coinsage.utils.PrefsHelper
import com.google.android.material.snackbar.Snackbar

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsHelper: PrefsHelper
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions: List<Transaction> = emptyList()
    private var lastAddedTransactionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        setupTransactionList()
        setupFabAction()
        setupSwipeToDelete()
        setupFilters()
    }

    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter(
            currencyCode = prefsHelper.getCurrency(),
            onEditClick = { transaction ->
                showEditTransactionDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }

        updateTransactionList()
    }

    private fun updateTransactionList(
        filterCategory: String? = null,
        sortBy: String? = null
    ) {
        transactions = prefsHelper.getTransactions()
        var filteredTransactions = transactions

        // Apply category filter
        if (filterCategory != null && filterCategory != "All") {
            val category = when (filterCategory.lowercase()) {
                "food" -> Category.FOOD
                "transport" -> Category.TRANSPORT
                "bills" -> Category.BILLS
                "entertainment" -> Category.ENTERTAINMENT
                "income" -> Category.INCOME
                else -> null
            }
            if (category != null) {
                filteredTransactions = filteredTransactions.filter { it.category == category }
            }
        }

        // Apply sorting
        filteredTransactions = when (sortBy) {
            "Date (Newest)" -> {
                // Only sort by date for newest transactions
                filteredTransactions.sortedByDescending { it.date }
            }
            "Date (Oldest)" -> {
                filteredTransactions.sortedWith(
                    compareBy<Transaction> { it.date }
                        .thenBy { it.id }
                )
            }
            "Amount (High to Low)" -> {
                filteredTransactions.sortedWith(
                    compareByDescending<Transaction> { it.amount }
                        .thenByDescending { it.date }
                        .thenByDescending { it.id }
                )
            }
            "Amount (Low to High)" -> {
                filteredTransactions.sortedWith(
                    compareBy<Transaction> { it.amount }
                        .thenByDescending { it.date }
                        .thenByDescending { it.id }
                )
            }
            else -> {
                // Default sorting is also by date only
                filteredTransactions.sortedByDescending { it.date }
            }
        }

        transactionAdapter.submitList(filteredTransactions)
    }

    private fun setupFabAction() {
        binding.fabAddTransaction.setOnClickListener {
            AddTransactionDialog.newInstance(TransactionType.EXPENSE) { transaction ->
                val transactions = prefsHelper.getTransactions().toMutableList()
                transactions.add(transaction)
                prefsHelper.saveTransactions(transactions)
                lastAddedTransactionId = transaction.id
                updateTransactionList(binding.spinnerFilterCategory.selectedItem?.toString(), 
                    binding.spinnerSort.selectedItem?.toString())
                Snackbar.make(binding.root, R.string.success_transaction_added, Snackbar.LENGTH_SHORT).show()
            }.show(childFragmentManager, "AddTransactionDialog")
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionAdapter.currentList[position]
                val transactions = prefsHelper.getTransactions().toMutableList()
                transactions.remove(transaction)
                prefsHelper.saveTransactions(transactions)
                updateTransactionList()

                Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        transactions.add(transaction)
                        prefsHelper.saveTransactions(transactions)
                        updateTransactionList()
                    }.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)
    }

    private fun setupFilters() {
        binding.spinnerFilterCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val category = parent.getItemAtPosition(position).toString()
                val sort = binding.spinnerSort.selectedItem?.toString()
                updateTransactionList(filterCategory = category, sortBy = sort)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val sort = parent.getItemAtPosition(position).toString()
                val category = binding.spinnerFilterCategory.selectedItem?.toString()
                updateTransactionList(filterCategory = category, sortBy = sort)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        AddTransactionDialog.newInstance(
            transactionType = transaction.type,
            existingTransaction = transaction
        ) { updatedTransaction ->
            val transactions = prefsHelper.getTransactions().toMutableList()
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                transactions[index] = updatedTransaction.copy(id = transaction.id)
                prefsHelper.saveTransactions(transactions)
                updateTransactionList()
                Snackbar.make(binding.root, R.string.success_transaction_updated, Snackbar.LENGTH_SHORT).show()
            }
        }.show(childFragmentManager, "EditTransactionDialog")
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        DeleteConfirmationDialog.newInstance {
            val transactions = prefsHelper.getTransactions().toMutableList()
            transactions.remove(transaction)
            prefsHelper.saveTransactions(transactions)
            updateTransactionList()
            Snackbar.make(binding.root, R.string.success_transaction_deleted, Snackbar.LENGTH_SHORT).show()
        }.show(childFragmentManager, "DeleteConfirmationDialog")
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list with current filters when returning to the fragment
        updateTransactionList(
            binding.spinnerFilterCategory.selectedItem?.toString(),
            binding.spinnerSort.selectedItem?.toString()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}