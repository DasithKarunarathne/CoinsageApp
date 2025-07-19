package com.example.coinsage.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.coinsage.R
import com.example.coinsage.databinding.ActivityLoginBinding
import com.example.coinsage.ui.MainActivity
import com.example.coinsage.utils.PrefsHelper
import com.example.coinsage.utils.SecurityUtils
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)

        // Check if user is already logged in
        if (prefsHelper.isLoggedIn()) {
            startMainActivity()
            finish()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateAndLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_field_required))
            return
        }

        // Check credentials
        val storedPassword = prefsHelper.getPassword(email)
        if (storedPassword != null && storedPassword == SecurityUtils.hashPassword(password)) {
            // Login successful
            prefsHelper.setLoggedInUser(email)
            startMainActivity()
            finish()
        } else {
            showError(getString(R.string.error_invalid_credentials))
        }
    }

    private fun showError(message: String) {
        binding.tvError.apply {
            text = message
            visibility = View.VISIBLE
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 