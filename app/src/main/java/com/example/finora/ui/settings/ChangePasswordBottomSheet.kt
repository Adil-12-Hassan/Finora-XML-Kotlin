package com.example.finora.ui.settings

import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.databinding.BottomSheetChangePasswordBinding
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.ChangePasswordRequest
import com.example.finora.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ChangePasswordBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val db = FinoraDatabase.getDatabase(requireContext())

        binding.btnUpdatePassword.setOnClickListener {
            val old = binding.etOldPassword.text.toString().trim()
            val new = binding.etNewPassword.text.toString().trim()
            val confirm = binding.etConfirmNew.text.toString().trim()

            // Validate
            if (old.isEmpty()) {
                binding.tilOldPassword.error = "Enter your current password"
                return@setOnClickListener
            } else binding.tilOldPassword.error = null

            if (new.isEmpty()) {
                binding.tilNewPassword.error = "Enter a new password"
                return@setOnClickListener
            } else if (new.length < 6) {
                binding.tilNewPassword.error = "Minimum 6 characters"
                return@setOnClickListener
            } else binding.tilNewPassword.error = null

            if (confirm != new) {
                binding.tilConfirmNew.error = "Passwords do not match"
                return@setOnClickListener
            } else binding.tilConfirmNew.error = null

            binding.btnUpdatePassword.isEnabled = false
            binding.btnUpdatePassword.text = "Updating..."

            lifecycleScope.launch(Dispatchers.IO) {
                var errorMessage: String? = null
                var updated = false
                val bearer = session.getBearerToken()

                if (bearer != null) {
                    try {
                        val response = RetrofitClient.api.changePassword(
                            bearer,
                            ChangePasswordRequest(old, new)
                        )
                        if (response.isSuccessful) {
                            db.userDao().findByEmail(session.getUserEmail())?.let {
                                db.userDao().updatePassword(it.id, new)
                            }
                            updated = true
                        } else {
                            val errorBody = response.errorBody()?.string()
                            errorMessage = errorBody?.let {
                                runCatching { JSONObject(it).optString("message") }.getOrNull()
                            }?.takeIf { it.isNotBlank() }
                                ?: "Could not update password"
                        }
                    } catch (e: java.io.IOException) {
                        Log.w("FinoraAuth", "Password change API unavailable; trying local account", e)
                    } catch (e: Exception) {
                        Log.e("FinoraAuth", "Password change request failed", e)
                        errorMessage = "Could not update password"
                    }
                }

                if (errorMessage == null && !updated) {
                    val user = db.userDao().login(session.getUserEmail(), old)
                    if (user == null) {
                        errorMessage = "Current password is incorrect"
                    } else {
                        db.userDao().updatePassword(user.id, new)
                        updated = true
                    }
                }

                withContext(Dispatchers.Main) {
                    if (updated) {
                        dismiss()
                        Snackbar.make(
                            requireActivity().window.decorView,
                            "Password updated successfully",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        if (errorMessage == "Current password is incorrect") {
                            binding.tilOldPassword.error = errorMessage
                        } else {
                            Snackbar.make(
                                binding.root,
                                errorMessage ?: "Could not update password",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        binding.btnUpdatePassword.isEnabled = true
                        binding.btnUpdatePassword.text = "Update password"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}