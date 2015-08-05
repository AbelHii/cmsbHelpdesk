package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.AsyncMethods;
import com.mycompany.CMSBHelpdesk.helpers.internetCheck;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;


public class AddPicture extends AddCase {

    private ImageButton mTakePic, mChoosePic;
    public ImageView mImageBitmap;
    public ImageView mDefaultBitmap;
    public static WebView mFullImage;
    public static ImageView mFullImageView;
    private ListView mImageLV;
    public static String filePath = null, filePathTemp = null, filePaths, filePathsAdd;
    String caseId, call;
    public static Bitmap photo;
    public static BitmapFactory.Options options = new BitmapFactory.Options();

    imageAdapter ia;
    ArrayList<Bitmap> image_list;
    public static ArrayList<String> listOfImages = new ArrayList<>(), imageList = new ArrayList<>(), tempList = new ArrayList<>();
    public static JSONObject json = new JSONObject(), jsonAdd = new JSONObject();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Attach Image");
        //set actionbar colour
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.colorAB)));
        setContentView(R.layout.add_pic);

        initialise();
        retrieve();

        //Retrieve and display the images from the server
        if(imgExists.equalsIgnoreCase("true") && internetCheck.isNetworkConnected(this)){
            Toast.makeText(AddPicture.this, "ONE", Toast.LENGTH_LONG).show();
            //add the images from the server to the list of images:
            listOfImages.addAll(imageList);
            //clear image list to prevent duplicates:
            imageList.clear();

            if(listOfImages.size()!=0){
                for (int i = 0; i < listOfImages.size(); ++i) {
                    String path = listOfImages.get(i);
                    options.inSampleSize = 5;
                    //if image path is a URL then download from server:
                    if(path.contains("http")) {
                        try {
                            photo = new AsyncMethods.DownloadImageTask(null, pDialog).execute(path).get();
                            if (photo != null) {
                                addImage(getOrientation(photo, path));
                            }else{
                                photo = BitmapFactory.decodeResource(this.getResources(), R.drawable.cms_logo, options);
                                addImage(photo);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }else if(!path.contains("http")){ //if its from SQLite then load this way:
                        try {
                            photo = new AsyncMethods.getSampleSize(path, options, pDialog).execute().get();//BitmapFactory.decodeFile(filePath, options);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        if(photo != null) {
                            addImage(getOrientation(photo, filePath));
                        }else{
                            photo = BitmapFactory.decodeResource(this.getResources(), R.drawable.cms_logo, options);
                            addImage(photo);
                        }
                    }
                }
            }
        }else if(listOfImages.size() != 0){
            Toast.makeText(AddPicture.this, "TWO", Toast.LENGTH_LONG).show();
            //this is for when the app is offline or for the temporary list (user haven't clicked submit yet)
            //Gets the local images from sq lite paths:
            options.inSampleSize = 2;
            for (int i = 0; i < listOfImages.size(); i++) {
                filePath=listOfImages.get(i);
                try {
                    photo = new AsyncMethods.getSampleSize(filePath, options, pDialog).execute().get();//BitmapFactory.decodeFile(filePath, options);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if(photo != null) {
                    addImage(getOrientation(photo, filePath));
                }
            }
            Toast.makeText(getApplicationContext(), listOfImages.size()+" image(s) from sq lite", Toast.LENGTH_SHORT).show();
        }else{//If both don't exist
            Toast.makeText(AddPicture.this, "THREE", Toast.LENGTH_LONG).show();
            if(pDialog != null)
                pDialog.dismiss();
            listOfImages = new ArrayList<>();
            mDefaultBitmap.setImageResource(R.drawable.cms_logo);
            mDefaultBitmap.setAlpha(127);
        }

        onImageClick();
    }



    public void retrieve(){
        call = getIntent().getStringExtra("call");

        Intent intent = getIntent();
        caseId = intent.getStringExtra("caseIdKey");
        imgExists = intent.getStringExtra("imgExists");

        String oPaths = intent.getStringExtra("filez");
        if (filePathTemp != null) {
            getFilePaths(filePathTemp, "imageArrayAdd");
            if(oPaths != null && !oPaths.equals("")){getFilePaths(oPaths, "imageArray");}
        }
        else if(oPaths != null){
            getFilePaths(oPaths, "imageArray");
        }
        else{
            mDefaultBitmap.setImageResource(R.drawable.cms_logo);
            mDefaultBitmap.setAlpha(127);
        }
    }

    //this reads the json object and stores the file paths in the listOfImages ArrayList.
    public static ArrayList getFilePaths(String paths, String jsonArr){
        //Empty list of images to avoid duplicates
        if(!listOfImages.contains("http"))
            listOfImages.clear();
        JSONArray arr;
        ArrayList files = new ArrayList();
        try {
            JSONObject jj = new JSONObject(paths);
            arr = jj.optJSONArray(jsonArr);
            if(arr != null){
                for(int i = 0; i<arr.length(); i++){
                    files.add(arr.get(i).toString());
                }
            }
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        listOfImages = files;
        return files;
    }

    public void initialise(){
        mTakePic = (ImageButton) findViewById(R.id.takePic);
        mChoosePic = (ImageButton) findViewById(R.id.choosePic);

        mDefaultBitmap = (ImageView) findViewById(R.id.bitmapImage);
        image_list = new ArrayList<>();
        listOfImages = new ArrayList<>();
        getListView();
    }

    public void onImageClick(){
        mTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(image_list.size() < 5) {
                    takePictureIntent();
                }else{
                    Toast.makeText(getApplicationContext(), "Max number of images reached", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mChoosePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(image_list.size() < 5) {
                    choosePicture();
                }else{
                    Toast.makeText(getApplicationContext(), "Max number of images reached", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mImageLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LayoutInflater layoutInflater
                        = (LayoutInflater) getBaseContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = layoutInflater.inflate(R.layout.fullscreen_image, null);
                final PopupWindow popupWindow = new PopupWindow(
                        popupView, AbsoluteLayout.LayoutParams.FILL_PARENT, AbsoluteLayout.LayoutParams.FILL_PARENT, true);


                mFullImage = (WebView) popupView.findViewById(R.id.fullImage);
                mFullImageView = (ImageView) popupView.findViewById(R.id.fullImageView);
                //This is to allow zoom by loading in a webview:
                mFullImage.setInitialScale(1);
                mFullImage.getSettings().setLoadWithOverviewMode(true);
                mFullImage.getSettings().setUseWideViewPort(true);
                mFullImage.getSettings().setDisplayZoomControls(false);
                mFullImage.getSettings().setBuiltInZoomControls(true);

                String imagePath = listOfImages.get(i);
                String html = "<html><head></head><body><img src=\""+ imagePath + "\"></body></html>";
                if(!imagePath.contains("http")){
                    //to load local images in imageview:
                    mFullImageView.setImageBitmap(image_list.get(i));
                }else {
                    mFullImage.loadDataWithBaseURL("", html, "text/html", "utf-8", "");
                }
                //to make background transparent:
                mFullImage.setBackgroundColor(0);

                final int position = i;
                mDelete = (Button) popupView.findViewById(R.id.delete);
                mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(AddPicture.this)
                                .setTitle("Are you sure you want to delete this picture?")
                                .setCancelable(true)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteImage(position);
                                        //code for exit
                                        popupWindow.dismiss();
                                    }

                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //code for exit dialog
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                });

                mCancel = (Button) popupView.findViewById(R.id.cancelPopup);
                mCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                    }
                });
                popupWindow.setAnimationStyle(R.style.popupAnimation);
                popupWindow.setBackgroundDrawable(new BitmapDrawable());
                popupWindow.setOutsideTouchable(true);
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
            }
        });
    }

    //To delete an image:
    public void deleteImage(int position){
        String filename = listOfImages.get(position).toString();
        String delImg [] = new String[2];
        delImg[0] = filename;
        delImg[1] = caseId;
        new AsyncMethods.deleteImage().execute(delImg);

        image_list.remove(position);
        listOfImages.remove(position);
        ia = new imageAdapter(this, image_list);
        setListAdapter(ia);
        ia.notifyDataSetChanged();
    }

    /*--------------------------IMAGE ADAPTER-------------------------------------------------------*/
    public ListView getListView() {
        if(mImageLV == null){
            mImageLV = (ListView)findViewById(android.R.id.list);
            return mImageLV;
        }
        return mImageLV;
    }
    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }

    private class imageAdapter extends ArrayAdapter<Bitmap>{
        private Context context;
        private ArrayList<Bitmap> items;
        public imageAdapter(Context context, ArrayList<Bitmap> data) {
            super (AddPicture.this, R.layout.listview_image, data);
            this.context = context;
            this.items = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = null;
            convertView = null;
            if(convertView == null) {
                LayoutInflater vi = LayoutInflater.from(getContext());
                view = vi.inflate(R.layout.listview_image, parent, false);

                Bitmap b = items.get(position);
                if(b!=null) {
                    mImageBitmap = (ImageView) view.findViewById(R.id.listImage);

                    mImageBitmap.setAlpha(250);
                    mImageBitmap.setImageBitmap(b);
                }
            }

            return view;
        }
    }

    public void addImage(Bitmap b){
        image_list.add(b);
        ia = new imageAdapter(this, image_list);
        setListAdapter(ia);
        ia.notifyDataSetChanged();
        //hide defaultBitmap
        mDefaultBitmap.setVisibility(View.INVISIBLE);
    }



