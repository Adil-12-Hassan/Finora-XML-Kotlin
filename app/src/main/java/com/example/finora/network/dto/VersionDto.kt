package com.example.finora.network.dto

data class VersionResponse(
    val latestVersion: String,
    val minVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)