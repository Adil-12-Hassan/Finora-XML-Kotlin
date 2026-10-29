package com.example.finora.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load
import coil.transform.CircleCropTransformation
import com.example.finora.BuildConfig
import com.example.finora.databinding.ActivitySettingsBinding
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.UpdateProfileRequest
import com.example.finora.ui.auth.LoginActivity
import com.example.finora.util.SessionManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var session: SessionManager

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val sizeBytes = inputStream?.available() ?: 0
                inputStream?.close()

                if (sizeBytes > 500 * 1024) {
                    Toast.makeText(this, "Avatar must be under 0.5MB", Toast.LENGTH_LONG).show()
                } else {
                    session.setAvatar(uri.toString())
                    loadProfile()
                    Toast.makeText(this, "Avatar updated successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        loadProfile()
        loadPreferences()
        setupListeners()
    }

    private fun loadProfile() {
        val name = session.getUserName()
        val email = session.getUserEmail()
        val avatar = session.getAvatar()

        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email

        if (!avatar.isNullOrEmpty()) {
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvAvatar.visibility = View.GONE
            binding.ivAvatar.load(Uri.parse(avatar)) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            binding.ivAvatar.visibility = View.GONE
            binding.tvAvatar.visibility = View.VISIBLE
            // Initials avatar
            val initials = name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            binding.tvAvatar.text = initials
        }
    }

    private fun loadPreferences() {
        binding.tvCurrency.text = "${session.getCurrencyCode()} — ${session.getCurrencyName()}"

        val budget = session.getMonthlyBudget()
        binding.tvMonthlyBudget.text = if (budget > 0)
            "${session.getCurrencyCode()} ${String.format("%,.0f", budget)}"
        else "Not set"

        // Version display
        binding.tvVersion.text = "Finora v${BuildConfig.VERSION_NAME}"

        // Dark mode state
        val isDark = AppCompatDelegate.getDefaultNightMode() ==
                AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.isChecked = isDark
    }

    private fun setupListeners() {

        // Edit profile (Name & Avatar)
        binding.layoutProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Currency
        binding.layoutCurrency.setOnClickListener {
            CurrencyBottomSheet { code, name ->
                binding.tvCurrency.text = "$code — $name"
            }.show(supportFragmentManager, "currency")
        }

        // Monthly budget
        binding.layoutSetBudget.setOnClickListener {
            showBudgetDialog()
        }

        // Change password
        binding.layoutChangePassword.setOnClickListener {
            ChangePasswordBottomSheet()
                .show(supportFragmentManager, "change_password")
        }

        // Logout
        binding.layoutLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out") { _, _ ->
                    val bearer = session.getBearerToken()
                    if (bearer != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = RetrofitClient.api.logout(bearer)
                                if (!response.isSuccessful) {
                                    Log.w("FinoraAuth", "Remote logout failed: HTTP ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Log.w("FinoraAuth", "Could not invalidate remote session", e)
                            }
                        }
                    }
                    session.logout()
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditProfileDialog() {
        val input = TextInputEditText(this)
        input.hint = "Enter name"
        input.setText(session.getUserName())

        val container = FrameLayout(this).apply {
            setPadding(48, 16, 48, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save Name") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    session.setUserName(newName)
                    loadProfile()
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()

                    val bearer = session.getBearerToken()
                    if (bearer != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitClient.api.updateProfile(bearer, UpdateProfileRequest(name = newName))
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            .setNeutralButton("Change Avatar") { _, _ ->
                pickImageLauncher.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBudgetDialog() {
        val input = TextInputEditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter amount"

        val current = session.getMonthlyBudget()
        if (current > 0) input.setText(String.format("%.0f", current))

        val container = FrameLayout(this).apply {
            setPadding(48, 16, 48, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Monthly budget")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                session.setMonthlyBudget(amount.toFloat())
                binding.tvMonthlyBudget.text =
                    "${session.getCurrencyCode()} ${String.format("%,.0f", amount)}"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
