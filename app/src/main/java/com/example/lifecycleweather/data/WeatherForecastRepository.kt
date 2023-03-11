package com.example.lifecycleweather.data

import com.example.lifecycleweather.api.OpenWeatherService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherForecastRepository(
    private val service: OpenWeatherService,
    private val ioDispatcher : CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadWeatherSearch(city: String?, units: String?): Result<FiveDayForecast> =
        withContext(ioDispatcher){
            try{
                val results = service.searchWeather(city, units)
                Result.success(results)
            } catch(e: Exception){
                Result.failure(e)
            }
        }
}