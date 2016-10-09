package com.example.fellipe.trackme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.rest.CustomRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import im.delight.android.location.SimpleLocation;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private static final long DELAY = 1000*20;
    private static final String TAG = "MAINACTIVITY";

    @InjectView(R.id.input_time)
    EditText _timeText;
    @InjectView(R.id.btn_iniciar_viagem)
    Button _startTripButton;
    @InjectView(R.id.btn_finalizar_viagem)
    Button _endTripButton;
    @InjectView(R.id.my_toolbar)
    Toolbar myToolbar;

    private GoogleMap mMap;

    private Handler handler = new Handler();
    private Runnable runnable;
    private SimpleLocation location;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        location = new SimpleLocation(this);
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        setSupportActionBar(myToolbar);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String userId = sharedPref.getString(getString(R.string.user_id), null);
        String tripId = sharedPref.getString(getString(R.string.trip_id), null);


        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        _startTripButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startTrip();
            }
        });

        _endTripButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                endTrip();

            }
        });

        buttonsOffTrip();


        if(tripId !=  null) {
           //getTripInfo(tripId); Criar um objeto trip no session
        }

        if(userId != null) {
            Session.getInstance().setUserId(userId);
        }else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    public void logout(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
        Session.getInstance().setUserId(null);
        Session.getInstance().setTripId(null);
    }

    public void goToLoginPage(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    public void goToContactPage(){
        Intent intent = new Intent(getApplicationContext(), ContactActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                goToLoginPage();
                return true;
            case R.id.contatos:
                goToContactPage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void getTripInfo() {

        String extraUrl = "/trip/user/"+Session.getInstance().getUserId();
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extraUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.get("id") != null && !response.get("id").toString().isEmpty()) {
                                Session.getInstance().setTripId((String.valueOf(response.get("id"))));
                                buttonsOnTrip();
                            }else{
                                buttonsOffTrip();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }){
        };

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private void buttonsOnTrip(){
        _startTripButton.setVisibility(View.GONE);
        _timeText.setVisibility(View.GONE);
        _endTripButton.setVisibility(View.VISIBLE);
    }
    private void buttonsOffTrip(){
        _startTripButton.setVisibility(View.VISIBLE);
        _timeText.setVisibility(View.VISIBLE);
        _endTripButton.setVisibility(View.GONE);
    }

    public void startTrip() {

        String time = _timeText.getText().toString();
        if (time.isEmpty() || !time.matches("\\d+(?:\\.\\d+)?")) {
            _timeText.setError("Tempo invalido. Insira números.");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getUserId());
        params.put("time", time);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.map_rest_url)+"/trip", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Session.getInstance().setTripId((String.valueOf(response.get("id"))));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        startTripSucess();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        startTripFail();
                    }
                }){
        };
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    public void endTrip() {

        String extra = "/trip/end/"+Session.getInstance().getTripId();

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extra, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Session.getInstance().setTripId(null);
                        endTripSucess();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        endTripFail();
                    }
                }){
        };

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private void startTripSucess() {
        Toast.makeText(this, "Trip Started!", Toast.LENGTH_LONG).show();
        buttonsOnTrip();

        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    location.beginUpdates();
                    sendCurrentLocation(location.getLatitude(), location.getLongitude());
                    handler.postDelayed(this, DELAY);
                } catch (Exception e) {
                    Log.e("Handler", e.getLocalizedMessage(), e);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void startTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
    }

    private void endTripSucess() {
        Toast.makeText(this, "Trip Finished!", Toast.LENGTH_LONG).show();
        buttonsOffTrip();
        location.endUpdates();
        handler.removeCallbacks(runnable);
    }

    private void endTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
    }

    private void sendCurrentLocation(double latitude, double longitude) {
        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getTripId());
        params.put("lat", String.valueOf(latitude));
        params.put("lng", String.valueOf(longitude));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.map_rest_url)+"/position", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Trip","Enviado posição");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Trip","Fallha ao enviar posição");
                    }
                }){
        };
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 15);
            mMap.animateCamera(cameraUpdate);
        } else {
            Toast.makeText(getBaseContext(), "Permissão de localização é necessária!", Toast.LENGTH_LONG).show();
        }

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(curLocation));
    }

    @Override
    public void onLocationChanged(Location loc) {
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getBaseContext(), "Falha na conexão!", Toast.LENGTH_LONG).show();
    }
}