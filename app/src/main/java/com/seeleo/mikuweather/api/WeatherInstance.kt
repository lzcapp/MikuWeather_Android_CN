package com.seeleo.mikuweather.api

import com.seeleo.mikuweather.model.Caiyun
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherInstance {
    @GET("v2.6/{key}/{query}/weather")
    suspend fun getCurrentWeather(
        @Path("key") key: String,
        @Path("query") query: String,
        @Query("alert") alert: Boolean
    ): Response<Caiyun>
}
