package com.mycompany.CMSBHelpdesk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.sharedPreference;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Settings extends ActionBarActivity {

    protected Button mSettingsBtn;
    public String username, password, server, loginID;
    //Login class "AttemptLogin" and "JSONParser" is from mrbool.com
    private EditText user;
    private EditText pass;
    private static EditText ip;
    int success;
    public static int newLogin;

    // DB Class to perform DB related operations
    DBController control = new DBController(this);
    // Progress Dialog
    private ProgressDialog pDialog;
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    private static String LOGIN_URL = "";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    public static final String LOG_TAG = "Requesting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //Default Back Button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //initialise
        user = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.password);
        ip = (EditText) findViewById(R.id.server);
        mSettingsBtn = (Button)findViewById(R.id.settingsBtn);

        username = sharedPreference.getString(this, "username");
        password = sharedPreference.getString(this,"password");
        server = sharedPreference.getString(this,"server");

        if(!username.equals("") && !password.equals("") && !server.equals("")){
            user.setText(username);
            pass.setText(password);
            ip.setText(server);

            mSettingsBtn.setTextAppearance(this, R.style.submitButton);
        }

        addListenerOnButton();
    }

    public boolean connectionCheck(){
        //check Internet connection
        if(isNetworkConnected(this)) {
            try {
                if (new checkConnection().execute(this).get()) {
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }else{
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.nowifi)
                    .setTitle("No internet connection")
                    .setMessage("Please turn on mobile data or wifi")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //code for exit
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                            Settings.this.finish();
                        }

                    })
                    .show();
            return false;
        }
        return false;
    }

    //To check if internet is actually connected
    //Have to run it in AsyncTask or else you'll get a NetworkOnMain exception error
    class checkConnection extends AsyncTask<Context, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Context... contexts) {
            return hasInternetConnection(contexts[0]);
        }
    }
    public static boolean isNetworkConnected(Context context) {
        final ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }
    public static boolean hasInternetConnection(Context context) {
        if (isNetworkConnected(context)) {
            try {
                //TODO: change the url it checks
                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error checking internet connection", e);
            }
        } else {
            Log.d(LOG_TAG, "No network available!");
        }
        return false;
    }


    class AttemptLogin extends AsyncTask<String, String, String> {

        //Before starting background thread Show Progress Dialog
        boolean failure = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Settings.this);
            pDialog.setMessage("Attempting to login...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // here Check for success tag
            String username = user.getText().toString();
            String password = pass.getText().toString();
            try {

                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", username));
                params.add(new BasicNameValuePair("password", password));

                Log.d("request!", "starting");

                JSONObject json = jsonParser.makeHttpRequest(
                        LOGIN_URL, "POST", params);

                // checking  log for json response
                Log.d("Login attempt", json.toString());

                // success tag for json
                sharedPreference.setInt(Settings.this, "success", json.getInt(TAG_SUCCESS));
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    Log.d("Successful Login", json.toString());
                    //sending shared preference to keep user logged in even if they close the app
                    sharedPreference.setString(Settings.this, "login", user.getText().toString());
                    sharedPreference.setString(Settings.this, "pass", pass.getText().toString());
                    sharedPreference.setString(Settings.this, "ip", ip.getText().toString());
                    sharedPreference.setString(Settings.this, MainActivity.TAG_LOGIN_ID, json.getString("loginID"));

                    MainActivity.checker = 0;
                    sharedPreference.setInt(Settings.this, "checker", MainActivity.checker);

                    Intent ii = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(ii);
                    Settings.this.finish();
                    //this finish() method is used to tell android os that we are done with current
                    // activity now! Moving to other activity
                    return json.getString(TAG_MESSAGE);
                }else{
                    return json.getString(TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        //Once the background process is done we need to  Dismiss the progress dialog asap
        protected void onPostExecute(String message) {
            pDialog.dismiss();
            if (message != null){
                Toast.makeText(Settings.this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    //For the Submit button if there is an error
    public void error(){
        mSettingsBtn.setTextColor(Color.parseColor("#CC0000"));
    }

    public void addListenerOnButton() {

        final Context context = this;

        mSettingsBtn = (Button) findViewById(R.id.settingsBtn);
        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (ip.getText().equals(null) || ip.getText().toString().trim().equals("")) {
                    Toast.makeText(Settings.this, "Server Field is empty", Toast.LENGTH_SHORT).show();
                    error();
                }else {
                    sharedPreference.setString(Settings.this, "username", user.getText().toString());
                    sharedPreference.setString(Settings.this, "password", pass.getText().toString());
                    sharedPreference.setString(Settings.this, "server", ip.getText().toString());

                    LOGIN_URL = "http://" + ip.getText().toString() + "/chd/public/app/login.php";
                    if(connectionCheck()) {
                        new AttemptLogin().execute();
                        success = sharedPreference.getInt(Settings.this, "success");
                        if (success == 1) {
                            mSettingsBtn.setBackgroundResource(R.drawable.on_btn_click);
                        }else if (success == 0) {
                            error();
                        }

                        sharedPreference.setInt(Settings.this, "log", 100);
                    }
                    else{
                        new AlertDialog.Builder(context)
                                .setIcon(R.drawable.nowifi)
                                .setTitle("No internet connection")
                                .setMessage("Check your Internet Connection")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //code for exit
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        System.exit(1);
                                        Settings.this.finish();
                                    }

                                })
                                .show();
                        error();
                    }
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Context context = this;

        if (id == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Closes the keyboard of you tap anywhere else on the screen
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }

}
