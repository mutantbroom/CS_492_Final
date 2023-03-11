package com.example.lifecycleweather.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.lifecycleweather.R
import com.example.lifecycleweather.data.*
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/*
 * Often, we'll have sensitive values associated with our code, like API keys, that we'll want to
 * keep out of our git repo, so random GitHub users with permission to view our repo can't see them.
 * The OpenWeather API key is like this.  We can keep our API key out of source control using the
 * technique described below.  Note that values configured in this way can still be seen in the
 * app bundle installed on the user's device, so this isn't a safe way to store values that need
 * to be kept secret at all costs.  This will only keep
 *
 * To use your own OpenWeather API key here, create a file called `gradle.properties` in your
 * GRADLE_USER_HOME directory (this will usually be `$HOME/.gradle/` in MacOS/Linux and
 * `$USER_HOME/.gradle/` in Windows), and add the following line:
 *
 *   OPENWEATHER_API_KEY="<put_your_own_OpenWeather_API_key_here>"
 *
 * Then, add the following line to the `defaultConfig` section of build.gradle:
 *
 *   buildConfigField("String", "OPENWEATHER_API_KEY", OPENWEATHER_API_KEY)
 *
 * The Gradle build for this project will grab that value and store it in the field
 * `BuildConfig.OPENWEATHER_API_KEY` that's used below.  You can read more about this setup on the
 * following pages:
 *
 *   https://developer.android.com/studio/build/gradle-tips#share-custom-fields-and-resource-values-with-your-app-code
 *
 *   https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties
 *
 * Alternatively, if you don't mind whether people see your OpenWeather API key on GitHub, you can
 * just hard-code your API key below, replacing `BuildConfig.OPENWEATHER_API_KEY` 🤷‍.
 */
