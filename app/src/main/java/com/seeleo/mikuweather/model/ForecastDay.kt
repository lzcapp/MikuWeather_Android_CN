package com.seeleo.mikuweather.model

@Suppress("SpellCheckingInspection", "PropertyName")
data class ForecastDay(
    val astro: Astro,
    val date: String,
    val date_epoch: Int,
    val day: Day,
    val hour: List<Hour>
)
