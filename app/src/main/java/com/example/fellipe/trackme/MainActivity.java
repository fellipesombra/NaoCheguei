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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.dto.TripInfo;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.enums.TransportType;
import com.example.fellipe.trackme.runnable.EstimatedTimeUpdater;
import com.example.fellipe.trackme.runnable.LocationTracker;
import com.example.fellipe.trackme.service.MapService;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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

    private static final String TAG = "MAINACTIVITY";

    @InjectView(R.id.estimatedTimeText)
    TextView _timeText;
    @InjectView(R.id.btn_car)
    ImageButton _carButton;
    @InjectView(R.id.btn_bike)
    ImageButton _bikeButton;
    @InjectView(R.id.btn_bus)
    ImageButton _busButton;
    @InjectView(R.id.btn_walking)
    ImageButton _walkingButton;

    @InjectView(R.id.btn_iniciar_viagem)
    ImageButton _startTripButton;
    @InjectView(R.id.btn_finalizar_viagem)
    ImageButton _endTripButton;

    @InjectView(R.id.my_toolbar)
    Toolbar myToolbar;

    private GoogleMap mMap;

    private Handler handler = new Handler();
    private Runnable locationTracker;
    private Runnable estimatedTimeUpdater;
    private SimpleLocation location;

    private MapService mapService;
    private TransportType activeTransportType;
    private Place activeDestination;


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


        mapService = new MapService(getResources().getString(R.string.google_distance_matrix_key), this, location);

        mapService.changeTransportType(TransportType.DRIVING);
        activeTransportType = TransportType.DRIVING;
        _carButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();

        setSupportActionBar(myToolbar);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String userId = sharedPref.getString(getString(R.string.user_id), null);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mapService.clear();
                mMap.clear();

                mapService.updateEstimatedTime(place);

                mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()));
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15);
                mMap.animateCamera(cameraUpdate);


                activeDestination = place;
                mapService.changeTransportType(activeTransportType);
                updateEstimatedTimeText();
            }

            @Override
            public void onError(Status status) {
                mMap.clear();
                mapService.clear();
                activeDestination = null;
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        if(userId != null) {
            Session.getInstance().setUserId(userId);
            //checkAtiveTrip();
        }else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        updateStartTripButton();
    }

    @Override
    public void onBackPressed() {
        // botão de voltar não fecha o aplicativo / deixa em segundo plano
        moveTaskToBack(true);
    }


    public void logout(){

        //TODO CASO ESTEJA EM VIAGEM = POPUP AVISANDO QUE VAI TERMINAR A VIAGEM COM CONFIRMAÇÃO DO USUÁRIO

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
        Session.getInstance().setUserId(null);
        Session.getInstance().setTrip(null);
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

    private void updateStartTripButton(){
        if(isTripActive()){
            _startTripButton.setVisibility(View.GONE);
            _endTripButton.setVisibility(View.VISIBLE);
        }else{
            _startTripButton.setVisibility(View.VISIBLE);
            _endTripButton.setVisibility(View.GONE);
        }
    }

    private void checkAtiveTrip() {

        String extraUrl = "/trip/user/"+ Session.getInstance().getUserId();
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extraUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getInt("responseStatus") == RestResponseStatus.OK.getStatusCode()) {
                                String tripId = String.valueOf(response.getInt("id"));
                                double lat = response.getDouble("latitude");
                                double lng = response.getDouble("longitude");
                                int estimatedTime = response.getInt("estimatedTime");
                                TripInfo trip = new TripInfo(tripId, new LatLng(lat,lng),estimatedTime);
                                Session.getInstance().setTrip(trip);
                            }else{
                                //erro ao recuperar viagem
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //erro ao recuperar viagem
                    }
                }){
        };

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }


    public void startTrip(View view) {
        if(activeDestination == null){
            Toast.makeText(getBaseContext(), "Adicione um destino!", Toast.LENGTH_SHORT).show();
            return;
        }else if(mapService.getActualEstimatedTime() == 0) {
            Toast.makeText(getBaseContext(), "Adicione uma estimativa de tempo!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getUserId());
        params.put("time", String.valueOf(mapService.getActualEstimatedTime()));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.map_rest_url)+"/trip", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            TripInfo trip = new TripInfo(String.valueOf(response.get("id")),activeDestination.getLatLng(),mapService.getActualEstimatedTime());
                            Session.getInstance().setTrip(trip);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        startTripSuccess();
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

    public void endTrip(View view) {

        String extra = "/trip/end/"+Session.getInstance().getTrip().getTripId();

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extra, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Session.getInstance().setTrip(null);
                        endTripSuccess();
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

    public void addTime(View view){
        if(activeDestination == null){
            Toast.makeText(this, "Selecione um destino primeiro!", Toast.LENGTH_LONG).show();
        }else if(!isTripActive()){
            mapService.addActualEstimatedTime(1800);
            updateEstimatedTimeText();
        }else{// em viagem
            String extra = "/trip/delay/"+Session.getInstance().getTrip().getTripId();

            CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extra, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mapService.addActualEstimatedTime(1800);
                            updateEstimatedTimeText();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(MainActivity.this, "Erro de conexão! Tente de novo mais tarde...", Toast.LENGTH_SHORT).show();
                        }
                    }){
            };

            MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
        }
    }

    private void startTripSuccess() {
        Toast.makeText(this, "Boa viagem!", Toast.LENGTH_LONG).show();

        updateStartTripButton();

        locationTracker = new LocationTracker(location,handler,this);
        estimatedTimeUpdater = new EstimatedTimeUpdater(mapService,handler,_timeText);

        handler.postDelayed(locationTracker, 0);
        handler.postDelayed(estimatedTimeUpdater, 0);
    }

    private void startTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
    }

    private void endTripSuccess() {
        Toast.makeText(this, "Viagem encerrada!", Toast.LENGTH_LONG).show();

        mapService.clear();
        mMap.clear();
        updateStartTripButton();
        updateEstimatedTimeText();
        centerMapUserLocation();

        location.endUpdates();
        handler.removeCallbacks(locationTracker);
        handler.removeCallbacks(estimatedTimeUpdater);
    }

    private boolean isTripActive(){
        return Session.getInstance().getTrip() != null;
    }

    private void endTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
    }


    public void changeToCar(View view){
        if(isTripActive()){
            return;
        }
        activeTransportType = TransportType.DRIVING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        _carButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();
    }

    public void changeToBus(View view){
        if(isTripActive()){
            return;
        }
        activeTransportType = TransportType.PUBLIC_TRANSPORT;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        _busButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();
    }

    public void changeToWalk(View view){
        if(isTripActive()){
            return;
        }
        activeTransportType = TransportType.WALKING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        _walkingButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();
    }

    public void changeToBike(View view){
        if(isTripActive()){
            return;
        }
        activeTransportType = TransportType.BICYCLING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        _bikeButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();
    }

    private void clearImageButtonBackGrounds() {
        _busButton.setBackground(null);
        _carButton.setBackground(null);
        _walkingButton.setBackground(null);
        _bikeButton.setBackground(null);
    }
    private void updateEstimatedTimeText() {
        _timeText.setText(mapService.getActualEstimatedTimeText());
    }

    public void centerMapUserLocation(){
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 15);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
           centerMapUserLocation();
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