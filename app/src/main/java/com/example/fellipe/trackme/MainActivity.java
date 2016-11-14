package com.example.fellipe.trackme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.example.fellipe.trackme.enums.HandlerMessagesCode;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.enums.TransportType;
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

import java.util.Date;
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
    @InjectView(R.id.btn_add_tempo)
    ImageButton _addTimeButton;

    @InjectView(R.id.my_toolbar)
    Toolbar myToolbar;

    private GoogleMap mMap;

    private Handler handler;
    private Runnable locationTracker;
    private SimpleLocation location;

    private MapService mapService;
    private TransportType activeTransportType;
    private LatLng activeDestination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        handler = new Handler(){
            public void handleMessage(Message msg) {
                if(msg.what == HandlerMessagesCode.TIME_FINISHED.getCode()) {
                    endTripSuccess();
                }
            }
        };

        location = new SimpleLocation(this);
        if (!location.hasLocationEnabled()) {
            // habilitar o gps
            SimpleLocation.openSettings(this);
        }

        setSupportActionBar(myToolbar);

        instantiateMap();
        defaultTripConfigurations();
        checkAtiveTrip();
        updateStartTripButton();

    }

    private void instantiateMap() {
        mapService = new MapService(getResources().getString(R.string.google_distance_matrix_key), this, location);

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


                activeDestination = place.getLatLng();
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

    private void defaultTripConfigurations(){
        mapService.changeTransportType(TransportType.DRIVING);
        activeTransportType = TransportType.DRIVING;
        _carButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border));
        updateEstimatedTimeText();
    }

    private void returnToActiveTrip(TripInfo trip){
        activeDestination = new LatLng(trip.getLatLng().latitude, trip.getLatLng().longitude);
        mapService.setActualEstimatedTime(trip.getEstimatedTime());
        mMap.addMarker(new MarkerOptions().position(trip.getLatLng()));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(trip.getLatLng(), 15);
        mMap.animateCamera(cameraUpdate);
        updateEstimatedTimeText();
        startTripSuccess();
    }

    private void checkAtiveTrip() {

        String extraUrl = "/trip/user/"+ Session.getInstance().getUserId();
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extraUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.OK.getStatusCode() && !response.isNull("trip")) {
                                JSONObject tripJSON = response.getJSONObject("trip");
                                String tripId = String.valueOf(tripJSON.getInt("id"));
                                double lat = tripJSON.getDouble("latitude");
                                double lng = tripJSON.getDouble("longitude");
                                long endTime = tripJSON.getLong("endTime");
                                int estimatedTime = calculateEstimatedTimeToDate(endTime);
                                TripInfo trip = new TripInfo(tripId, new LatLng(lat,lng),estimatedTime);
                                Session.getInstance().setTrip(trip);
                                returnToActiveTrip(trip);
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

    private int calculateEstimatedTimeToDate(long endTime) {
        return (int) (endTime - (new Date()).getTime())/1000;
    }


    public void startTrip(View view) {
        _startTripButton.setEnabled(false);
        boolean hasError = false;
        if(activeDestination == null){
            Toast.makeText(getBaseContext(), "Adicione um destino!", Toast.LENGTH_SHORT).show();
            hasError = true;
        }else if(mapService.getActualEstimatedTime() == 0) {
            Toast.makeText(getBaseContext(), "Adicione uma estimativa de tempo!", Toast.LENGTH_SHORT).show();
            hasError = true;
        }else if(!location.hasLocationEnabled()){
            Toast.makeText(getBaseContext(), "Ative o GPS para iniciar a viagem!", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if(hasError){
            _startTripButton.setEnabled(true);
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getUserId());
        params.put("time", String.valueOf(mapService.getActualEstimatedTime()));
        params.put("lat", String.valueOf(activeDestination.latitude));
        params.put("lng", String.valueOf(activeDestination.longitude));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.map_rest_url)+"/trip", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            TripInfo trip = new TripInfo(String.valueOf(response.get("id")),activeDestination,mapService.getActualEstimatedTime());
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
        _endTripButton.setEnabled(false);

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
        _addTimeButton.setEnabled(false);
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
                            _addTimeButton.setEnabled(true);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(MainActivity.this, "Erro de conexão! Tente de novo mais tarde...", Toast.LENGTH_SHORT).show();
                            _addTimeButton.setEnabled(true);
                        }
                    }){
            };

            MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
        }
    }

    private void startTripSuccess() {
        Toast.makeText(this, "Boa viagem!", Toast.LENGTH_LONG).show();
        _startTripButton.setEnabled(true);

        updateStartTripButton();

        locationTracker = new LocationTracker(location,handler,this,mapService,_timeText);
        handler.postDelayed(locationTracker, 0);
    }

    private void startTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        _startTripButton.setEnabled(true);
    }

    private void endTripSuccess() {
        Toast.makeText(this, "Viagem encerrada!", Toast.LENGTH_LONG).show();
        _endTripButton.setEnabled(true);

        mapService.clear();
        mMap.clear();
        Session.getInstance().setTrip(null);
        updateStartTripButton();
        updateEstimatedTimeText();
        centerMapUserLocation();

        location.endUpdates();
        handler.removeCallbacks(locationTracker);
    }

    private boolean isTripActive(){
        return Session.getInstance().getTrip() != null;
    }

    private void endTripFail() {
        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        _endTripButton.setEnabled(true);
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