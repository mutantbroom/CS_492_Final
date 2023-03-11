package com.example.lifecycleweather.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifecycleweather.data.*
import com.example.lifecycleweather.api.OpenWeatherService
import kotlinx.coroutines.launch

class WeatherSearchViewModel : ViewModel() {
    private val repository = WeatherForecastRepository(OpenWeatherService.create())

    private val _searchResults = MutableLiveData<FiveDayForecast?>(null)
    val searchResults: LiveData<FiveDayForecast?> = _searchResults

    private val _loadingStatus = MutableLiveData(LoadingStatus.SUCCESS)
    val loadingStatus: LiveData<LoadingStatus> = _loadingStatus

    fun loadSearchResults(city: String?, units: String?){
        viewModelScope.launch{
            _loadingStatus.value = LoadingStatus.LOADING
            val result = repository.loadWeatherSearch(city, units)
            _searchResults.value = result.getOrNull()
            _loadingStatus.value = when(result.isSuccess){
                true -> LoadingStatus.SUCCESS
                false -> LoadingStatus.ERROR
            }
        }
    }
}