val OPENWEATHER_APPID = "8aaf66086421201b80bef063d96fbde5"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val tag = "MainActivity"
    private val apiBaseUrl = "https://api.openweathermap.org/data/2.5"

    private lateinit var forecastAdapter: ForecastAdapter
    private val viewModel: WeatherSearchViewModel by viewModels()

    private lateinit var requestQueue: RequestQueue
    private lateinit var forecastJsonAdapter: JsonAdapter<FiveDayForecast>

    private lateinit var forecastListRV: RecyclerView
    private lateinit var loadingErrorTV: TextView
    private lateinit var loadingIndicator: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadingErrorTV = findViewById(R.id.tv_loading_error)
        loadingIndicator = findViewById(R.id.loading_indicator)
        forecastListRV = findViewById(R.id.rv_forecast_list)

        forecastAdapter = ForecastAdapter(::onForecastItemClick)

        forecastListRV.layoutManager = LinearLayoutManager(this)
        forecastListRV.setHasFixedSize(true)
        forecastListRV.adapter = forecastAdapter

        requestQueue = Volley.newRequestQueue(this)

        val moshi = Moshi.Builder()
            .add(OpenWeatherListJsonAdapter())
            .add(OpenWeatherCityJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        forecastJsonAdapter = moshi.adapter(FiveDayForecast::class.java)

        viewModel.searchResults.observe(this){
            searchResults -> forecastAdapter.updateForecast(searchResults)
        }
        viewModel.loadingStatus.observe(this){
            loadingStatus -> when(loadingStatus){
                LoadingStatus.LOADING-> {
                    loadingIndicator.visibility = View.VISIBLE
                    forecastListRV.visibility = View.INVISIBLE
                    loadingErrorTV.visibility = View.INVISIBLE
                }
                LoadingStatus.ERROR->{
                    loadingIndicator.visibility = View.INVISIBLE
                    forecastListRV.visibility = View.INVISIBLE
                    loadingErrorTV.visibility = View.VISIBLE
                }
                else->{
                    loadingIndicator.visibility = View.INVISIBLE
                    forecastListRV.visibility = View.VISIBLE
                    loadingErrorTV.visibility = View.INVISIBLE
                }
            }
        }

        /*
         * Trigger a call to fetch forecast data when the activity is first created.
         */
        //fetchFiveDayForecast("Corvallis,OR,US", "imperial")
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val location = sharedPrefs.getString(
            getString(R.string.pref_location_key),
            null
        )
        val unit = sharedPrefs.getString(
            getString(R.string.pref_unit_key),
            null
        )
        forecastAdapter.updateUnit(unit)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)

        viewModel.loadSearchResults(location,unit)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val location = sharedPreferences!!.getString(
            getString(R.string.pref_location_key),
            null
        )
        val unit = sharedPreferences!!.getString(
            getString(R.string.pref_unit_key),
            null
        )
        forecastAdapter.updateUnit(unit)
        viewModel.loadSearchResults(location,unit)
    }

    /**
     * This method fetches 5-day forecast data from the OpenWeather API.  It sends an HTTP request
     * to the API based on the provided data and handles the response.
     *
     * @param location The location for which to fetch forecast data.
     * @param units The units in which data should be returned by the API.
     */
    /*private fun fetchFiveDayForecast(location: String, units: String) {
        /*
         * Show loading indicator and hide other UI components while data fetching is in progress.
         */
        loadingIndicator.visibility = View.VISIBLE
        forecastListRV.visibility = View.INVISIBLE
        loadingErrorTV.visibility = View.INVISIBLE

        /*
         * Asynchronously send HTTP request to the OpenWeather API using the OpenWeatherService
         * Retrofit service.
         */
        openWeatherService.loadFiveDayForecast(location, units, OPENWEATHER_APPID)
            .enqueue(object : Callback<FiveDayForecast> {
                /*
                 * onResponse() is the callback executed when a response is received from the API.
                 * The response may or may not indicate success.
                 */
                override fun onResponse(
                    call: Call<FiveDayForecast>,
                    response: Response<FiveDayForecast>
                ) {
                    if (response.isSuccessful) {
                        /*
                         * If response was successful, grab the forecast data out of response
                         * body and plug them into the RecyclerView adapter.  Show RecyclerView.
                         */
                        forecastAdapter.updateForecast(response.body())
                        supportActionBar?.title = forecastAdapter.forecastCity?.name
                        loadingIndicator.visibility = View.INVISIBLE
                        forecastListRV.visibility = View.VISIBLE
                    } else {
                        handleFetchError(response.errorBody()?.string() ?: "unknown error response (status code ${response.code()})")
                    }
                }

                /*
                 * onFailure() is called when an API call can't be executed (i.e. the request can't
                 * be sent, or no response is received).
                 */
                override fun onFailure(call: Call<FiveDayForecast>, t: Throwable) {
                    handleFetchError(t.message ?: "unknown failure (Throwable: $t)")
                }

                /*
                 * This is a helper function to display an error message to the user when
                 * appropriate.
                 */
                private fun handleFetchError(message: String) {
                    Log.d(tag, "Error fetching forecast: $message")
                    loadingErrorTV.text = getString(R.string.loading_error, message)
                    loadingIndicator.visibility = View.INVISIBLE
                    loadingErrorTV.visibility = View.VISIBLE
                }
            })
    }*/

    /**
     * This method is passed into the RecyclerView adapter to handle clicks on individual items
     * in the list of forecast items.  When a forecast item is clicked, a new activity is launched
     * to view its details.
     */
    private fun onForecastItemClick(forecastPeriod: ForecastPeriod) {
        val intent = Intent(this, ForecastDetailActivity::class.java).apply {
            putExtra(EXTRA_FORECAST_PERIOD, forecastPeriod)
            putExtra(EXTRA_FORECAST_CITY, forecastAdapter.forecastCity)
            putExtra(EXTRA_FORECAST_UNIT, forecastAdapter.forecastUnit)
        }
        startActivity(intent)
    }

    /**
     * This method is called to insert a custom menu into the action bar for this activity.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    /**
     * This method is called when the user selects an action from the action bar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_map -> {
                viewForecastCityOnMap()
                true
            }
            R.id.action_settings -> {
                viewSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method creates an implicit intent to display the current forecast location in a map.
     */
    private fun viewForecastCityOnMap() {
        if (forecastAdapter.forecastCity != null) {
            val geoUri = Uri.parse(getString(
                R.string.geo_uri,
                forecastAdapter.forecastCity?.lat ?: 0.0,
                forecastAdapter.forecastCity?.lon ?: 0.0,
                11
            ))
            val intent = Intent(Intent.ACTION_VIEW, geoUri)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    findViewById(R.id.coordinator_layout),
                    R.string.action_map_error,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun viewSettings(){
        val intent = Intent(this, SettingsActivity::class.java).apply {
        }
        startActivity(intent)
    }
}