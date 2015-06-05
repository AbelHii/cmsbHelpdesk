package com.mycompany.CMSBHelpdesk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

public class LoginActivity extends ActionBarActivity {

    protected Button mLoginBtn;
    protected Button mCreateAccountBtn;

    //Login class "AttemptLogin" and "JSONParser" is from mrbool.com
    private EditText user, pass;

    // Progress Dialog
    private ProgressDialog pDialog;
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    private static final String LOGIN_URL = "http://10.1.2.52/chd/public/abel/login.php";//"http://abelhii.comli.com/login.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    public static final String LOG_TAG = "Requesting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //initialise
        user = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.password);
        mLoginBtn = (Button)findViewById(R.id.loginBtn);
        mCreateAccountBtn = (Button) findViewById(R.id.createAccountLogin);

        //check Internet connection
        try {
            if(new checkConnection().execute(this).get()) {
                addListenerOnButton();
            }
            else{
                new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.nowifi)
                        .setTitle("No internet connection")
                        .setMessage("Please turn on mobile data or wifi")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //code for exit
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                                LoginActivity.this.finish();
                            }

                        })
                        .show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    //to disable the back button so they cant go into MainActivity
    @Override
    public void onBackPressed(){}

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
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Attempting to login...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // TODO Auto-generated method stub
            // here Check for success tag
            int success;
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
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    Log.d("Successful Login", json.toString());
                    //sending shared preference to keep user logged in even if they close the app
                    sharedPreference.setString(LoginActivity.this, "login", user.getText().toString());
                    sharedPreference.setString(LoginActivity.this, "pass", pass.getText().toString());

                    MainActivity.checker = 0;
                    sharedPreference.setInt(LoginActivity.this, "checker", MainActivity.checker);

                    Intent ii = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(ii);
                    LoginActivity.this.finish();
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
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }
    }


    public void addListenerOnButton() {

        final Context context = this;

        mCreateAccountBtn = (Button) findViewById(R.id.createAccountLogin);
        mCreateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreference.setString(context ,"login" ,user.getText().toString());
                sharedPreference.setString(context ,"pass" ,pass.getText().toString());
                MainActivity.checker = 0;
                sharedPreference.setInt(LoginActivity.this, "checker", MainActivity.checker);
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });

        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mLoginBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0) {
                new AttemptLogin().execute();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
