package com.seeleo.mikuweather

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
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

        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.refreshLayout)

        refreshLayout.setOnRefreshListener {
            Handler().postDelayed({
                refreshLayout.isRefreshing = false
                weatherUpdate()
            }, 3000)
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
        if (mLat == "0.0" && mLon == "0.0") {
            viewModel.getCurrentTemp("auto:ip")
        } else {
            viewModel.getCurrentTemp("$mLat,$mLon")
        }

        viewModel.weather.observe(this) { it ->
            val location = if (it.location.name == it.location.region) {
                it.location.name + ", " + it.location.country
            } else {
                it.location.name + ", " + it.location.region + ", " + it.location.country
            }
            findViewById<TextView>(R.id.locationName).text = location

            this.currentWeatherDisplay(it.current.condition.code, it.current.is_day)
            findViewById<TextView>(R.id.currentTemp).text = it.current.temp_c.toString()
            findViewById<TextView>(R.id.conditionString).text = capitalizeEachWord(it.current.condition.text)

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
        }
    }

    //Main Screen Weather Data Display
    private fun currentWeatherDisplay(code: Int, isDay: Int) {
        var dayCheck = false
        if (isDay == 1) dayCheck = true
        val weatherIcon: ImageView = findViewById(R.id.weatherIcon)
        when (code) {
            //Sunny
            1000 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yoru)
            })
            //Partly Cloudy
            1003 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kumori)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kumori_yoru)
            })
            //Cloudy
            1006 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
            })
            //Overcast
            1009 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
            })
            //Mist
            1030 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
            })
            //Patchy Rain Possible
            1063 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Patchy Snow Possible
            1066 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki_yoru)
            })
            //Patchy Sleet Possible
            1069 -> ((if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            }))
            //Patchy Freezing Drizzle Possible
            1072 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki_yoru)
            })
            //Thundery outbreaks possible
            1087 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kaminari)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kaminari_yoru)
            })
            //Blowing snow
            1114 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Blizzard
            1117 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
            })
            //Fog
            1135 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_hare_yoru)
            })
            //Freezing Fog
            1147 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
            })
            //Patchy light drizzle
            1150 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame_yoru)
            })
            //Light drizzle
            1153 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame_yoru)
            })
            //Freezing drizzle
            1168 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_yuki)
            })
            //Heavy freezing drizzle
            1171 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_ame)
            })
            //Patchy light rain
            1180 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_ame_yoru)
            })
            //Light rain
            1183 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Moderate rain at times
            1186 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Moderate rain
            1189 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Heavy rain at times
            1192 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            })
            //Heavy rain
            1195 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            })
            //Light freezing rain
            1198 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_yuki)
            })
            //Moderate or heavy freezing rain
            1201 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_ame)
            })
            //Light sleet
            1204 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Moderate or heavy sleet
            1207 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            })
            //Patchy light snow
            1210 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki_yoru)
            })
            //Light snow
            1213 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Patchy moderate snow
            1216 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Moderate snow
            1219 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Patchy heavy snow
            1222 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki)
            })
            //Heavy snow
            1225 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhusetsu)
            })
            //Ice pellets
            1237 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Light rain shower
            1240 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Moderate or heavy rain shower
            1243 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_ame)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_ame_hare_yoru)
            })
            //Torrential rain shower
            1246 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_bouhuu)
            })
            //Light sleet showers
            1249 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Moderate or heavy sleet showers
            1252 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Light snow showers
            1255 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_yuki_yoru)
            })
            //Moderate or heavy snow showers
            1258 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare)
            })
            //Light showers of ice pellets
            1261 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Moderate or heavy showers of ice pellets
            1264 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_hare_yoru)
            })
            //Patchy light rain with thunder
            1273 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kaminari)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_hare_kaminari_yoru)
            })
            //Moderate or heavy rain with thunder
            1276 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_kaminari)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_kaminari)
            })
            //Patchy light snow with thunder
            1279 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_kaminari)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_yuki_kaminari)
            })
            //Moderate or heavy snow with thunder
            1282 -> (if (dayCheck) {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki_kaminari)
            } else {
                weatherIcon.setImageResource(R.mipmap.tenki_kumori_yuki_kaminari)
            })
        }
    }
}