/*------------------------------------USING THE CAMERA AND GALLERY--------------------------------------------------------------------------*/
    //----------This is how you retrieve the images after taking or choosing a picture:----------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case (REQUEST_IMAGE_CAPTURE): {
                if (resultCode == RESULT_OK) {
                    galleryAddPic();

                    options.inSampleSize = 3;
                    Bitmap mBitmap = null;
                    try {
                        mBitmap = new AsyncMethods.getSampleSize(filePath, options, null).execute().get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    mBitmap = getOrientation(mBitmap, filePath);

                    addImage(mBitmap);
                    //add the filePaths to an ArrayList:
                    listOfImages.add(filePath);
                    break;
                }else{
                    break;
                }
            }
            case (REQUEST_IMAGE):{
                if (resultCode == RESULT_OK) {
                    InputStream is = null;
                    Uri selectedImage = data.getData();
                    //Do this to get the file path
                    //cursor selects only the file path because images store stuff like geo tag and stuff
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    filePath = cursor.getString(columnIndex);
                    cursor.close();
                    //Reduces the size of the image to be contained in the bitmap
                    options.inSampleSize = 4;
                    //sets the image to the bitmap

                    try {
                        is = this.getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), e+" There was an error loading the image please " +
                                "press back and try again", Toast.LENGTH_LONG).show();
                    }
                    photo = BitmapFactory.decodeStream(is, null, options);
                    photo = getOrientation(photo, filePath);
                    addImage(photo);

                    //add the filePaths to an ArrayList:
                    listOfImages.add(filePath);
                    break;
                }
            }
        }
    }

    //Method for taking a picture with the camera:
    private void takePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, ex + " error occurred while creating the file", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //to access the camera
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }else{
            Toast.makeText(this, "Camera not detected", Toast.LENGTH_SHORT).show();
        }
    }

    //method for choosing an existing picture from your gallery
    private void choosePicture(){
        //Intent.ACTION_GET_CONTENT
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);

        // Chooser of filesystem options.
        //final Intent chooserIntent = Intent.createChooser(intent, "Select Source");
        startActivityForResult(intent, REQUEST_IMAGE);
        //startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE);
        /*Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE);*/
    }

    //This creates a new file for the image to go in
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("dd-MM-yy_HH-mm-ss").format(new Date());
        String imageFileName = "CHD_" + timeStamp + "_";
        final File newDir = new File(Environment.getExternalStorageDirectory() + "/CHDImages/");
        if(newDir.mkdir()){
           Toast.makeText(this, "created new directory /CHDImages/", Toast.LENGTH_SHORT).show();
        }
        File storageDir = newDir;
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        filePath = image.getAbsolutePath();
        return image;
    }
    //to save the image to the default photo gallery/media providers database (so other apps and androids gallery can view it)
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(filePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    /*---------------TO GET CORRECT ORIENTATION OF IMAGE---------------------------------*/
    public static Bitmap getOrientation(Bitmap b, String fileP){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(fileP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotated = rotateBitmap(b, orientation);

        return rotated;
    }
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_add_picture, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            exit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        exit();
    }

    //to leave this activity:
    private void exit(){
        //putting the array of files as a JSONObject to set is a single string to store in SQLite
        try {//to remove the duplicate urls:
            json.put("imageArray", new JSONArray(listOfImages));
            filePaths = json.toString();

            //tempList is for seperating the http URL paths from the local paths
            //because you don't can't upload the URL paths to the server
            tempList.clear();
            tempList = listOfImages;
            for(int i = tempList.size()-1; i >= 0; i--){
                if(tempList.get(i).contains("http"))
                    tempList.remove(i);
            }
            jsonAdd.put("imageArrayAdd", new JSONArray(tempList));
            filePathsAdd = jsonAdd.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //set the hook to say image was added
        if(tempList.size() > 0)
            hook = true;

        //Returning the values to AddCase
        Intent returnIntent = new Intent();
        String [] arr = listOfImages.toArray(new String[listOfImages.size()]);
        returnIntent.putStringArrayListExtra("files", listOfImages);
        returnIntent.putExtra("filePaths", filePaths);
        setResult(Activity.RESULT_OK, returnIntent);


        //Photo.recycle() is just to make sure the app doesn't go out of memory
        if(photo != null)
            photo.recycle();
        photo=null;
        this.finish();
        overridePendingTransition(R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom);
    }

}

