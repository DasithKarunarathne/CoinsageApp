package com.example.coinsage.ui.transactions

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.coinsage.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteConfirmationDialog : DialogFragment() {
    private var onConfirmDelete: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(R.string.delete_confirmation_message)
            .setNegativeButton(R.string.btn_cancel) { _, _ -> dismiss() }
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                onConfirmDelete?.invoke()
                dismiss()
            }
            .create()
    }

    companion object {
        fun newInstance(onConfirmDelete: () -> Unit): DeleteConfirmationDialog {
            return DeleteConfirmationDialog().apply {
                this.onConfirmDelete = onConfirmDelete
            }
        }
    }
} 