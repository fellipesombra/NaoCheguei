package com.example.fellipe.trackme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.fellipe.trackme.dto.ContatoDTO;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.util.MyListAdapter;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;
import com.example.fellipe.trackme.util.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Fellipe on 29/09/2016.
 */
public class ContactActivity extends AppCompatActivity {

    @InjectView(R.id.input_contact_email)
    EditText _emailText;
    @InjectView(R.id.btn_register)
    FloatingActionButton _registerButton;
    @InjectView(R.id.list)
    ListView _listView ;
    @InjectView(R.id.my_toolbar_contatos)
    Toolbar myToolbar;


    ArrayList<ContatoDTO> contactList = new ArrayList<>();
    private ArrayAdapter<ContatoDTO> listAdapter ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        ButterKnife.inject(this);

        setSupportActionBar(myToolbar);

        _registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerContact();
            }
        });

        listAdapter = new MyListAdapter(this, contactList);

        getAllUserContacts();

        _listView.setAdapter(listAdapter);

    }

    private void getAllUserContacts() {
        listAdapter.clear();
        String url = "/"+ Session.getInstance().getUserId();
        JsonArrayRequest jsObjRequest = new JsonArrayRequest( getString(R.string.contact_rest_url)+url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                String email = (String) response.getJSONObject(i).get("email");
                                String id = String.valueOf(response.getJSONObject(i).get("id"));
                                listAdapter.add( new ContatoDTO(id, email));
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
                        Log.d("ContactActivity", "Erro na requisição REST.");
                    }
                }){
        };


        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private void registerContact() {

        String email = _emailText.getText().toString();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Entre um email válido");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getUserId());
        params.put("email", email);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, getString(R.string.contact_rest_url), params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_ADD.getStatusCode()) {
                                Toast.makeText(getBaseContext(), "Contato adicionado", Toast.LENGTH_LONG).show();
                                getAllUserContacts();
                            }else if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_ALREADY_EXISTS.getStatusCode()){
                                Toast.makeText(getBaseContext(), "Contato já existente", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(getBaseContext(), "Erro no servidor. Tente novamente mais tarde", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getBaseContext(), "Erro no servidor. Tente novamente mais tarde", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getBaseContext(), "Erro no servidor. Tente novamente mais tarde", Toast.LENGTH_LONG).show();
                    }
                }){
        };
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    public void logout(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
        Session.getInstance().setUserId(null);
        Session.getInstance().setTrip(null);
    }

    public void goToLoginPage(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    public void goToTripPage(){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
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
            case R.id.viagem:
                goToTripPage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
