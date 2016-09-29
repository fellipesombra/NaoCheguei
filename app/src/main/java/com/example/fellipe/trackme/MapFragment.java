package com.example.fellipe.trackme;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.rest.CustomRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.InjectView;
import im.delight.android.location.SimpleLocation;

/**
 * Created by Fellipe on 29/09/2016.
 */
public class MapFragment extends Fragment {

    private static final long DELAY = 1000*20;

    private EditText _timeText;
    private Button _startTripButton;
    private Button _endTripButton;

    Context context;

    private Handler handler = new Handler();
    private Runnable runnable;
    private SimpleLocation location;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        location = new SimpleLocation(context);
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(context);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_map, container, false);

        _timeText = (EditText) view.findViewById(R.id.input_time);
        _startTripButton = (Button) view.findViewById(R.id.btn_iniciar_viagem);
        _endTripButton = (Button) view.findViewById(R.id.btn_finalizar_viagem);

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
        return view;
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

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
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

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
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

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }

    private void startTripSucess() {
        Toast.makeText(context, "Trip Started!", Toast.LENGTH_LONG).show();
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
        Toast.makeText(context, "Error!", Toast.LENGTH_LONG).show();
    }

    private void endTripSucess() {
        Toast.makeText(context, "Trip Finished!", Toast.LENGTH_LONG).show();
        buttonsOffTrip();
        location.endUpdates();
        handler.removeCallbacks(runnable);
    }

    private void endTripFail() {
        Toast.makeText(context, "Error!", Toast.LENGTH_LONG).show();
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

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }

}
