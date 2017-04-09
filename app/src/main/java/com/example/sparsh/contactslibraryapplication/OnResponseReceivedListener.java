package com.example.sparsh.contactslibraryapplication;

import org.json.JSONObject;

/**
 * Created by sparsh on 4/9/17.
 */

public interface OnResponseReceivedListener {
    public void onResponseReceived(String responseString,JSONObject responseObject);
}