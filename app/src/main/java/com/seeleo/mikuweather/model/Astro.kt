package com.seeleo.mikuweather.model

@Suppress("SpellCheckingInspection", "PropertyName")
data class Astro(
    val moon_illumination: String,
    val moon_phase: String,
    val moonrise: String,
    val moonset: String,
    val sunrise: String,
    val sunset: String,
    val isMoonUp: Int,
    val isSunUp: Int
)
