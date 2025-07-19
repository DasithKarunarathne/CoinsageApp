package com.example.coinsage.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.coinsage.data.model.Transaction
import java.math.BigDecimal

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "CoinSagePrefs"
        private const val KEY_LOGGED_IN_USER = "logged_in_user"
        private const val KEY_USER_PREFIX = "user_"
        private const val KEY_TRANSACTIONS_PREFIX = "transactions_"
        private const val KEY_BUDGET_PREFIX = "budget_"
        private const val KEY_MONTHLY_BUDGET_PREFIX = "monthly_budget_"
        private const val KEY_CURRENCY_PREFIX = "currency_"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }

    // Authentication
    fun setLoggedInUser(email: String) {
        prefs.edit().putString(KEY_LOGGED_IN_USER, email).apply()
    }

    fun getLoggedInUser(): String? = prefs.getString(KEY_LOGGED_IN_USER, null)

    fun isLoggedIn(): Boolean = getLoggedInUser() != null

    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN_USER).apply()
    }

    // User Credentials
    fun saveCredentials(email: String, hashedPassword: String) {
        prefs.edit().putString("$KEY_USER_PREFIX$email", hashedPassword).apply()
    }

    fun getPassword(email: String): String? = prefs.getString("$KEY_USER_PREFIX$email", null)

    // Transactions
    fun saveTransactions(transactions: List<Transaction>) {
        val user = getLoggedInUser() ?: return
        val json = gson.toJson(transactions)
        prefs.edit().putString("$KEY_TRANSACTIONS_PREFIX$user", json).apply()
    }

    fun getTransactions(): List<Transaction> {
        val user = getLoggedInUser() ?: return emptyList()
        val json = prefs.getString("$KEY_TRANSACTIONS_PREFIX$user", null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Budget (Deprecated)
    @Deprecated("Use setMonthlyBudget instead", ReplaceWith("setMonthlyBudget(amount.toDouble())"))
    fun saveBudget(amount: BigDecimal) {
        val user = getLoggedInUser() ?: return
        prefs.edit().putString("$KEY_BUDGET_PREFIX$user", amount.toString()).apply()
    }

    @Deprecated("Use getMonthlyBudget instead", ReplaceWith("getMonthlyBudget()"))
    fun getBudget(): BigDecimal {
        val user = getLoggedInUser() ?: return BigDecimal.ZERO
        val budgetStr = prefs.getString("$KEY_BUDGET_PREFIX$user", "0")
        return try {
            BigDecimal(budgetStr)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    // Monthly Budget
    fun setMonthlyBudget(amount: Double) {
        val user = getLoggedInUser() ?: return
        prefs.edit().putFloat("$KEY_MONTHLY_BUDGET_PREFIX$user", amount.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        val user = getLoggedInUser() ?: return 0.0
        return prefs.getFloat("$KEY_MONTHLY_BUDGET_PREFIX$user", 0f).toDouble()
    }

    // Currency
    fun setCurrency(currency: String) {
        val user = getLoggedInUser() ?: return
        prefs.edit().putString("$KEY_CURRENCY_PREFIX$user", currency).apply()
    }

    fun getCurrency(): String {
        val user = getLoggedInUser() ?: return "USD"
        return prefs.getString("$KEY_CURRENCY_PREFIX$user", "USD") ?: "USD"
    }

    // Notifications
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    // Clear user data
    fun clearUserData() {
        val user = getLoggedInUser() ?: return
        prefs.edit().apply {
            remove("$KEY_TRANSACTIONS_PREFIX$user")
            remove("$KEY_BUDGET_PREFIX$user")
            remove("$KEY_MONTHLY_BUDGET_PREFIX$user")
            remove("$KEY_CURRENCY_PREFIX$user")
            apply()
        }
    }
} 