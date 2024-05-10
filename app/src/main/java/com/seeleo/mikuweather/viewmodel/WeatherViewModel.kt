package com.seeleo.mikuweather.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.seeleo.mikuweather.model.Caiyun
import com.seeleo.mikuweather.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    val weather: MutableLiveData<Caiyun> = MutableLiveData()

    //WeatherAPI API key
    private val apikey = "XX3OXGV581TJoQNP"

    fun getCurrentTemp(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = repository.getCurrentWeather(apikey, query)
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
