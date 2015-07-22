package com.mycompany.CMSBHelpdesk.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.mycompany.CMSBHelpdesk.AddCase;
import com.mycompany.CMSBHelpdesk.AddPicture;
import com.mycompany.CMSBHelpdesk.MainActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Abel on 08/07/2015.
 */
public class AsyncMethods {
    private static final String GET_MAX_ID = "http://"+ MainActivity.TAG_IP +"/chd/public/app/getMaxId.php";
    private static final String DELETE_IMAGE = "http://"+ MainActivity.TAG_IP +"/chd/public/app/deleteImage.php";


    /*-----------------------GET THE MAX CASE ID TO AVOID CONFLICTS--------------------------------*/
    public static class getMaxId extends AsyncTask<String, Void, String>{
        Context context;
        public getMaxId(Context context){
            this.context = context;
        }

        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            JSONObject json = new JSONParser().makeHttpRequest(GET_MAX_ID, "POST", parameters);

            try {
                success = json.getInt("success");
                if(success == 1) {
                    int max = json.getInt("max_id");
                    return String.valueOf(max);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            if(result != null) {
                AddCase caseAdd = (AddCase) context;
                caseAdd.case_id = result;
                caseAdd.mIDCase.setText(String.valueOf(Integer.parseInt(caseAdd.case_id)+1));
                super.onPostExecute(result);
            }
        }
    }


    /*----------------------------LOAD IMAGE FROM URL------------------------------------*/
    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage)
        {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap image = null;
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
                    BitmapFactory.decodeStream(in, null, AddPicture.options);
                    in.close();
                    in = null;

                    int height = options.outHeight;
                    int width = options.outWidth;

                    in = new java.net.URL(urldisplay).openStream();
                    options = new BitmapFactory.Options();

                    if(height > 200 || width > 400){
                        final int halfHeight = height / 2;
                        final int halfWidth = width / 2;

                        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                        // height and width larger than the requested height and width.
                        while ((halfHeight / inSampleSize) > 200
                                && (halfWidth / inSampleSize) > 400) {
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

        public getSampleSize(String filePath, BitmapFactory.Options options){
            this.filePath = filePath;
            this.options = options;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            return getSize(filePath, options);
        }

        //to dynamically resize image
        public static Bitmap getSize(String filePath, BitmapFactory.Options options){
            Bitmap image = null;
            int inSampleSize = 2;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            int height = options.outHeight;
            int width = options.outWidth;

            options = new BitmapFactory.Options();

            if(height > 200 || width > 400){
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > 200
                        && (halfWidth / inSampleSize) > 400) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;

            image = BitmapFactory.decodeFile(filePath, options);
            return image;
        }
    }

}
