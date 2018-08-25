package br.com.fellipe.naocheguei.service;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import br.com.fellipe.naocheguei.IntroActivity;
import br.com.fellipe.naocheguei.R;
import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.enums.RestResponseStatus;
import br.com.fellipe.naocheguei.util.Session;
import br.com.fellipe.naocheguei.util.rest.CustomRequest;
import br.com.fellipe.naocheguei.util.rest.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.util.Session;
import br.com.fellipe.naocheguei.util.rest.MySingleton;

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
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(context.getString(br.com.fellipe.naocheguei.R.string.contact_rest_url)+url,
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
                            Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };
        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }

    public void updateContactList(){
        String url = "/"+ Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(context.getString(br.com.fellipe.naocheguei.R.string.contact_rest_url)+url,
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
                            Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
