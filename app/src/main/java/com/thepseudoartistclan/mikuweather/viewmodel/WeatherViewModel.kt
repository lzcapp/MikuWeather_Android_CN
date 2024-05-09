package com.thepseudoartistclan.mikuweather.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thepseudoartistclan.mikuweather.model.Weather
import com.thepseudoartistclan.mikuweather.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    val weather: MutableLiveData<Weather> = MutableLiveData()

    //WeatherAPI API key
    private val apikey = "ed825e2a9de342c58a190022240605" //use your own API key

    fun getCurrentTemp(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = repository.getCurrentWeather(apikey, query, 2)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    weather.value = response.body()
                } else {
                    Log.d(TAG, "Error fetching data")
                }
            }
        }
    }
}
