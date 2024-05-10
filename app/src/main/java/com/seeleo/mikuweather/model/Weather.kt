package com.seeleo.mikuweather.model

data class Weather(
    val current: Current,
    val forecast: Forecast,
    val location: Location
)
