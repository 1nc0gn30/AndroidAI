package com.example.model

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val category: String = "App",
    val launchIntentAvailable: Boolean = true
)
