package com.example.fellipe.trackme;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.fellipe.trackme.rest.CustomRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.InjectView;

/**
 * Created by Fellipe on 29/09/2016.
 */
public class ContactFragment extends Fragment {

    EditText _phoneText;
    EditText _emailText;
    Button _registerButton;
    ListView _listView ;

    ArrayList<String> contactList = new ArrayList<>();
    private ArrayAdapter<String> listAdapter ;

    Context context;

    public ContactFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        listAdapter = new ArrayAdapter<>(context, R.layout.simple_row, contactList);

        getAllUserContacts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_contact, container, false);

        _phoneText = (EditText) view.findViewById(R.id.input_phone);
        _emailText = (EditText) view.findViewById(R.id.input_contact_email);
        _registerButton = (Button) view.findViewById(R.id.btn_register);
        _listView= (ListView) view.findViewById(R.id.list);

        _listView.setAdapter(listAdapter);

        _registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerContact();
            }
        });

        return view;
    }

    private void getAllUserContacts() {
        listAdapter.clear();
        String url = "/"+Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest( getString(R.string.contact_rest_url)+url,
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


        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
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

        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getUserId());
        params.put("email", email);
        params.put("phone", phone);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.contact_rest_url), params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, "Success", Toast.LENGTH_LONG).show();
                        getAllUserContacts();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Error!", Toast.LENGTH_LONG).show();
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
