package br.com.fellipe.naocheguei;

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
import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.enums.RestResponseStatus;
import br.com.fellipe.naocheguei.service.ContactService;
import br.com.fellipe.naocheguei.util.MyListAdapter;
import br.com.fellipe.naocheguei.util.rest.CustomRequest;
import br.com.fellipe.naocheguei.util.rest.MySingleton;
import br.com.fellipe.naocheguei.util.Session;

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

    ContactService contactService;

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

        listAdapter = new MyListAdapter(this);
        _listView.setAdapter(listAdapter);

        contactService = new ContactService(this,listAdapter);
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
                                contactService.updateContactList();
                                Toast.makeText(getBaseContext(), R.string.msg_contact_add_success, Toast.LENGTH_LONG).show();
                            }else if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_ALREADY_EXISTS.getStatusCode()){
                                Toast.makeText(getBaseContext(), R.string.msg_contact_already_exists, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getBaseContext(), R.string.msg_server_error, Toast.LENGTH_LONG).show();
                    }
                }){
        };

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    public void logout(){

        if(Session.getInstance().getTrip() != null){
            //TODO CASO ESTEJA EM VIAGEM = POPUP AVISANDO QUE VAI TERMINAR A VIAGEM COM CONFIRMAÇÃO DO USUÁRIO
        }

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
        Session.getInstance().setUserId(null);
        Session.getInstance().setTrip(null);
        Session.getInstance().getContacts().clear();
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
        inflater.inflate(R.menu.menu_contatos, menu);
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
