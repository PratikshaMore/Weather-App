package edu.umb.cs443;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    public final static String DEBUG_TAG = "edu.umb.cs443.MYMSG";
    private String url = "http://api.openweathermap.org/data/2.5/weather?";
    private String apiKey = "&APPID=f9a0da7858696d1453d0faa23006c2d9";
    private GoogleMap mMap;
    private String iconUrl = "http://openweathermap.org/img/w/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mFragment.getMapAsync(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void getWeatherInfo(View v) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String inputText = editText.getText().toString();
        if (inputText == null) return;

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String query = new String();

            //checking for zipcode validation
            if (inputText.matches("[0-9]+")) {

                query = url + "zip=" + inputText + ",us" + apiKey;
            } else {
             //if not zip code
                query = url + "q=" + inputText + apiKey;
            }
            new DownloadweatherInfoTask().execute(query);
        } else {
            Toast.makeText(getApplicationContext(), "No network connection available", Toast.LENGTH_SHORT);
        }


    }

    private class DownloadweatherInfoTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return null;
            }
        }


        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            String result = new String();
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.i(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();
                //Convert the InputStream to a target data type

                Reader reader = new InputStreamReader(is, "UTF-8");
                //URL Size Restriction to specific no. of chars
                int len = 8192;
                char[] buffer = new char[len];
                reader.read(buffer);
                result = new String(buffer);

            } finally {
                if (is != null) {
                    is.close();
                    Log.d(DEBUG_TAG, "Input stream is closed");
                }
            }
            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Log.i(DEBUG_TAG, "weather info is null");
                return;
            }
            try {
                //Parse	the	String	variable
                JSONObject jo = new JSONObject(result);
                JSONObject main = jo.getJSONObject("main");
                //temp conversion to C
                double temp = main.getDouble("temp");
                temp = temp - 273.15;

                JSONArray warray = jo.getJSONArray("weather");
                JSONObject weather = warray.getJSONObject(0);
                String icon = weather.getString("icon");
               // AsyncTask	to download the icon file
                new DownloadImageTask().execute(iconUrl + icon + ".png");

             //Get	the	latitude	and	longitude	of	the	specified	city from	the	returned	response.
                JSONObject coord = jo.getJSONObject("coord");
                double lng = coord.getDouble("lon");
                double lat = coord.getDouble("lat");

                //Move	the	camera	of	the	Google	Map
                float zoom = (float) Math.random() * 10 + 5;
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lng));
                CameraUpdate mzoom = CameraUpdateFactory.zoomTo(zoom);
                if (mMap != null) {
                    mMap.moveCamera(center);
                    mMap.animateCamera(mzoom);
                }

                //displaying the temperature
                TextView myTextView = (TextView) findViewById(R.id.textView);
                myTextView.setText(String.format("%.2f", temp) + "C");


            } catch (Exception e) {
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView image = (ImageView) findViewById(R.id.imageView);
            if (result != null) image.setImageBitmap(result);
            else {
                Log.i(DEBUG_TAG, "returned bitmap is null");
            }
        }
    }

    private Bitmap downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        Bitmap image = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.i(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            image = BitmapFactory.decodeStream(is);

        } catch (Exception e) {
            Log.i(DEBUG_TAG, e.toString());
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return image;
    }


    @Override
    public void onMapReady(GoogleMap map) {
        this.mMap = map;
    }
}
