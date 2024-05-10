package com.seeleo.mikuweather.repository

import com.seeleo.mikuweather.api.WeatherInstance

class WeatherRepository(private val instance: WeatherInstance) {
    suspend fun getCurrentWeather(key: String, query: String, days: Int) = instance.getCurrentTemp(key, query, days, "no", "no")
}