
package com.example.finora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.User
import com.example.finora.databinding.ActivityRegisterBinding
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.RegisterRequest
import com.example.finora.ui.dashboard.DashboardActivity
import com.example.finora.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirm.isEmpty()
        ) {
            showError("All fields are required")
            return
        }

        if (password != confirm) {
            showError("Passwords do not match")
            return
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Try API registration
                val response = RetrofitClient.api.register(
                    RegisterRequest(name, email, password)
                )

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.token != null && body.user != null) {

                        val db = FinoraDatabase.getDatabase(
                            this@RegisterActivity
                        )

                        // Find existing local user or create one
                        var localUser = db.userDao()
                            .findByEmail(body.user.email)

                        if (localUser == null) {
                            db.userDao().register(
                                User(
                                    name = body.user.name,
                                    email = body.user.email,
                                    password = ""
                                )
                            )

                            localUser = db.userDao()
                                .findByEmail(body.user.email)
                        }

                        // Save API session with local Room user ID
                        session.saveApiLogin(
                            mongoId = body.user.id,
                            name = body.user.name,
                            email = body.user.email,
                            token = body.token,
                            localUserId = localUser?.id ?: 1
                        )

                        // Sync profile settings from server
                        session.setMonthlyBudget(
                            body.user.monthlyBudget
                        )

                        session.setCurrency(
                            body.user.currency,
                            currencyName(body.user.currency)
                        )

                        startDashboard()
                        return@launch
                    }
                }

                // Show server error message
                val errorMsg = response.errorBody()?.string()
                    ?.let { parseErrorMessage(it) }
                    ?: "Registration failed. Try again."

                showError(errorMsg)

            } catch (e: Exception) {
                // Offline fallback → save locally in Room only
                try {
                    val db = FinoraDatabase.getDatabase(
                        this@RegisterActivity
                    )

                    val existing = db.userDao().findByEmail(email)

                    if (existing != null) {
                        showError("Email already registered locally.")
                    } else {
                        val userId = db.userDao()
                            .register(
                                User(
                                    name = name,
                                    email = email,
                                    password = password
                                )
                            )
                            .toInt()

                        session.saveLogin(
                            userId,
                            name,
                            email
                        )

                        startDashboard()
                    }
                } catch (localException: Exception) {
                    showError(
                        "Unable to register. Please try again."
                    )
                }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun startDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.progressBar.visibility =
            if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        Snackbar.make(
            binding.root,
            msg,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun parseErrorMessage(json: String): String? =
        try {
            org.json.JSONObject(json).optString("message")
        } catch (e: Exception) {
            null
        }

    private fun currencyName(code: String) = when (code) {
        "USD" -> "US Dollar"
        "EUR" -> "Euro"
        "GBP" -> "British Pound"
        "AED" -> "UAE Dirham"
        else -> "Pakistani Rupee"
    }
}
