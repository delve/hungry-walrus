package com.delve.hungrywalrus.domain.model

data class LogEntry(
    val id: Long = 0,
    val foodName: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val timestamp: Long,
)
