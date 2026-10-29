
package com.example.finora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.User
import com.example.finora.databinding.ActivityLoginBinding
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.LoginRequest
import com.example.finora.ui.dashboard.DashboardActivity
import com.example.finora.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // Already logged in → skip to dashboard
        if (session.isLoggedIn()) {
            startDashboard()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Try API login
                val response = RetrofitClient.api.login(
                    LoginRequest(email, password)
                )

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.token != null && body.user != null) {

                        val db = FinoraDatabase.getDatabase(
                            this@LoginActivity
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

                        // Save API session with the local Room user ID
                        session.saveApiLogin(
                            mongoId = body.user.id,
                            name = body.user.name,
                            email = body.user.email,
                            token = body.token,
                            localUserId = localUser?.id ?: 1
                        )

                        // Sync profile settings from the server
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
                    ?: "Login failed. Check your credentials."

                showError(errorMsg)

            } catch (e: Exception) {
                // Network unavailable → fall back to Room local login
                try {
                    val db = FinoraDatabase.getDatabase(
                        this@LoginActivity
                    )

                    val user = db.userDao().login(email, password)

                    if (user != null) {
                        session.saveLogin(
                            user.id,
                            user.name,
                            user.email
                        )
                        startDashboard()
                    } else {
                        showError(
                            "No network and no local account found."
                        )
                    }
                } catch (localException: Exception) {
                    showError(
                        "Unable to log in. Please try again."
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
        binding.btnLogin.isEnabled = !loading
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

    /** Parses {"status":"Error","message":"..."} JSON */
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
