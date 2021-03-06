package com.robertsolutions.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "Sunshine";

    private  ArrayAdapter<String> mForecastAdapter;



    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //You should place your menu items in to menu.
        //onCreateOptionsMenu >> For this method to be called, you must have first called
        setHasOptionsMenu(true);

    }

    public View rootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView =  inflater.inflate(R.layout.fragment_main, container, false);


        String[] forecastArray = {
                "Today - Sunny - 30º/33º",
                "Tomorrow - Sunny - 30º/33º",
                "Monday - Sunny - 30º/33º",
                "Tuesday - Rain - 30º/33º",
                "Wednesday - Foggy - 30º/33º",
                "Thursday - Cloudy - 30º/33º",
                "Friday - Heavy Rain - 30º/33º",
                "Saturday - Sunny - 30º/33º",
                "Sunday - Sunny - 30º/33º",
        };
        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray)
        );

         mForecastAdapter = new ArrayAdapter<String>(
                //The current context
                getActivity(),
                //ID of list item layout
                R.layout.list_item_forecast,
                //ID of the textview to populate
                R.id.list_item_forecast_textview,
                //Forecast data
                weekForecast);

        ListView mListView = (ListView) rootView.findViewById(R.id.list_item_forecast);

        mListView.setAdapter(mForecastAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
//                int duration = Toast.LENGTH_SHORT;
//                Toast toast = Toast.makeText(getActivity(), forecast, duration);
//                toast.show();

                try {
                    Intent intent = new Intent(getActivity(), DetailActivity.class);
                    intent.putExtra(Intent.EXTRA_TEXT, forecast);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
            }
        });


        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecast_fragment, menu);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        // Handle item selection
        switch (id) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            case R.id.action_settings:
                 intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            case R.id.action_map:
                showMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showMap() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String city = sp.getString("pref_location_key", "Madrid");
        Uri uriGeo = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", city)
                .build();

        String myUrl = uriGeo.toString();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uriGeo);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void updateWeather()
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String city = sp.getString("pref_location_key","Madrid");
        FetchWeatherTask fwt = new FetchWeatherTask();
        fwt.execute(city);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = sp.getString("pref_temp_key","Metric");
            if(units == "imperial")
            {
                /*
                TODO
                Convert metric to imperial inside result
                */
            }

            if (result != null) {

                mForecastAdapter.clear();
                for (String day : result) {
                    mForecastAdapter.add(day);
                }
            }

        }

        @Override
        protected String[] doInBackground(String... param) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;


            Uri.Builder uriBuilder = new Uri.Builder();
            String city = param[0];
            String mode = "json";

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = sp.getString("pref_temp_key","Metric");

            int days = 7;
            String[] weekForecast = new String[0];

            uriBuilder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data/2.5/forecast/daily")
                    .appendQueryParameter("q", city)
                    .appendQueryParameter("mode", mode)
                    .appendQueryParameter("units", units)
                    .appendQueryParameter("cnt", String.valueOf(days));

            String myUrl = uriBuilder.build().toString();

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=Madrid&mode=json&units=metric&cnt=7");

                URL url = new URL(myUrl);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                weekForecast = getWeatherDataFromJson(forecastJsonStr, days);

                Log.v(LOG_TAG, "Forecast JSON string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            return weekForecast;
        }

//        public double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
//                throws JSONException {
//            // TODO: add parsing code here
//            JSONObject jsonObject = new JSONObject(weatherJsonStr);
//            JSONArray weatherArray = jsonObject.getJSONArray("list");
//            JSONObject dayForecast = weatherArray.getJSONObject(dayIndex);
//            JSONObject temperatureObject = dayForecast.getJSONObject("temp");
//            double tempMax = temperatureObject.getDouble("max");
//            return tempMax;
//        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
             * so for convenience we're breaking it out into its own method now.
             */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }
    }
}
