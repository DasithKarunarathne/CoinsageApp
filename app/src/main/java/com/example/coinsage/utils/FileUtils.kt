package com.example.coinsage.utils

import android.content.Context
import com.example.coinsage.data.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object FileUtils {
    private const val BACKUP_FILE = "transactions_backup.json"
    private val gson = Gson()

    fun exportTransactions(context: Context, transactions: List<Transaction>): Boolean {
        return try {
            val json = gson.toJson(transactions)
            context.openFileOutput(BACKUP_FILE, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importTransactions(context: Context): List<Transaction>? {
        return try {
            val file = File(context.filesDir, BACKUP_FILE)
            if (!file.exists()) return null
            val json = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}