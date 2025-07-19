package com.example.coinsage.utils

import android.content.Context
import com.example.coinsage.data.model.Transaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val backupFileName = "coinsage_backup.json"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    fun backupData(transactions: List<Transaction>): Result<String> {
        return try {
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "backup_${timestamp}.json")

            val jsonData = gson.toJson(transactions)
            backupFile.writeText(jsonData)
            
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(IOException("Failed to backup data: ${e.message}"))
        }
    }

    fun restoreData(backupFile: File): Result<List<Transaction>> {
        return try {
            val jsonData = backupFile.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(jsonData, type)
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(IOException("Failed to restore data: ${e.message}"))
        }
    }

    fun getLatestBackup(): File? {
        val backupDir = File(context.filesDir, "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.maxByOrNull { it.lastModified() }
        } else null
    }

    fun getAllBackups(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else emptyList()
    }
} 