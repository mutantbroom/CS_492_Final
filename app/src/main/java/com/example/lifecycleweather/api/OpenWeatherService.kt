package com.example.lifecycleweather.api

import com.example.lifecycleweather.data.FiveDayForecast
import com.example.lifecycleweather.data.OpenWeatherCityJsonAdapter
import com.example.lifecycleweather.data.OpenWeatherListJsonAdapter
import com.example.lifecycleweather.ui.OPENWEATHER_APPID
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * This is a Retrofit service interface encapsulating communication with the OpenWeather API.
 */
interface OpenWeatherService {
    /**
     * This method is used to query the OpenWeather API's 5-day/3-hour forecast method:
     * https://openweathermap.org/forecast5
     *
     * @param location Specifies the location for which to fetch forecast data.  For US cities,
     *   this should be specified as "<city>,<state>,<country>" (e.g. "Corvallis,OR,US"), while
     *   for international cities, it should be specified as "<city>,<country>" (e.g. "London,GB").
     * @param units Specifies the type of units that should be returned by the OpenWeather API.
     *   Can be one of: "standard", "metric", and "imperial".
     * @param apiKey Should be a valid OpenWeather API key.
     *
     * @return Returns a Retrofit `Call<>` object.  The response body associated with this call will
     *   contain a `FiveDayForecast` object if the call was successful.
     */
    @GET("forecast/")
    suspend fun searchWeather(
        @Query("q") city: String?,
        @Query("units") units: String?,
        @Query("appid") appid: String = OPENWEATHER_APPID
    ) : FiveDayForecast

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

        /**
         * This method can be called as `OpenWeatherService.create()` to create an object
         * implementing the OpenWeatherService interface and which can be used to make calls to
         * the OpenWeather API.
         */
        fun create() : OpenWeatherService {
            val moshi = Moshi.Builder()
                .add(OpenWeatherListJsonAdapter())
                .add(OpenWeatherCityJsonAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            return retrofit.create(OpenWeatherService::class.java)
        }
    }
}