package com.example.coinsage.ui.budget

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.coinsage.utils.PrefsHelper

class BudgetViewModelFactory(
    private val application: Application,
    private val prefsHelper: PrefsHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(application, prefsHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 