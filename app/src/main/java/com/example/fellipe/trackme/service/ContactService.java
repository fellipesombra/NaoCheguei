package com.example.fellipe.trackme.service;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.fellipe.trackme.IntroActivity;
import com.example.fellipe.trackme.R;
import com.example.fellipe.trackme.dto.ContatoDTO;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Fellipe on 20/11/2016.
 */

public class ContactService {

    private Context context;
    private ArrayAdapter<ContatoDTO> listAdapter ;

    public ContactService(Context context, ArrayAdapter<ContatoDTO> listAdapter) {
        this.context = context;
        this.listAdapter = listAdapter;
    }

    public ContactService(Context context) {
        this.context = context;
    }


    public void getAllUserContacts() {
        String url = "/"+ Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(context.getString(R.string.contact_rest_url)+url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Session.getInstance().getContacts().clear();
                            for (int i = 0; i < response.length(); i++) {
                                String email = (String) response.getJSONObject(i).get("email");
                                String id = String.valueOf(response.getJSONObject(i).get("id"));
                                Session.getInstance().getContacts().add(new ContatoDTO(id, email));
                            }
                        } catch (JSONException e) {
                            Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };
        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }

    public void updateContactList(){
        String url = "/"+ Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(context.getString(R.string.contact_rest_url)+url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Session.getInstance().getContacts().clear();
                            for (int i = 0; i < response.length(); i++) {
                                String email = (String) response.getJSONObject(i).get("email");
                                String id = String.valueOf(response.getJSONObject(i).get("id"));
                                Session.getInstance().getContacts().add(new ContatoDTO(id, email));
                            }
                            listAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
