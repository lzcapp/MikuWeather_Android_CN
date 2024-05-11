package com.seeleo.mikuweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.seeleo.mikuweather.repository.WeatherRepository

class WeatherViewModelFactory(private val weatherRepository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            WeatherViewModel(this.weatherRepository) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}