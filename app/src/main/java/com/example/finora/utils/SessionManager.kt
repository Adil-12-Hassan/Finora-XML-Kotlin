package com.example.finora.util

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("finora_session", Context.MODE_PRIVATE)

    // ── Auth 

    fun saveLogin(id: Int, name: String, email: String) {
        prefs.edit()
            .putInt("user_id", id)
            .putString("user_name", name)
            .putString("user_email", email)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    /** Called after a successful API login/register — stores MongoDB _id and JWT */
    fun saveApiLogin(mongoId: String, name: String, email: String, token: String, localUserId: Int) {
        prefs.edit()
            .putString("mongo_id", mongoId)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("jwt_token", token)
            .putInt("user_id", localUserId)   // ← this was missing
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)
    /** Returns "Bearer <token>" ready to use as Authorization header. Null if not logged in. */
    fun getBearerToken(): String? = getToken()?.let { "Bearer $it" }
    fun getMongoId(): String? = prefs.getString("mongo_id", null)
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUserName(): String = prefs.getString("user_name", "User") ?: "User"
    fun getUserEmail(): String = prefs.getString("user_email", "") ?: ""
    fun logout() {
        prefs.edit().clear().apply()
    }
    // ── Currency ──
    fun setCurrency(code: String, name: String) {
        prefs.edit()
            .putString("currency_code", code)
            .putString("currency_name", name)
            .apply()
    }
    fun getCurrencyCode(): String = prefs.getString("currency_code", "PKR") ?: "PKR"
    fun getCurrencyName(): String = prefs.getString("currency_name", "Pakistani Rupee") ?: "Pakistani Rupee"
    // ── Monthly Budget ──
    fun setMonthlyBudget(amount: Float) {
        prefs.edit().putFloat("monthly_budget", amount).apply()
    }
    fun getMonthlyBudget(): Float = prefs.getFloat("monthly_budget", 0f)

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun setAvatar(avatar: String?) {
        prefs.edit().putString("user_avatar", avatar).apply()
    }

    fun getAvatar(): String? = prefs.getString("user_avatar", null)
}
