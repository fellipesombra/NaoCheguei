package com.example.fellipe.trackme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.fellipe.trackme.service.ContactService;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class IntroActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 0;

    private ContactService contactService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        checkServertStatus();
    }

    public void continueToApp(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String userId = sharedPref.getString(getString(R.string.user_id), null);

        if(userId != null) {
            Session.getInstance().setUserId(userId);
            contactService = new ContactService(this);
            contactService.getAllUserContacts();
            Intent mainActivity = new Intent(this, MainActivity.class);
            startActivity(mainActivity);
        }else{
            Intent loginActivity = new Intent(this, LoginActivity.class);
            startActivityForResult(loginActivity, REQUEST_LOGIN);
        }
    }

    public void checkServertStatus() {
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.server_status_url), null,
                new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                        continueToApp();
                }
            },
                new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getBaseContext(), getString(R.string.msg_server_error), Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }
            }){
        };

        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES*4,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                Intent mainActivity = new Intent(this, MainActivity.class);
                startActivity(mainActivity);
            }
        }
    }
}
