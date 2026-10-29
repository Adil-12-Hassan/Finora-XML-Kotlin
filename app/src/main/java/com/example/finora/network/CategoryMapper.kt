package com.example.finora.network

/**
 * Maps Android category display names to the MongoDB enum values used by the web backend.
 * Web allows: Food, Transport, Shopping, Bills, Health, Entertainment, Other
 */
object CategoryMapper {

    /** Returns null if the transaction should NOT be synced (e.g., Income). */
    fun toApiCategory(androidCategory: String): String? = when (androidCategory) {
        "Food & Dining"      -> "Food"
        "Transport"          -> "Transport"
        "Shopping"           -> "Shopping"
        "Health"             -> "Health"
        "Entertainment"      -> "Entertainment"
        "Bills & Utilities"  -> "Bills"
        "Education"          -> "Other"
        "Other"              -> "Other"
        "Income"             -> null   // Income is local-only; not in web model
        else                 -> "Other"
    }

    fun fromApiCategory(apiCategory: String): String = when (apiCategory) {
        "Food"          -> "Food & Dining"
        "Transport"     -> "Transport"
        "Shopping"      -> "Shopping"
        "Health"        -> "Health"
        "Entertainment" -> "Entertainment"
        "Bills"         -> "Bills & Utilities"
        "Other"         -> "Other"
        else            -> "Other"
    }
}