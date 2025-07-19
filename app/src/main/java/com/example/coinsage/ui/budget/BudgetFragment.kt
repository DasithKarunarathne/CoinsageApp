package com.example.coinsage.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.coinsage.R
import com.example.coinsage.databinding.FragmentBudgetBinding
import com.example.coinsage.utils.PrefsHelper
import com.example.coinsage.ui.settings.SettingsFragment
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsHelper: PrefsHelper
    private val viewModel: BudgetViewModel by activityViewModels {
        BudgetViewModelFactory(requireActivity().application, PrefsHelper(requireContext()))
    }

    companion object {
        private const val WARNING_THRESHOLD = 80 // Show warning at 80% of budget
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefsHelper = PrefsHelper(requireContext())
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        observeCurrencyChanges()
    }

    private fun setupViews() {
        viewModel.currentBudget.observe(viewLifecycleOwner) { budget ->
            if (budget > BigDecimal.ZERO) {
                binding.etBudgetAmount.setText(budget.toString())
            }
        }

        binding.btnSaveBudget.setOnClickListener {
            val budgetText = binding.etBudgetAmount.text.toString()
            if (budgetText.isNotEmpty()) {
                try {
                    val budget = BigDecimal(budgetText)
                    if (budget > BigDecimal.ZERO) {
                        viewModel.updateBudget(budget)
                        Toast.makeText(requireContext(), R.string.success_budget_set, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), R.string.error_field_required, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.budgetProgress.observe(viewLifecycleOwner) { progress ->
            updateBudgetProgress(progress)
        }

        viewModel.currentBudget.observe(viewLifecycleOwner) { budget ->
            updateBudgetDisplay(budget)
            // Recalculate progress when budget changes
            viewModel.calculateBudgetProgress()
        }

        // Observe transaction changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactionChanges.collect {
                    // Recalculate progress when transactions change
                    viewModel.calculateBudgetProgress()
                }
            }
        }
    }

    private fun observeCurrencyChanges() {
        SettingsFragment.currencyChanged.observe(viewLifecycleOwner) { _ ->
            viewModel.currentBudget.value?.let { budget ->
                updateBudgetDisplay(budget)
            }
        }
    }

    private fun updateBudgetProgress(progress: BigDecimal) {
        val progressInt = progress.toInt().coerceIn(0, 100)
        binding.progressBudget.progress = progressInt

        // Update progress bar color based on progress
        val progressColor = when {
            progressInt >= 100 -> R.color.error_red
            progressInt >= WARNING_THRESHOLD -> R.color.warning_yellow
            else -> R.color.primary
        }
        binding.progressBudget.progressTintList = resources.getColorStateList(progressColor, null)

        viewModel.currentBudget.value?.let { budget ->
            updateBudgetDisplay(budget, progress)
            updateWarningMessage(progressInt)
        }
    }

    private fun updateWarningMessage(progress: Int) {
        with(binding) {
            when {
                progress >= 100 -> {
                    layoutBudgetWarning.visibility = View.VISIBLE
                    tvBudgetWarning.text = getString(R.string.budget_warning_exceeded)
                    layoutBudgetWarning.setBackgroundResource(R.drawable.rounded_warning_background)
                    tvBudgetWarning.setTextColor(resources.getColor(R.color.error_red, null))
                }
                progress >= WARNING_THRESHOLD -> {
                    layoutBudgetWarning.visibility = View.VISIBLE
                    tvBudgetWarning.text = getString(R.string.budget_warning_near_limit)
                    layoutBudgetWarning.setBackgroundResource(R.drawable.rounded_warning_background)
                    tvBudgetWarning.setTextColor(resources.getColor(R.color.warning_yellow, null))
                }
                else -> {
                    layoutBudgetWarning.visibility = View.GONE
                }
            }
        }
    }

    private fun updateBudgetDisplay(budget: BigDecimal, progress: BigDecimal = BigDecimal.ZERO) {
        val currencyCode = prefsHelper.getCurrency()
        val numberFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currencyCode)
        }

        val spent = budget.multiply(progress).divide(BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP)
        
        binding.tvBudgetProgressText.text = String.format(
            getString(R.string.budget_progress_format),
            numberFormat.format(spent),
            numberFormat.format(budget)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}