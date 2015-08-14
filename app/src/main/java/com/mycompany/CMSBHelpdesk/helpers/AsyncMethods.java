package com.mycompany.CMSBHelpdesk.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.mycompany.CMSBHelpdesk.AddCase;
import com.mycompany.CMSBHelpdesk.AddPicture;
import com.mycompany.CMSBHelpdesk.MainActivity;
import com.mycompany.CMSBHelpdesk.R;
import com.mycompany.CMSBHelpdesk.userList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Abel on 08/07/2015.
 * The three types used by an asynchronous task are the following:
 *  Params, the type of the parameters sent to the task upon execution.
 *  Progress, the type of the progress units published during the background computation.
 *  Result, the type of the result of the background computation.
 *
 * extends AsyncTask<Params, Progress, Result>
 *
 */
public class AsyncMethods {
    private static final String DELETE_IMAGE = "http://"+ MainActivity.TAG_IP +"/public/app/deleteImage.php";

    /*----------------------------LOAD IMAGE FROM URL------------------------------------*/
    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        ProgressDialog pDialog;
        public DownloadImageTask(ImageView bmImage, ProgressDialog pDialog)
        {
            this.bmImage = bmImage;
            this.pDialog = pDialog;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap image = null, imageOld = null;
            int inSampleSize = 2;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                //make the image smaller to make sure it doesnt run out of memory:
                if(bmImage != null) {
                    options.inSampleSize = 7;
                    image = BitmapFactory.decodeStream(in, null, options);
                    return image;
                }else{
                    options.inJustDecodeBounds = true;
                    imageOld = BitmapFactory.decodeStream(in, null, AddPicture.options);
                    in.close();

                    int height = options.outHeight;
                    int width = options.outWidth;

                    in = new java.net.URL(urldisplay).openStream();
                    options = new BitmapFactory.Options();

                    if(imageOld != null)
                        imageOld.recycle();

                    if(height > 100 || width > 300){
                        final int halfHeight = height / 2;
                        final int halfWidth = width / 2;

                        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                        // height and width larger than the requested height and width.
                        while ((halfHeight / inSampleSize) > 100
                                && (halfWidth / inSampleSize) > 300) {
                            inSampleSize *= 2;
                        }
                    }
                    options.inSampleSize = inSampleSize;
                    options.inJustDecodeBounds = false;
                    //options.inPreferredConfig = Bitmap.Config.RGB_565;
                    image = BitmapFactory.decodeStream(in, null, options);
                    return image;
                }

            } catch (Exception e) {
                Log.e("Error", e.toString());
                e.printStackTrace();
            }
            return image;
        }

        protected void onPostExecute(Bitmap result) {
            if(pDialog != null) {
                AddCase.pDialog.dismiss();
            }
            if(bmImage != null) {
                bmImage.setImageBitmap(result);
            }
        }
    }


    /*--------------------------DELETE IMAGE--------------------------*/
    public static class deleteImage extends AsyncTask<String, Void, Void>{
        String filename, id;

        @Override
        protected Void doInBackground(String... strings) {
            filename = strings[0];
            id = strings[1];
            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("delete_file", filename));
            parameters.add(new BasicNameValuePair("id", id));
            new JSONParser().makeHttpRequest(DELETE_IMAGE, "POST", parameters);

            return null;
        }
    }

    //to dynamically resize image
    public static class getSampleSize extends AsyncTask<String, Bitmap, Bitmap>{
        String filePath;
        BitmapFactory.Options options;
        ProgressDialog pDialog;

        public getSampleSize(String filePath, BitmapFactory.Options options, ProgressDialog pDialog){
            this.filePath = filePath;
            this.options = options;
            this.pDialog = pDialog;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            return getSize(filePath, options);
        }

        //to dynamically resize image
        public static Bitmap getSize(String filePath, BitmapFactory.Options options){
            Bitmap image = null, imageOld = null;
            int inSampleSize = 2;
            int height, width;
            options.inJustDecodeBounds = true;

            imageOld = BitmapFactory.decodeFile(filePath, options);

            height = options.outHeight;
            width = options.outWidth;

            options = new BitmapFactory.Options();
            if(imageOld != null)
                imageOld.recycle();
            if (height > 100 || width > 300) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > 100
                        && (halfWidth / inSampleSize) > 300) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;

            image = BitmapFactory.decodeFile(filePath, options);
            return image;
        }

        @Override
        public void onPostExecute(Bitmap result){
            if(pDialog != null){
                pDialog.dismiss();
            }
        }
    }

    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //gets Users from MySQL DB
    public static class getUsers extends AsyncTask<String, String, String> {
        Context c;
        JSONParser jsonP;
        ProgressDialog pDialog;
        DBController dbc;
        public getUsers(Context c, JSONParser jsonP, ProgressDialog pDialog, DBController dbc){
            this.c = c;
            this.jsonP = jsonP;
            this.pDialog = pDialog;
            this.dbc = dbc;
        }

        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(pDialog != null) {
                pDialog = new ProgressDialog(c, R.style.MyTheme);
                pDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
                pDialog.setCancelable(false);
                pDialog.show();
            }
            //MainActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{
                JSONParser jsonParser1;
                //get JSON string from URL
                JSONObject jsonUse = jsonP.makeHttpRequest(MainActivity.USER_URL, "GET", parameters);
                if(internetCheck.isNetworkConnected(c)) {

                    //check log cat for JSON response
                    Log.d("Users: ", jsonUse.toString());
                    //Check for SUCCESS TAG
                    success = jsonUse.getInt(MainActivity.TAG_SUCCESS);
                }
                if(success == 1) {
                    //users found, get array of users
                    userList.users = jsonUse.getJSONArray(MainActivity.TAG_USERS);
                    userList.companies = jsonUse.getJSONArray(MainActivity.TAG_DIVISIONS);

                    //Loop through all the users
                    for(int i =0; i < userList.users.length(); i++){
                        JSONObject u = userList.users.getJSONObject(i);

                        String id = u.getString(MainActivity.TAG_USERID);
                        String name = u.getString(MainActivity.TAG_NAME);
                        String company = u.getString(MainActivity.TAG_COMPANY);
                        String email = u.getString(MainActivity.TAG_EMAIL);
                        String telephone = u.getString(MainActivity.TAG_TELEPHONE);
                        String division_id = u.getString(MainActivity.TAG_DIVISION_ID);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(MainActivity.TAG_USERID, id);
                        maps.put(MainActivity.TAG_NAME, name);
                        maps.put(MainActivity.TAG_COMPANY, company);
                        maps.put(MainActivity.TAG_EMAIL, email);
                        maps.put(MainActivity.TAG_TELEPHONE, telephone);
                        maps.put(MainActivity.TAG_DIVISION_ID, division_id);

                        //add this map to SQLite too
                        dbc.insertUser(maps);
                    }

                    //Loop through all the companies
                    for(int j =0; j < userList.companies.length(); j++){
                        JSONObject u = userList.companies.getJSONObject(j);

                        String id = u.getString(MainActivity.TAG_COMPANY_ID);
                        String name = u.getString(MainActivity.TAG_COMPANY_NAME);
                        String enabled = u.getString(MainActivity.TAG_ENABLED);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(MainActivity.TAG_COMPANY_ID, id);
                        maps.put(MainActivity.TAG_COMPANY_NAME, name);
                        maps.put(MainActivity.TAG_ENABLED, enabled);

                        //add this map to Company SQLite too
                        if(enabled.equals("1")) {
                            dbc.insertCompany(maps);
                        }
                    }

                    return jsonUse.getString(MainActivity.TAG_MESSAGE);
                }
                else {
                    return jsonUse.getString(MainActivity.TAG_MESSAGE);
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            // dismiss the dialog after getting all users
            if(pDialog != null)
                pDialog.dismiss();
        }
    }


}
