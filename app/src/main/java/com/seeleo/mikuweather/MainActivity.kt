package com.seeleo.mikuweather

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seeleo.mikuweather.api.WeatherService
import com.seeleo.mikuweather.model.Hour
import com.seeleo.mikuweather.repository.WeatherRepository
import com.seeleo.mikuweather.viewmodel.WeatherViewModel
import com.seeleo.mikuweather.viewmodel.WeatherViewModelFactory
import com.thepseudoartistclan.mikuweather.LocationHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.*

@Suppress("unused")
private const val TAG: String = "LocationServices"

class MainActivity : AppCompatActivity() {
    //Query Location
    private var mLat: String = "0.0"
    private var mLon: String = "0.0"
    private var hour: ArrayList<Hour> = ArrayList()

    @Suppress("PrivatePropertyName")
    private val REQUEST_LOCATION_PERMISSION = 100

    //Main screen start
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            getCurrentLocation()
        }

        weatherUpdate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(){
        val location = LocationHelper.getLocation(this)
        mLat =" ${location.latitude}"
        mLon = "${location.longitude}"
    }

    private fun capitalizeEachWord(input: String): String {
        val words = input.split(" ")
        val capitalizedWords = words.map {
            @Suppress("DEPRECATION")
            it.capitalize(Locale.ROOT)
        }
        return capitalizedWords.joinToString(" ")
    }

    @Throws(java.lang.IndexOutOfBoundsException::class)
    private fun weatherUpdate() {
        val weatherInstance = WeatherService.getInstance()
        val repository = WeatherRepository(weatherInstance)
        val viewModel = ViewModelProvider(this, WeatherViewModelFactory(repository))[WeatherViewModel::class.java]

        var timeout = 0
        while (mLat == "0.0" && mLon == "0.0" && timeout <= 10) {
            Thread.sleep(1000)
            timeout += 1
        }
        if (mLat != "0.0" || mLon != "0.0") {
            viewModel.getCurrentTemp("$mLon,$mLat")
        } else {
            finish()
        }

        viewModel.weather.observe(this) { it ->
            val sortedAdcodes = it.result.alert.adcodes.sortedByDescending { it.adcode }
            val location = sortedAdcodes.joinToString(", ") { it.name }
            findViewById<TextView>(R.id.locationName).text = location

            this.currentWeatherDisplay(it.result.realtime.skycon, isDay())
            findViewById<TextView>(R.id.currentTemp).text = it.result.realtime.temperature.toString()
            findViewById<TextView>(R.id.conditionString).text = translateSkycon(it.result.realtime.skycon)

            /*
            LocalTime.now().hour
            hour = ArrayList<Hour>()
            val currentTime = Calendar.getInstance()
            currentTime.set(Calendar.MINUTE, 0)
            currentTime.set(Calendar.SECOND, 0)
            currentTime.set(Calendar.MILLISECOND, 0)
            val currentTimestamp = currentTime.timeInMillis / 1000
            it.forecast.forecastday[0].hour.filter { it.time_epoch >= currentTimestamp }.let { hour.addAll(it) }
            it.forecast.forecastday[1].hour.let { hour.addAll(it) }

            val forecastRecyclerView = findViewById<RecyclerView>(R.id.forecast_fragment)
            forecastRecyclerView.layoutManager = LinearLayoutManager(this)
            forecastRecyclerView.adapter = ForecastAdapter(hour)
            */
        }
    }

    private fun translateSkycon(skycon: String): String {
        when (skycon) {
            "CLEAR_DAY" -> {
                return "晴"
            }

            "CLEAR_NIGHT" -> {
                return "晴"
            }

            "PARTLY_CLOUDY_DAY" -> {
                return "多云"
            }

            "PARTLY_CLOUDY_NIGHT" -> {
                return "多云"
            }

            "CLOUDY" -> {
                return "阴"
            }

            "LIGHT_HAZE" -> {
                return "轻度雾霾"
            }

            "MODERATE_HAZE" -> {
                return "中度雾霾"
            }

            "HEAVY_HAZE" -> {
                return "重度雾霾"
            }

            "LIGHT_RAIN" -> {
                return "小雨"
            }

            "MODERATE_RAIN" -> {
                return "中雨"
            }

            "HEAVY_RAIN" -> {
                return "大雨"
            }

            "STORM_RAIN" -> {
                return "暴雨"
            }

            "FOG" -> {
                return "雾"
            }

            "LIGHT_SNOW" -> {
                return "小雪"
            }

            "MODERATE_SNOW" -> {
                return "中雪"
            }

            "HEAVY_SNOW" -> {
                return "大雪"
            }

            "STORM_SNOW" -> {
                return "暴雪"
            }

            "DUST" -> {
                return "浮尘"
            }

            "SAND" -> {
                return "沙尘"
            }

            "WIND" -> {
                return "大风"
            }
        }
        return ""
    }

    private fun isDay(): Boolean {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        return currentHour in 6..17
    }

    //Main Screen Weather Data Display
    private fun currentWeatherDisplay(skycon: String, isDay: Boolean) {
        val weatherIcon: ImageView = findViewById(R.id.weatherIcon)
        when (skycon) {
            "CLEAR_DAY" -> {
                weatherIcon.setImageResource(R.mipmap.tenki_hare)
            }
            "CLEAR_NIGHT" -> {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yoru)
            }
            "PARTLY_CLOUDY_DAY" -> {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kumori)
            }
            "PARTLY_CLOUDY_NIGHT" -> {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kumori_yoru)
            }
            "CLOUDY" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "LIGHT_HAZE" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "MODERATE_HAZE" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "HEAVY_HAZE" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "LIGHT_RAIN" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_ame_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
                })
            }
            "MODERATE_RAIN" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_ame_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
                })
            }
            "HEAVY_RAIN" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
                })
            }
            "STORM_RAIN" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
                })
            }
            "FOG" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "LIGHT_SNOW" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
                })
            }
            "MODERATE_SNOW" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_yuki)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
                })
            }
            "HEAVY_SNOW" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
                })
            }
            "STORM_SNOW" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
                })
            }
            "DUST" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "SAND" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
            "WIND" -> {
                (if (isDay) {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
                } else {
                    weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
                })
            }
        }
    }
}