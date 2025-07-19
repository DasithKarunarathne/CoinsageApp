package com.example.coinsage.ui.transactions

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.coinsage.R
import com.example.coinsage.data.model.Category
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.databinding.DialogAddTransactionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialog : DialogFragment() {
    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!
    private var transactionType: TransactionType = TransactionType.EXPENSE // Default value
    private var existingTransaction: Transaction? = null
    private var onTransactionAdded: ((Transaction) -> Unit)? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddTransactionBinding.inflate(LayoutInflater.from(context))

        setupCategoryDropdown()
        setupInitialValues()
        setupDatePicker()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingTransaction != null) R.string.edit_transaction else R.string.add_transaction)
            .setView(binding.root)
            .create()
    }

    private fun setupCategoryDropdown() {
        val categories = when (transactionType) {
            TransactionType.EXPENSE -> listOf(
                Category.FOOD,
                Category.TRANSPORT,
                Category.BILLS,
                Category.ENTERTAINMENT,
                Category.OTHER
            )
            TransactionType.INCOME -> listOf(Category.INCOME)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_item,
            categories.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }
        )

        binding.spinnerCategory.apply {
            setAdapter(adapter)
            setDropDownBackgroundResource(R.color.background_dark)
            tag = categories // Store categories in the AutoCompleteTextView's tag
        }
    }

    private fun setupInitialValues() {
        existingTransaction?.let { transaction ->
            binding.etTitle.setText(transaction.title)
            binding.etAmount.setText(transaction.amount.toString())
            binding.etDate.setText(dateFormatter.format(transaction.date))
            binding.etNotes.setText(transaction.notes ?: "") // Assuming Transaction has a notes field

            // Get the stored categories list from tag
            @Suppress("UNCHECKED_CAST")
            val categories = binding.spinnerCategory.tag as? List<Category> ?: return

            // Set the category text
            val categoryName = transaction.category.name.lowercase().replaceFirstChar { it.uppercase() }
            binding.spinnerCategory.setText(categoryName, false)
        }
    }

    private fun setupDatePicker() {
        // Set current date as default if no date is set
        if (binding.etDate.text.isNullOrEmpty()) {
            binding.etDate.setText(dateFormatter.format(Date()))
        }

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            
            // Parse existing date if any
            binding.etDate.text.toString().takeIf { it.isNotEmpty() }?.let { dateStr ->
                try {
                    calendar.time = dateFormatter.parse(dateStr) ?: Date()
                } catch (e: Exception) {
                    calendar.time = Date()
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    binding.etDate.setText(dateFormatter.format(calendar.time))
                },
                year,
                month,
                day
            )

            // Set max date to today
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString()
        val amountStr = binding.etAmount.text.toString()
        val dateStr = binding.etDate.text.toString()
        val notes = binding.etNotes.text.toString()

        // Get the stored categories list
        @Suppress("UNCHECKED_CAST")
        val categories = binding.spinnerCategory.tag as? List<Category>
        val selectedCategoryName = binding.spinnerCategory.text.toString()
        val category = categories?.find {
            it.name.lowercase().replaceFirstChar { it.uppercase() } == selectedCategoryName
        } ?: Category.OTHER

        // Validate inputs
        if (title.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty() || selectedCategoryName.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val amount = BigDecimal(amountStr).takeIf { it > BigDecimal.ZERO }
                ?: throw IllegalArgumentException("Amount must be positive")
            val date = dateFormatter.parse(dateStr) ?: throw IllegalArgumentException("Invalid date format")

            val transaction = Transaction(
                id = existingTransaction?.id ?: UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                date = date,
                category = category,
                type = transactionType,
                notes = notes.ifEmpty { null } // Store null if notes are empty
            )

            onTransactionAdded?.invoke(transaction)
            dismiss() // Close dialog on success
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message ?: "Invalid input"}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            transactionType: TransactionType,
            existingTransaction: Transaction? = null,
            onTransactionAdded: (Transaction) -> Unit
        ): AddTransactionDialog {
            return AddTransactionDialog().apply {
                this.transactionType = transactionType
                this.existingTransaction = existingTransaction
                this.onTransactionAdded = onTransactionAdded
            }
        }
    }
}