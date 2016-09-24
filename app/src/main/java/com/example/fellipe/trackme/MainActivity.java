package com.example.fellipe.trackme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.fellipe.trackme.rest.CustomRequest;

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
public class MainActivity extends AppCompatActivity {

    private static final String TRIP_URL = "http://192.168.1.2:8080/onmyway-service/rest/map";
    private static final long DELAY = 1000*20;
    @InjectView(R.id.btn_cadastro_contatos)
    Button _registerContactsButton;
    @InjectView(R.id.input_time)
    EditText _timeText;
    @InjectView(R.id.btn_iniciar_viagem)
    Button _startTripButton;
    @InjectView(R.id.btn_finalizar_viagem)
    Button _endTripButton;

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

        buttonsOffTrip();
        //checkActiveTrip();

        _registerContactsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ContactActivity.class);
                startActivityForResult(intent, 0);
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

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void checkActiveTrip() {

        String extraUrl = "/trip/user/"+Session.getInstance().getUserId();
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, TRIP_URL+extraUrl, null,
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

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
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

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", Session.getInstance().getUserId());
        params.put("time", time);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, TRIP_URL+"/trip", params,
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

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    public void endTrip() {

        String extra = "/trip/end/"+Session.getInstance().getTripId();

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, TRIP_URL+extra, null,
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

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    private void startTripSucess() {
        Toast.makeText(getBaseContext(), "Trip Started!", Toast.LENGTH_LONG).show();
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
        Toast.makeText(getBaseContext(), "Error!", Toast.LENGTH_LONG).show();
    }

    private void endTripSucess() {
        Toast.makeText(getBaseContext(), "Trip Finished!", Toast.LENGTH_LONG).show();
        buttonsOffTrip();
        location.endUpdates();
        handler.removeCallbacks(runnable);
    }

    private void endTripFail() {
        Toast.makeText(getBaseContext(), "Error!", Toast.LENGTH_LONG).show();
    }

    private void sendCurrentLocation(double latitude, double longitude) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("id", Session.getInstance().getTripId());
        params.put("lat", String.valueOf(latitude));
        params.put("lng", String.valueOf(longitude));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, TRIP_URL+"/position", params,
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

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu_main; this adds items to the action bar if it is present.
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
}