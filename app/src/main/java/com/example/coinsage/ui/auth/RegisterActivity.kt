package com.example.coinsage.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.coinsage.R
import com.example.coinsage.databinding.ActivityRegisterBinding
import com.example.coinsage.utils.PrefsHelper
import com.example.coinsage.utils.SecurityUtils
import com.example.coinsage.ui.MainActivity // Add this import
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun validateAndRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Basic validation
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.error_field_required))
            return
        }

        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address")
            return
        }

        // Password match validation
        if (password != confirmPassword) {
            showError(getString(R.string.error_passwords_dont_match))
            return
        }

        // Check if user already exists
        if (prefsHelper.getPassword(email) != null) {
            showError("Email already registered")
            return
        }

        // Save credentials
        val hashedPassword = SecurityUtils.hashPassword(password)
        prefsHelper.saveCredentials(email, hashedPassword)
        prefsHelper.setLoggedInUser(email)

        // Show success message and navigate to MainActivity
        Snackbar.make(binding.root, "Registration successful", Snackbar.LENGTH_SHORT).show()
        startMainActivity()
    }

    private fun showError(message: String) {
        binding.tvError.apply {
            text = message
            visibility = View.VISIBLE
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
} 