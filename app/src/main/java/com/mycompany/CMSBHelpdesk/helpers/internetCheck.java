package com.mycompany.CMSBHelpdesk.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.mycompany.CMSBHelpdesk.R;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by Abel on 22/06/2015.
 * checks connectivity by pinging to google.com
 */
public class internetCheck {
    public static final String LOG_TAG = "Requesting";

    public static boolean connectionCheck(Context c){
        //check Internet connection
        if(isNetworkConnected(c)) {
            try {
                if (new checkConnection().execute(c).get()) {
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
            new AlertDialog.Builder(c)
                    .setIcon(R.drawable.nowifi)
                    .setTitle("No internet connection")
                    .setMessage("Please check your internet connection")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //code for exit
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }

                    })
                    .show();
            return false;
        }
        return false;
    }

    //To check if internet is actually connected
    //Have to run it in AsyncTask or else you'll get a NetworkOnMain exception error
    static class checkConnection extends AsyncTask<Context, Void, Boolean> {
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
                //TODO: change the URL it checks
                HttpURLConnection urlc = (HttpURLConnection) (new URL("https://www.google.com").openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(2500);
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
}
