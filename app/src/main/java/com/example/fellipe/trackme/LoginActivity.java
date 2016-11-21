package com.example.fellipe.trackme;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.fellipe.trackme.service.ContactService;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.Session;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class LoginActivity extends AppCompatActivity {
    private static final int REQUEST_SIGNUP = 0;

    @InjectView(R.id.input_email) EditText _emailText;
    @InjectView(R.id.input_password) EditText _passwordText;
    @InjectView(R.id.btn_login) Button _loginButton;

    private ContactService contactService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        contactService = new ContactService(this);

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    public void signUp(View view){
        Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
        startActivityForResult(intent, REQUEST_SIGNUP);
    }

    public void login() {
        if (!validate()) {
            onLoginFailed(getString(R.string.msg_error_validate_infos));
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Contectando...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        authenticate(email,password);

        progressDialog.dismiss();
    }

    private void authenticate(final String email, final String password) {
        String queryParam = "?email="+email+"&password="+password;
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, getString(R.string.user_rest_url)+queryParam, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String userId = String.valueOf(response.get("id"));
                            if(Integer.valueOf(userId) > 0){
                                Session.getInstance().setUserId(userId);
                                onLoginSuccess();
                            }else if (Integer.valueOf(userId) == 0){
                                onLoginFailed(getString(R.string.msg_error_password_incorrect));
                            }else{
                                onLoginFailed(getString(R.string.msg_error_user_not_registered));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoginFailed(getString(R.string.msg_server_error));
                    }
                }){

        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                addUserIdToSharedPrefs();
                contactService.getAllUserContacts();
                setResult(RESULT_OK, null);
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void addUserIdToSharedPrefs(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user_id),Session.getInstance().getUserId());
        editor.commit();
    }

    public void onLoginSuccess() {
        Toast.makeText(getBaseContext(), "Bem vindo!", Toast.LENGTH_LONG).show();
        addUserIdToSharedPrefs();
        contactService.getAllUserContacts();
        _loginButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    public void onLoginFailed(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError(getString(R.string.msg_error_valid_email));
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError(getString(R.string.msg_error_password_characteres));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
}