package com.example.sparsh.contactslibraryapplication;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.people.v1.People;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;


import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnResponseReceivedListener {

    private static final int RC_AUTHORIZE_CONTACTS = 100;
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String REQUEST_URL = "https://www.google.com/m8/feeds/contacts/default/full?alt=json";


    private Button bImport = null;
    private GoogleApiClient mClient = null;
    private Account authorizedAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bImport = (Button) findViewById(R.id.b_contacts);
        bImport.setEnabled(false);

        bImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authorizeContactsAccess();
            }
        });

        // build google api client for authorization
        buildGoogleApiClient();
    }

    private void buildGoogleApiClient() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder().requestEmail()
                .requestScopes(new Scope("https://www.google.com/m8/feeds/")).build();

        mClient = new GoogleApiClient.Builder(this, this, this).addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions).build();
    }

    @Override
    protected void onResume() {
        mClient.connect();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mClient.disconnect();
        super.onPause();
    }

    private void authorizeContactsAccess() {
        if (mClient.isConnected()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mClient);
            startActivityForResult(signInIntent, RC_AUTHORIZE_CONTACTS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_AUTHORIZE_CONTACTS) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount googleSignInAccount = result.getSignInAccount();
                authorizedAccount = googleSignInAccount.getAccount();
                getContacts();
            }
        }
    }

    private void getContacts() {
        GetContactsTask task = new GetContactsTask(authorizedAccount,this);
        task.execute();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        // Enable import button
        bImport.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Some error occurred");
        Log.d(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
    }

    @Override
    public void onResponseReceived(String responseString, JSONObject responseObject) {
        Log.d(TAG, "onResponseReceived: " + responseString);
    }


    public class GetContactsTask extends AsyncTask<Void, Void, String> {
        Account mAccount;
        OnResponseReceivedListener mListener;

        public GetContactsTask(Account mAccount , OnResponseReceivedListener mListener) {
            this.mAccount = mAccount;
            this.mListener = mListener;
        }

        @Override
        protected String doInBackground(Void... params) {

            String accessToken = null;

            try {
                GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(MainActivity.this,
                        Collections.singleton("https://www.google.com/m8/feeds/"));
                mCredential.setSelectedAccount(mAccount);

                // Retrieve access token
                accessToken = mCredential.getToken();

            } catch (IOException | GoogleAuthException e) {
                e.printStackTrace();
            }
            return accessToken;
        }

        @Override
        protected void onPostExecute(final String accessToken) {
            Log.d(TAG, "onPostExecute: accessToken " + accessToken);
            // Use volley to get the contacts result from google api
            JsonObjectRequest mRequest = new JsonObjectRequest(Request.Method.GET,
                    REQUEST_URL,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mListener.onResponseReceived(response.toString(),response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "onErrorResponse: " + error.getMessage());
                        }
                    }){

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> headers = new HashMap<>();

                    final String authorizationHeader = "Bearer"+ " " + accessToken;

                    headers.put("Authorization" , authorizationHeader);
                    return headers;
                }
            };

            AppController.getInstance().getRequestQueue().add(mRequest);
        }
    }


}
