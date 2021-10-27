package com.example.heavyrainalert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.heavyrainalert.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

class httpHandler extends Thread{
    String tmp_url;
    BufferedReader in;

    public void setTmp_url(String url){
        this.tmp_url = url;
    }

    public void run(){

        try {
            //Cria um objeto para guardar a URL
            URL url = new URL(tmp_url);

            //Abre uma conexao com a URL especificada
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            //Define propriedades de request
            connection.setRequestProperty("accept", "application/json");

            //Faz o request e armazena o response
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ERRO");
        }
    }

    public BufferedReader getResponse(){
        return in;
    }
}

public class MainActivity extends AppCompatActivity {



    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    FusedLocationProviderClient fusedLocationClient;
    int PERMISSION_ID = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        getLastLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        }else{//coordenadas para mandar pra api
                            callAPI(location.getLatitude(), location.getLongitude());
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(5);
        locationRequest.setFastestInterval(0);
        locationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private LocationCallback locationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location lastLocation = locationResult.getLastLocation();

            callAPI(lastLocation.getLatitude(), lastLocation.getLongitude());
        }
    };



    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    public void callAPI(double lat, double longt){

        //pegando o tempo atual
        String api_key = new String("76b42f207958d3328b4282874cb0d1bd");
        String tmp_url = new String("https://api.openweathermap.org/data/2.5/weather?");
        tmp_url = tmp_url + "lat=" + Double.toString(lat) + "&lon=" + Double.toString(longt);
        tmp_url = tmp_url + "&appid=" + api_key + "&units=metric&lang=pt_br";

        //criando thread para acesso a internet
        httpHandler requestThread = new httpHandler();
        requestThread.start();
        requestThread.setTmp_url(tmp_url);
        requestThread.run();

        //pegando response do request http
        BufferedReader in = requestThread.getResponse();

        JSONParser parser = new JSONParser();

        try {
            //processamento do json
            Object obj = parser.parse(in);
            JSONObject object = (JSONObject) obj;
            JSONArray array = (JSONArray) object.get("weather");
            JSONObject object1 = (JSONObject) array.get(0);
            ((TextView)findViewById(R.id.textview_second)).setText(object1.get("description").toString());

            JSONObject atual = (JSONObject)object.get("main");
            int temp = (int)Math.round((Double)(atual.get("temp")));
            ((TextView)findViewById(R.id.textview_third)).setText(Integer.toString(temp) + "°C");

            ((TextView)findViewById(R.id.textview_first)).setText(object.get("name").toString());

            //escolhendo o icone certo de acordo com o id
            String str_icone = object1.get("icon").toString();
            int icone = 0;

            if(str_icone.equals("01d")){
                icone = R.drawable._01d;
            }
            else if(str_icone.equals("01n")){
                icone = R.drawable._01n;
            }
            else if(str_icone.equals("02d")){
                icone = R.drawable._02d;
            }
            else if(str_icone.equals("02n")){
                icone = R.drawable._02n;
            }
            else if(str_icone.equals("03d")){
                icone = R.drawable._03d;
            }
            else if(str_icone.equals("03n")){
                icone = R.drawable._03n;
            }
            else if(str_icone.equals("04d")){
                icone = R.drawable._04d;
            }
            else if(str_icone.equals("04n")){
                icone = R.drawable._04n;
            }
            else if(str_icone.equals("09d")){
                icone = R.drawable._09d;
            }
            else if(str_icone.equals("09n")){
                icone = R.drawable._09n;
            }
            else if(str_icone.equals("10d")){
                icone = R.drawable._10d;
            }
            else if(str_icone.equals("10n")){
                icone = R.drawable._10n;
            }
            else if(str_icone.equals("11d")){
                icone = R.drawable._11d;
            }
            else if(str_icone.equals("11n")){
                icone = R.drawable._11n;
            }
            else if(str_icone.equals("13d")){
                icone = R.drawable._13d;
            }
            else if(str_icone.equals("13n")){
                icone = R.drawable._13n;
            }
            else if(str_icone.equals("50d")){
                icone = R.drawable._50d;
            }
            else if(str_icone.equals("50n")){
                icone = R.drawable._50n;
            }


            ((ImageView)findViewById(R.id.icon_weather)).setImageResource(icone);

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ERRO");
        }

        //pegando a previsao do tempo
        tmp_url = new String("https://api.openweathermap.org/data/2.5/forecast?");
        tmp_url = tmp_url + "lat=" + Double.toString(lat) + "&lon=" + Double.toString(longt);
        tmp_url = tmp_url + "&appid=" + api_key + "&units=metric&lang=pt_br&cnt=8";

        requestThread.setTmp_url(tmp_url);
        requestThread.run();

        //pegando response do request http
        in = requestThread.getResponse();

        parser = new JSONParser();

        try {
            //processamento do json
            Object obj = parser.parse(in);
            JSONObject object = (JSONObject) obj;
            JSONArray array = (JSONArray) object.get("list");

            ((TextView)findViewById(R.id.textview_fourth)).setText("Não há previsão de chuvas fortes nas próximas 24h");


            Long heavy_rain_ids[] = {202L, 212L, 221L, 232L, 502L, 503L, 504L, 522L, 531L};
            for (int i = 0; i < 8; ++i){
                JSONObject tmp = (JSONObject) array.get(i);
                JSONArray arrweather = (JSONArray) tmp.get("weather");
                JSONObject weather = (JSONObject) arrweather.get(0);
                long weather_id = (long)weather.get("id");

                if(Arrays.asList(heavy_rain_ids).contains(weather_id)){
                    //mudando a mensagem do app caso hajam chuvas fortes
                    ((TextView)findViewById(R.id.textview_fourth)).setText("Há previsão de chuvas fortes nas próximas 24h");
                    ((TextView)findViewById(R.id.textview_fourth)).setTextColor(Color.RED);
                }
            }
        }catch (Exception e){

            System.out.println(e);
            System.out.println("ERRO");
        }
    }
}

