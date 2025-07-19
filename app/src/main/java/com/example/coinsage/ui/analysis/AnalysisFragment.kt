package com.example.coinsage.ui.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coinsage.R
import com.example.coinsage.data.model.Transaction
import com.example.coinsage.data.model.TransactionType
import com.example.coinsage.data.model.Category
import com.example.coinsage.databinding.FragmentAnalysisBinding
import com.example.coinsage.databinding.ItemCategoryAnalysisBinding
import com.example.coinsage.utils.PrefsHelper
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.animation.Easing
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsHelper: PrefsHelper
    private val viewModel: AnalysisViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAnalysisAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())
        setupViews()
        setupPieChart()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupViews() {
        binding.spinnerTimeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.setTimeFilter(TimeFilter.WEEK)
                    1 -> viewModel.setTimeFilter(TimeFilter.MONTH)
                    2 -> viewModel.setTimeFilter(TimeFilter.YEAR)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPieChart() {
        binding.pieChartSpending.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(8f, 8f, 8f, 8f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                textSize = 12f
                textColor = Color.WHITE
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
                xOffset = 0f
                formSize = 12f
                formToTextSpace = 5f
            }
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAnalysisAdapter(prefsHelper.getCurrency())
        binding.rvCategoryAnalysis.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.categoryData.observe(viewLifecycleOwner) { categoryData ->
            updatePieChart(categoryData)
            categoryAdapter.submitList(categoryData)
        }
    }

    private fun updatePieChart(categoryData: List<CategorySpending>) {
        val entries = mutableListOf<PieEntry>()
        categoryData.forEach { category ->
            entries.add(PieEntry(category.percentage.toFloat(), category.category))
        }

        val colors = listOf(
            Color.parseColor("#FF6B6B"),  // Coral Red
            Color.parseColor("#4ECDC4"),  // Turquoise
            Color.parseColor("#45B7D1"),  // Sky Blue
            Color.parseColor("#96CEB4"),  // Sage Green
            Color.parseColor("#FFEEAD"),  // Cream Yellow
            Color.parseColor("#D4A5A5"),  // Dusty Rose
            Color.parseColor("#9FA4C4"),  // Lavender Gray
            Color.parseColor("#B5EAD7")   // Mint Green
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.pieChartSpending)
            valueLineColor = Color.WHITE
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            valueLineWidth = 2f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            sliceSpace = 2f
            selectionShift = 8f
            isValueLineVariableLength = true
        }

        val pieData = PieData(dataSet).apply {
            setValueTextSize(14f)
            setValueTextColor(Color.WHITE)
            setValueFormatter(PercentFormatter(binding.pieChartSpending))
        }

        binding.pieChartSpending.apply {
            data = pieData
            highlightValues(null)
            invalidate()
        }
    }

    private fun filterTransactionsByTime(transactions: List<Transaction>, timeFilter: String): List<Transaction> {
        val currentMonth = java.time.YearMonth.now()
        return when (timeFilter) {
            "Current Month" -> transactions.filter { transaction ->
                val transactionMonth = java.time.YearMonth.from(
                    java.time.Instant.ofEpochMilli(transaction.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                )
                transactionMonth == currentMonth
            }
            "Last Month" -> transactions.filter { transaction ->
                val transactionMonth = java.time.YearMonth.from(
                    java.time.Instant.ofEpochMilli(transaction.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                )
                transactionMonth == currentMonth.minusMonths(1)
            }
            else -> transactions
        }
    }

    private fun formatCurrency(amount: BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(prefsHelper.getCurrency())
        return format.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class CategoryAnalysisItem(
    val category: String,
    val amount: BigDecimal,
    val percentage: Float
)

class CategoryAnalysisAdapter(private val currency: String) : RecyclerView.Adapter<CategoryAnalysisAdapter.ViewHolder>() {
    private var items: List<CategorySpending> = emptyList()

    fun submitList(newItems: List<CategorySpending>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemCategoryAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategorySpending) {
            binding.tvCategory.apply {
                text = item.category
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            binding.tvAmount.apply {
                text = formatCurrency(item.amount)
                setTextColor(Color.WHITE)
                textSize = 16f
            }
            binding.tvPercentage.apply {
                text = String.format("%.1f%%", item.percentage)
                setTextColor(Color.WHITE)
                textSize = 14f
            }
        }
    }

    private fun formatCurrency(amount: BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currency)
        return format.format(amount)
    }
}