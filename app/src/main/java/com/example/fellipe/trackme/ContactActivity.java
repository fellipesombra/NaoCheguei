package com.example.fellipe.trackme;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.fellipe.trackme.rest.CustomRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class ContactActivity extends AppCompatActivity {
    private static final String CONTACT_URL = "http://192.168.1.2:8080/onmyway-service/rest/contact";

    @InjectView(R.id.input_phone)
    EditText _phoneText;
    @InjectView(R.id.input_contact_email)
    EditText _emailText;
    @InjectView(R.id.btn_register)
    Button _registerButton;
    @InjectView(R.id.list)
    ListView listView ;

    ArrayList<String> contactList = new ArrayList<String>();

    private ArrayAdapter<String> listAdapter ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        ButterKnife.inject(this);


        listAdapter = new ArrayAdapter<String>(this, R.layout.simple_row, contactList);

        getAllUserContacts();
        listView.setAdapter(listAdapter);

        _registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerContact();
            }
        });

    }

    private void getAllUserContacts() {
        listAdapter.clear();
        String url = "/"+Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest( CONTACT_URL+url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                listAdapter.add((String)response.getJSONObject(i).get("email"));
                            }
                            listAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("teste", "error");
                    }
                }){
        };


        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    private void registerContact() {

        String phone = _phoneText.getText().toString();
        String email = _emailText.getText().toString();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Entre um email válido");
            return;
        }
        if(!phone.matches("\\d+(?:\\.\\d+)?")) {
            _phoneText.setError("Somente numeros");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", Session.getInstance().getUserId());
        params.put("email", email);
        params.put("phone", phone);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, CONTACT_URL, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_LONG).show();
                        getAllUserContacts();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getBaseContext(), "Error!", Toast.LENGTH_LONG).show();
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
}
