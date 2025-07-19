package com.example.coinsage.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.coinsage.R
import com.example.coinsage.databinding.FragmentSettingsBinding
import com.example.coinsage.utils.PrefsHelper
import com.example.coinsage.utils.BackupManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsHelper: PrefsHelper
    private lateinit var backupManager: BackupManager

    companion object {
        private val SUPPORTED_CURRENCIES = arrayOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "LKR")
        private val _currencyChanged = MutableLiveData<String>()
        val currencyChanged: LiveData<String> = _currencyChanged
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())
        backupManager = BackupManager(requireContext())
        setupSettingsView()
        setupListeners()
    }

    private fun setupSettingsView() {
        // Setup currency spinner
        val currencyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            SUPPORTED_CURRENCIES
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerCurrency.adapter = currencyAdapter

        // Set current values
        val currentCurrency = prefsHelper.getCurrency()
        val currentNotificationState = prefsHelper.getNotificationsEnabled()

        binding.spinnerCurrency.setSelection(SUPPORTED_CURRENCIES.indexOf(currentCurrency))
        binding.switchNotifications.isChecked = currentNotificationState
    }

    private fun setupListeners() {
        binding.btnSaveSettings.setOnClickListener {
            val selectedCurrency = binding.spinnerCurrency.selectedItem.toString()
            val notificationsEnabled = binding.switchNotifications.isChecked

            val previousCurrency = prefsHelper.getCurrency()
            if (previousCurrency != selectedCurrency) {
                prefsHelper.setCurrency(selectedCurrency)
                _currencyChanged.value = selectedCurrency
            }
            
            prefsHelper.setNotificationsEnabled(notificationsEnabled)

            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
        }

        binding.btnBackup.setOnClickListener {
            showBackupConfirmationDialog()
        }

        binding.btnRestore.setOnClickListener {
            showRestoreConfirmationDialog()
        }
    }

    private fun showBackupConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup_confirm)
            .setMessage(R.string.backup_confirm_message)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                performBackup()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showRestoreConfirmationDialog() {
        val latestBackup = backupManager.getLatestBackup()
        if (latestBackup == null) {
            Toast.makeText(requireContext(), "No backup file found", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restore_confirm)
            .setMessage(R.string.restore_confirm_message)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                performRestore(latestBackup)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun performBackup() {
        val transactions = prefsHelper.getTransactions()
        backupManager.backupData(transactions)
            .onSuccess { 
                Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                Toast.makeText(requireContext(), R.string.backup_error, Toast.LENGTH_SHORT).show()
            }
    }

    private fun performRestore(backupFile: File) {
        backupManager.restoreData(backupFile)
            .onSuccess { transactions -> 
                prefsHelper.saveTransactions(transactions)
                Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                Toast.makeText(requireContext(), R.string.restore_error, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 