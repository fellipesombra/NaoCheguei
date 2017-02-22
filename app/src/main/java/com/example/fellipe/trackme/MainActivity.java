package com.example.fellipe.trackme;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.example.fellipe.trackme.service.NotificationService;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
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

import static com.example.fellipe.trackme.enums.TransportType.BICYCLING;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_LOCATION_PERMISSION_REQUEST = 1;

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
    Button _startTripButton;
    @InjectView(R.id.btn_finalizar_viagem)
    Button _endTripButton;
    @InjectView(R.id.btn_add_tempo)
    Button _addTimeButton;

    @InjectView(R.id.my_toolbar)
    Toolbar myToolbar;

    int mNotificationId = 001;

    private GoogleMap mMap;

    private Handler handler;
    private Runnable locationTracker;
    private SimpleLocation location;

    private NotificationService notificationService;
    private MapService mapService;
    private TransportType activeTransportType;
    private LatLng activeDestination;

    private PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        notificationService = new NotificationService(this);

        handler = new Handler(){
            public void handleMessage(Message msg) {
                if(msg.what == HandlerMessagesCode.TIME_FINISHED.getCode()) {
                    endTripSuccess();
                    notifyUserAboutTrip(getString(R.string.notification_title_time_finished), getString(R.string.notification_desc_time_finished));
                }else if(msg.what == HandlerMessagesCode.ARRIVED_AT_DESTIONATION.getCode()){
                    endTripImpl();
                    notifyUserAboutTrip(getString(R.string.notification_title_arrived_destination), getString(R.string.notification_desc_arrived_destination));
                }else if(msg.what == HandlerMessagesCode.X_MINUTES_LEFT.getCode()){
                    notifyUserAboutTrip(getString(R.string.notification_title_5_min), getString(R.string.notification_desc_5_min));
                }
            }
        };

        setSupportActionBar(myToolbar);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "NaoChegueiViagemLock");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            instantiateMap();
            checkAtiveTrip();
            updateStartTripButton();
        }else{
            requestLocationPermission();
        }

    }


    private void instantiateMap() {
        location = new SimpleLocation(this);
        mapService = new MapService(getResources().getString(R.string.google_distance_matrix_key), this, location);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setHint("Adicionar destino");
        //especifica a busca no maps para endereços aqui do Brasil
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("BR")
                .build();
        autocompleteFragment.setFilter(typeFilter);
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
            }

            @Override
            public void onError(Status status) {
                mMap.clear();
                mapService.clear();
                activeDestination = null;
                Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // botão de voltar não fecha o aplicativo / deixa em segundo plano
        moveTaskToBack(true);
    }


    public void logout(){

        if(Session.getInstance().getTrip() != null){
            //TODO CASO ESTEJA EM VIAGEM = POPUP AVISANDO QUE VAI TERMINAR A VIAGEM COM CONFIRMAÇÃO DO USUÁRIO
        }

        activeDestination = null;
        activeTransportType = null;
        clearImageButtonBackGrounds();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
        Session.getInstance().setUserId(null);
        Session.getInstance().setTrip(null);
        Session.getInstance().getContacts().clear();
    }

    public void goToLoginPage(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    public void goToContactPage(){
        Intent intent = new Intent(getApplicationContext(), ContactActivity.class);
        startActivity(intent);
    }

    private void notifyUserAboutTrip(String title, String descrition) {
        NotificationCompat.Builder mBuilder = notificationService.createDefaultTextNotification(title, descrition);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        mNotificationId++;
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

    private void returnToActiveTrip(TripInfo trip){
        activeDestination = new LatLng(trip.getLatLng().latitude, trip.getLatLng().longitude);
        activeTransportType = trip.getTransportType();
        transportButtonSelected(activeTransportType);
        mapService.setActualEstimatedTime(trip.getEstimatedTime());
        mMap.addMarker(new MarkerOptions().position(trip.getLatLng()));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(trip.getLatLng(), 15);
        mMap.animateCamera(cameraUpdate);
        updateEstimatedTimeText();
        startTripSuccess();
    }

    private void transportButtonSelected(TransportType activeTransportType) {
        switch (activeTransportType){
            case WALKING:
                transportSelected(_walkingButton,R.drawable.walk_selected_32);
                break;
            case DRIVING:
                transportSelected(_carButton,R.drawable.car_selected_32);
                break;
            case BICYCLING:
                transportSelected(_bikeButton,R.drawable.bike_selected_32);
                break;
            case PUBLIC_TRANSPORT:
                transportSelected(_busButton,R.drawable.bus_selected_32);
                break;
            default:
                break;
        }
    }

    private void checkAtiveTrip() {

        String extraUrl = "/trip/user/"+ Session.getInstance().getUserId();
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extraUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.TRIP_FOUND.getStatusCode() && !response.isNull("trip")) {
                                JSONObject tripJSON = response.getJSONObject("trip");
                                String tripId = String.valueOf(tripJSON.getInt("id"));
                                double lat = tripJSON.getDouble("latitude");
                                double lng = tripJSON.getDouble("longitude");
                                long endTime = tripJSON.getLong("endTime");
                                String transport = (tripJSON.getString("transport"));
                                int estimatedTime = calculateEstimatedTimeToDate(endTime);
                                TripInfo trip = new TripInfo(tripId, new LatLng(lat,lng),estimatedTime, TransportType.getByName(transport));
                                Session.getInstance().setTrip(trip);
                                returnToActiveTrip(trip);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private int calculateEstimatedTimeToDate(long endTime) {
        return (int) (endTime - (new Date()).getTime())/1000;
    }

    private Dialog buildAddContactDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_no_contacts)
                .setMessage(R.string.dialog_msg_no_contacts)
                .setPositiveButton(R.string.msg_ok,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        goToContactPage();
                    }
                });
        return builder.create();
    }

    public void startTrip(View view) {
        _startTripButton.setEnabled(false);
        boolean hasError = false;
        if(activeDestination == null) {
            Toast.makeText(getBaseContext(), R.string.msg_error_add_destiny, Toast.LENGTH_SHORT).show();
            hasError = true;
        }else if(activeTransportType == null){
            Toast.makeText(getBaseContext(), R.string.msg_error_select_transport_type, Toast.LENGTH_SHORT).show();
            hasError = true;
        }else if(mapService.getActualEstimatedTime() == 0) {
            Toast.makeText(getBaseContext(), R.string.msg_error_add_estimated_time, Toast.LENGTH_SHORT).show();
            hasError = true;
        }else if(!location.hasLocationEnabled()){
            Toast.makeText(getBaseContext(), R.string.msg_error_activate_gps, Toast.LENGTH_SHORT).show();
            SimpleLocation.openSettings(this);
            hasError = true;
        }else if(Session.getInstance().getContacts().isEmpty()){
            buildAddContactDialog().show();
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
        params.put("transport", activeTransportType.getName());

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.map_rest_url)+"/trip", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(!response.isNull("id")) {
                            try {
                                TripInfo trip = new TripInfo(String.valueOf(response.get("id")), activeDestination, mapService.getActualEstimatedTime(), activeTransportType);
                                Session.getInstance().setTrip(trip);
                            } catch (JSONException e) {
                                Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                            }
                            startTripSuccess();
                        }else{
                            startTripFail();
                        }
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
        endTripImpl();
    }

    private void endTripImpl() {
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
        if(activeDestination == null) {
            Toast.makeText(getBaseContext(), R.string.msg_error_add_destiny, Toast.LENGTH_SHORT).show();
            _addTimeButton.setEnabled(true);
        }else if(activeTransportType == null){
            Toast.makeText(getBaseContext(), R.string.msg_error_select_transport_type, Toast.LENGTH_SHORT).show();
            _addTimeButton.setEnabled(true);
        }else if(!isTripActive()){
            mapService.addActualEstimatedTime(900);
            updateEstimatedTimeText();
            _addTimeButton.setEnabled(true);
        }else{// em viagem
            String extra = "/trip/delay/"+Session.getInstance().getTrip().getTripId();

            CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.map_rest_url)+extra, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mapService.addActualEstimatedTime(900);
                            updateEstimatedTimeText();
                            _addTimeButton.setEnabled(true);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
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

        mapService.setCurrentDestination(activeDestination);
        location.beginUpdates();
        locationTracker = new LocationTracker(location,handler,this,mapService,_timeText);
        handler.postDelayed(locationTracker, 0);

        wakeLock.acquire();
    }

    private void startTripFail() {
        Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
        _startTripButton.setEnabled(true);
    }

    private void endTripSuccess() {
        Toast.makeText(this, "Viagem encerrada!", Toast.LENGTH_LONG).show();
        _endTripButton.setEnabled(true);
        activeDestination = null;
        activeTransportType = null;
        clearImageButtonBackGrounds();

        mapService.clear();
        mMap.clear();
        Session.getInstance().setTrip(null);
        updateStartTripButton();
        updateEstimatedTimeText();
        centerMapUserLocation();

        location.endUpdates();
        handler.removeCallbacks(locationTracker);
        wakeLock.release();
    }

    private boolean isTripActive(){
        return Session.getInstance().getTrip() != null;
    }

    private void endTripFail() {
        Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
        _endTripButton.setEnabled(true);
    }


    public void changeToCar(View view){
        if(isTripActive() || activeDestination == null){
            return;
        }
        activeTransportType = TransportType.DRIVING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        transportSelected(_carButton,R.drawable.car_selected_32);
        updateEstimatedTimeText();
    }

    public void changeToBus(View view){
        if(isTripActive() || activeDestination == null){
            return;
        }
        activeTransportType = TransportType.PUBLIC_TRANSPORT;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        transportSelected(_busButton,R.drawable.bus_selected_32);
        updateEstimatedTimeText();
    }

    public void changeToWalk(View view){
        if(isTripActive() || activeDestination == null){
            return;
        }
        activeTransportType = TransportType.WALKING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        transportSelected(_walkingButton,R.drawable.walk_selected_32);
        updateEstimatedTimeText();
    }

    public void changeToBike(View view){
        if(isTripActive() || activeDestination == null){
            return;
        }
        activeTransportType = BICYCLING;
        mapService.changeTransportType(activeTransportType);

        clearImageButtonBackGrounds();
        transportSelected(_bikeButton,R.drawable.bike_selected_32);
        updateEstimatedTimeText();
    }

    private void transportSelected(ImageButton transportButton, int drawableId) {
        transportButton.setBackgroundColor(Color.WHITE);
        transportButton.setBackground(ContextCompat.getDrawable(this,R.drawable.border_top));
        transportButton.setImageDrawable(ContextCompat.getDrawable(this,drawableId));
    }

    private void clearImageButtonBackGrounds() {

        _busButton.setBackground(null);
        _carButton.setBackground(null);
        _walkingButton.setBackground(null);
        _bikeButton.setBackground(null);

        _busButton.setBackgroundColor(Color.TRANSPARENT);
        _carButton.setBackgroundColor(Color.TRANSPARENT);
        _walkingButton.setBackgroundColor(Color.TRANSPARENT);
        _bikeButton.setBackgroundColor(Color.TRANSPARENT);

        _busButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.bus_32));
        _carButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.car_32));
        _walkingButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.walk_32));
        _bikeButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.bike_32));
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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            centerMapUserLocation();
        }
    }

    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    instantiateMap();
                    checkAtiveTrip();
                    updateStartTripButton();
                } else {
                    Toast.makeText(getBaseContext(), R.string.msg_location_permission_necessary, Toast.LENGTH_LONG).show();
                    requestLocationPermission();
                }
                return;
            }
        }
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
        Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_SHORT).show();
    }
}