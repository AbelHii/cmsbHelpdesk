package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Scroller;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.AsyncMethods;
import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.internetCheck;
import com.mycompany.CMSBHelpdesk.helpers.sharedPreference;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class AddCase extends ActionBarActivity {

    public Button mUser, mCancel, mDelete;
    public ImageButton mInfo;
    public ImageView mBitmapImage;
    private Spinner mStatus;
    public static String case_id = "0";
    private String name, username = "", description, actionT, assigneeID, statusID, sync = "", id_user, path;
    private static String caseID;
    public static String imageFilePath;
    public Button mSubmit;
    public TextView mIDCase;
    private TextView mAssigneeLabel;
    private TextView mContactName;
    private TextView mCompany;
    private TextView mEmail;
    private TextView mTel;
    private TextView mActionTaken;
    private TextView mDesc;
    private TextView mActionTakenLabel;
    private TextView mDescLabel;
    private TextView mStatusDivider;
    public TextView mFilePath;
    private SharedPreferences sp;
    private SharedPreferences.Editor e;

    //To check whether its in addcase or editcase
    String caller;
    static String title = "Add Case";
    String fps;

    // DB Class to perform DB related operations
    DBController userControl = new DBController(this);

    //Stores the values for AddCase and EditCase
    public static ArrayList<String> spinnerUsernames;
    public static ArrayList<String> companyV, temporaryList;

    //DB stuff:
    // JSON parser class
    static JSONParser jsonParser = new JSONParser();


    private static ProgressDialog pDialog;
    public static String ADD_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/AddCase.php";
    public static String UPDATE_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/UpdateCase.php";
    public static String INSERT_IMAGE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/insertImage.php";
    public static String CHECK_IMAGE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/checkImages.php";
    public static String imgExists;
    /*---------------------------------------------ON CREATE-----------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set actionbar colour
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.colorAB)));

        //Default Back Button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_add_case);
        setTitle(title);

        //initialises the variables
        initialise();
        //Check to see if has to retrieve data or not (goes to Add Case or Edit Case)
        caller = getIntent().getStringExtra("caller");
        if(caller != null) {
            switch (caller.toLowerCase()) {
                case "editcase":
                    title = "Edit Case";
                    setTitle(title);
                    retrieve();
                    checkLabel();
                    break;
                case "addcase":
                    title = "Add Case";
                    setTitle(title);
                    //getting the most recent case case_id and assignee case_id
                    if(internetCheck.isNetworkConnected(this)) {
                        new AsyncMethods.getMaxId(this).execute();
                        sharedPreference.setString(AddCase.this, MainActivity.TAG_ID, case_id);
                    }else{
                        case_id = userControl.getMaxId("cases");
                        if(sharedPreference.getString(AddCase.this, MainActivity.TAG_ID) == null)
                            sharedPreference.setString(AddCase.this, MainActivity.TAG_ID, case_id);
                    }
                    mIDCase.setText(String.valueOf(Integer.parseInt(case_id) + 1));
                    checkLabel();
                    break;
            }
        }

        //This just listens for when a button is clicked.
        addListenerOnButton();
    }

    public void checkLabel(){
        if (mDesc.getText().toString().trim().equals("")) {
            mDescLabel.setVisibility(View.GONE);
            mStatusDivider.setVisibility(View.VISIBLE);
        } else {
            mDescLabel.setVisibility(View.VISIBLE);
            mDesc.setLines(2);
            mStatusDivider.setVisibility(View.GONE);
        }
        if (mActionTaken.getText().toString().trim().equals("")){
            mActionTakenLabel.setVisibility(View.GONE);
        }
        else{
            mActionTakenLabel.setVisibility(View.VISIBLE);
            mActionTaken.setLines(2);
        }
    }

    //----------------------------------------INITIALISE-------------------------------------------------------------------
    private void initialise(){
        MainActivity.checkLog = sharedPreference.getString(this, "login");
        MainActivity.TAG_IP = sharedPreference.getString(this, "ip");
        ADD_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/AddCase.php";
        UPDATE_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/UpdateCase.php";

        //SharedPreference initialisation to save things:
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        e = sp.edit();

        mUser = (Button) findViewById(R.id.spinnerNames);
        mInfo = (ImageButton) findViewById(R.id.moreInfo);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);
        ArrayList<String> status = new ArrayList<>();
        String[] s = this.getResources().getStringArray(R.array.status);
        for(int i = 0; i < s.length; i++)
            status.add(s[i]);
        statusAdapter adapter = new statusAdapter(AddCase.this, R.layout.spinner_item, status);
        mStatus.setAdapter(adapter);

        mIDCase = (TextView) findViewById(R.id.IDCase);
        mContactName = (TextView) findViewById(R.id.contactName);
        mCompany = (TextView)findViewById(R.id.company);
        mEmail = (TextView) findViewById(R.id.email);
        mTel = (TextView)findViewById(R.id.tel);

        mDesc = (TextView)findViewById (R.id.caseDesc);
        mActionTaken = (TextView) findViewById (R.id.actionTaken);
        mStatusDivider = (TextView) findViewById(R.id.statusDivider);
        mDescLabel = (TextView) findViewById(R.id.caseDescLabel);
        mActionTakenLabel = (TextView) findViewById (R.id.actionTakenLabel);

        case_id = sharedPreference.getString(AddCase.this, MainActivity.TAG_ID);
        assigneeID = sharedPreference.getString(AddCase.this, MainActivity.TAG_LOGIN_ID);

        imgExists = sharedPreference.getString(this, "imgExists");
        mFilePath = (TextView) findViewById(R.id.filePath);
    }

    public void removeSharedPreferences(){
        e.remove("filePaths").commit();
        e.remove("imgExists").commit();
    }
    public void clearOnExit(){
        removeSharedPreferences();
        //remove the image from json and array;
        AddPicture.json.remove("imageArray");
        AddPicture.json.remove("imageArrayAdd");
        AddPicture.listOfImages = new ArrayList<>();
    }

    //------------------------------------ADAPTER FOR STATUS SPINNER-----------------------------------------------------------
    public void chooseStatColour(String s, TextView v){
        switch(s){
            case "Not Started":
                v.setTextColor(Color.parseColor("#cc0000"));
                break;
            case "In Progress":
                v.setTextColor(Color.parseColor("#12af83"));
                break;
            case "Waiting for Vendor":
                v.setTextColor(Color.parseColor("#ddbb00"));
                break;
            case "Differed":
                v.setTextColor(Color.parseColor("#af1283"));
                break;
            case "Pending Close":
                v.setTextColor(Color.parseColor("#1abef9"));
                break;
        }
    }
    private class statusAdapter extends ArrayAdapter<String> {
        private Context context;
        private ArrayList<String> itemList;
        public statusAdapter(Context context, int textViewResourceId, ArrayList<String> itemList) {
            super(context, textViewResourceId, itemList);
            this.context = context;
            this.itemList = itemList;
        }
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);

            if (v == null) {
                v = new TextView(context);
            }
            TextView mSpin = (TextView) v.findViewById(R.id.spinner_item);
            chooseStatColour(itemList.get(position), mSpin);
            if(position % 2 == 0){
                v.setBackgroundResource(R.drawable.mycolors);
            }else {
                v.setBackgroundResource(R.drawable.mycolors2);
            }
            v.setText(itemList.get(position));

            return v;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if(view == null){
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                view = vi.inflate(R.layout.spinner_item, null);
            }

            TextView mSpin = (TextView) view.findViewById(R.id.spinner_item);
            chooseStatColour(itemList.get(position), mSpin);

            mSpin.setText(itemList.get(position));
            return view;
        }
    }



    /*-------------------------------------ADD LISTENER TO BUTTON -------------------------------------------------------------------*/
    public void addListenerOnButton() {
        final Context context = this;

        mUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddCase.this, userList.class);
                intent.putExtra("user", mUser.getText().toString());
                startActivityForResult(intent, 3);
            }
        });
        mUser.setBackgroundResource(R.drawable.on_btn_click2);
        mDesc.setBackgroundResource(R.drawable.on_btn_click3);
        mActionTaken.setBackgroundResource(R.drawable.on_btn_click3);

        mStatus.setBackgroundResource(R.drawable.on_btn_click2);
        mDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddCase.this, TextEditor.class);
                intent.putExtra("text", mDesc.getText().toString());
                intent.putExtra("mType", "desc");
                startActivityForResult(intent, 1);
            }
        });
        mActionTaken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddCase.this, TextEditor.class);
                intent.putExtra("text", mActionTaken.getText().toString());
                intent.putExtra("mType", "actionT");
                startActivityForResult(intent, 2);
            }
        });
        mDesc.setScroller(new Scroller(this));
        mDesc.setVerticalScrollBarEnabled(true);
        mDesc.setMovementMethod(new ScrollingMovementMethod());

        mActionTaken.setScroller(new Scroller(this));
        mActionTaken.setVerticalScrollBarEnabled(true);
        mActionTaken.setMovementMethod(new ScrollingMovementMethod());

        //popup
        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (mUser.getText().toString().equals("") || mUser.getText().toString().equals(null)) {
                Toast.makeText(AddCase.this, "Name is Empty", Toast.LENGTH_SHORT).show();
            } else {
                LayoutInflater layoutInflater
                        = (LayoutInflater) getBaseContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = layoutInflater.inflate(R.layout.contact_info, null);
                final PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        AbsoluteLayout.LayoutParams.WRAP_CONTENT,
                        AbsoluteLayout.LayoutParams.WRAP_CONTENT);

                setTexts(popupView);

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
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -300);
                //popupWindow.showAsDropDown(mInfo, 50, -100);
            }
            }
        });

        //Submit button
        mSubmit = (Button) findViewById(R.id.submitBtn);
        if(!mUser.getText().toString().trim().equalsIgnoreCase("")){
            mSubmit.setTextAppearance(AddCase.this, R.style.submitButton);
        }
        //Don't allow admin to submit or edit cases:
        if(MainActivity.checkLog.equalsIgnoreCase("admin")){
        }else {
            mSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                for(int i = 0; i < AddPicture.listOfImages.size(); i++){
                    Toast.makeText(getApplicationContext(), AddPicture.listOfImages.get(i).toString(), Toast.LENGTH_LONG).show();
                }
                if (!mUser.getText().toString().trim().equalsIgnoreCase("")) {
                    mSubmit.setTextAppearance(AddCase.this, R.style.submitButton);

                    //Logic for adding case:
                    if (title.trim().equalsIgnoreCase("add Case")) {//ADD
                        if (internetCheck.connectionCheck(context)) {
                            id_user = userControl.getID("users", "userId", mUser.getText().toString(), MainActivity.TAG_NAME, 0);
                            new addCase().execute();
                            if(AddPicture.listOfImages.size() != 0){
                                Toast.makeText(getApplicationContext(), String.valueOf(Integer.parseInt(case_id)+1), Toast.LENGTH_LONG).show();
                                temporaryList = AddPicture.tempList;
                                new uploadImage(caseID).execute(temporaryList);
                            }
                        } else if (!internetCheck.connectionCheck(context)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            addCaseSQLite("10");
                            Toast.makeText(AddCase.this, "Adding Case For Sync ", Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        }
                        //Logic for editing case:
                    } else if (title.trim().equalsIgnoreCase("edit Case")) { //EDIT
                        Intent intent = new Intent(context, MainActivity.class);
                        if (internetCheck.isNetworkConnected(context)) {
                            new updateCase().execute();
                            if(AddPicture.listOfImages.size() != 0){
                                Toast.makeText(getApplicationContext(), case_id, Toast.LENGTH_LONG).show();
                                temporaryList = AddPicture.tempList;
                                new uploadImage(caseID).execute(temporaryList);
                            }
                        } else if (!internetCheck.connectionCheck(context) && sync.equals("10")) {
                            updateCaseSQLite("10");
                            Toast.makeText(AddCase.this, "Updating Case For Sync", Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        } else if (!internetCheck.connectionCheck(context) && (sync.equals("") || sync.equals("20"))) {
                            updateCaseSQLite("20");
                            Toast.makeText(AddCase.this, "Updating Case For Sync", Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        }
                    }
                    AddPicture.listOfImages = new ArrayList<>();
                    removeSharedPreferences();
                } else {
                    mSubmit.setTextColor(Color.parseColor("#CC0000"));
                    Toast.makeText(getApplicationContext(), "Name is Empty", Toast.LENGTH_SHORT).show();
                }
                }
            });
        }
    }

    @Override
    public void onBackPressed(){
        clearOnExit();
        this.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /*----------------------------------SQLITE EDITS-------------------------------*/
    //Adds Case to SQLite:
    public void addCaseSQLite(String num){
        if(!internetCheck.isNetworkConnected(this)){
            sync = num;
        }
        if(!internetCheck.isNetworkConnected(this) && (case_id.equals("0") || case_id.equals("1"))) {
            case_id = userControl.getMaxId("cases");
        }
        String identification = String.valueOf(Integer.parseInt(case_id)+1);
        if(AddPicture.listOfImages.size() == 0){
            AddPicture.filePathsAdd = null; }

        userControl.insertOneCase(identification,
                mStatus.getSelectedItem().toString(),
                mUser.getText().toString(),
                mDesc.getText().toString(),
                mActionTaken.getText().toString(),
                assigneeID,
                String.valueOf(mStatus.getSelectedItemId() + 1),
                sync,
                AddPicture.filePathsAdd);
    }
    //Updates SQLite Cases
    public void updateCaseSQLite(String num){
        if(!internetCheck.isNetworkConnected(this)){
            sync = num;
        }
        if(AddPicture.listOfImages.size() == 0){
            AddPicture.filePathsAdd = null;}

        userControl.updateOneCase(caseID,
                mStatus.getSelectedItem().toString(),
                mUser.getText().toString(),
                mDesc.getText().toString(),
                mActionTaken.getText().toString(),
                assigneeID,
                String.valueOf(mStatus.getSelectedItemPosition() + 1),
                sync,
                AddPicture.filePathsAdd);
    }


 /*-----------------------RETRIEVE VALUES FROM MAINACTIVITY AND SET THEM IN ADDCASE------------------*/
    public void setTexts(View popupView){
        mContactName = (TextView) popupView.findViewById(R.id.contactName);
        mCompany = (TextView) popupView.findViewById(R.id.company);
        mEmail = (TextView) popupView.findViewById(R.id.email);
        mTel = (TextView) popupView.findViewById(R.id.tel);

        username = mUser.getText().toString();
        mContactName.setText(username);
        mCompany.setText(userControl.getUsersData(username).get(0));
        mEmail.setText(userControl.getUsersData(username).get(1));
        mTel.setText(userControl.getUsersData(username).get(2));
    }

    //For Edit Case:
    public void retrieve(){
        Bundle bund = getIntent().getExtras();
        //Retrieving details from when list item is clicked in main activity
        caseID = bund.getString(MainActivity.TAG_ID);

        String assignee = bund.getString(MainActivity.TAG_ASSIGNEE);
        case_id = bund.getString(MainActivity.TAG_ID);
        description = bund.getString(MainActivity.TAG_DESCRIPTION);
        actionT = bund.getString(MainActivity.TAG_ACTION_TAKEN);
        username = bund.getString(MainActivity.TAG_USERNAME);
        statusID = bund.getString(MainActivity.TAG_STATUS_ID);
        assigneeID = bund.getString(MainActivity.TAG_LOGIN_ID);
        sync = bund.getString(MainActivity.TAG_SYNC);
        imageFilePath = bund.getString(MainActivity.TAG_IMAGE);

        //This just sets the static values according to the user
        //SET TEXT
        mUser.setText(username);
        mUser.setTextAppearance(this, R.style.bigFont);
        mDesc.setText(description);
        mActionTaken.setText(actionT);
        mStatus.setSelection(Integer.parseInt(statusID) - 1);
        mIDCase.setText(case_id);

        id_user = userControl.getID("users", "userId", mUser.getText().toString(), MainActivity.TAG_NAME, 0);

        if(MainActivity.checkLog.equalsIgnoreCase("admin")){
            mAssigneeLabel = (TextView) findViewById(R.id.assigneeLabel);
            mAssigneeLabel.setText("Assignee: "+assignee);
        }

        mBitmapImage = (ImageView)findViewById(R.id.bitmapImage);
        mFilePath = (TextView) findViewById(R.id.filePath);

        if(AddPicture.filePathTemp != null) {
            ArrayList<String> files = AddPicture.getFilePaths(AddPicture.filePathTemp, "imageArrayAdd");
            String filepath = files.get(0);
            //Reduces the size of the image to be contained in the bitmap
            AddPicture.options.inSampleSize = 7;
            Bitmap img = BitmapFactory.decodeFile(filepath, AddPicture.options);
            mBitmapImage.setImageBitmap(AddPicture.getOrientation(img, filepath));
            mFilePath.setText(filepath);
        }
        if(internetCheck.isNetworkConnected(this)){
            //To check if images for this case exist in the server:
            checkImages check = new checkImages(this);
            check.execute();
            try {
                AddPicture.imageList = check.get();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void retrieveUserList(Intent data){
        Bundle b = data.getExtras();
        name = b.getString(MainActivity.TAG_NAME);
        //SET TEXT
        mUser.setText(name);
        mUser.setTextAppearance(this, R.style.bigFont);
    }

    //-----------------------------------IMPORTANT CODE!---------------------------------------------------
    //This stuff is just the action bar for like settings and stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_case, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Context context = this;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(context, Settings.class);
            startActivity(intent);
            overridePendingTransition(R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom);
            return true;
        }
        if(id == R.id.add_picture){
            Intent intent = new Intent(AddCase.this, AddPicture.class);
            intent.putExtra("caseIdKey", case_id);
            intent.putExtra("call", caller);
            fps = sharedPreference.getString(AddCase.this, "filePaths");
            intent.putExtra("filez", fps);
            //attempt to save case_id
            if(title.trim().equalsIgnoreCase("addcase")){
                if(internetCheck.isNetworkConnected(context))
                sharedPreference.setString(AddCase.this, MainActivity.TAG_ID, String.valueOf(Integer.parseInt(case_id)+1));
            }else{
                sharedPreference.setString(AddCase.this, MainActivity.TAG_ID, case_id);
            }

            imgExists = sharedPreference.getString(this, "imgExists");
            if(imgExists.equals("true")){
                intent.putExtra("imgExists", imgExists);
            }else{
                intent.putExtra("imgExists", "false");
            }

            startActivity(intent);
            overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_top);
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        if (id == android.R.id.home) {
            clearOnExit();
            this.finish();
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*---------------ON ACTIVITY RESULT--------------------------------------------*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (1) : {
                if (resultCode == Activity.RESULT_OK) {
                    String newText = data.getStringExtra("result");
                    mDesc.setText(newText);
                    checkLabel();
                }
                break;
            }case (2) : {
                if (resultCode == Activity.RESULT_OK) {
                    String newText = data.getStringExtra("result");
                    mActionTaken.setText(newText);
                    checkLabel();
                }
                break;
            }case (3): {
                if (resultCode == Activity.RESULT_OK) {
                    setTitle(title);
                    retrieveUserList(data);
                    mSubmit.setTextAppearance(AddCase.this, R.style.submitButton);
                }
                break;
            }
        }
    }

/*------------------------------------------ASYNC TASK to connect to MYSQL SB----------------------------------------------------------------------*/

    /*---------IMPORTANT CODE!----------------------------------------------------------------------------------------------*/
    //add New Case to MySQL DB
    class addCase extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddCase.this);
            pDialog.setMessage("Adding Case \nPlease Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //getID parameters: (tablename, idname, compare to string, columnName, columnNumber)
            //everything can be found in the DBController.java
            String nameId = id_user;
            String description = mDesc.getText().toString();
            String actionTaken = mActionTaken.getText().toString();
            String assignee = assigneeID;
            String status = String.valueOf(mStatus.getSelectedItemPosition() + 1);

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("name", nameId));
            parameters.add(new BasicNameValuePair("description", description));
            parameters.add(new BasicNameValuePair("actiontaken", actionTaken));
            parameters.add(new BasicNameValuePair("assignee", assignee));
            parameters.add(new BasicNameValuePair("status", status));

            Log.d("request!", "starting");

            JSONObject json = jsonParser.makeHttpRequest(
                    ADD_CASE_URL, "POST", parameters);

            try {

                //check log cat for JSON response
                Log.d("Inserting... ", json.toString());

                //Check for SUCCESS TAG
                success = json.getInt(MainActivity.TAG_SUCCESS);
                if (success == 1) {
                    //check log cat for JSON response
                    Log.d("Successfully Added Case: ", json.toString());

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    addCaseSQLite("10");
                    startActivity(intent);

                    finish();
                    return json.getString(MainActivity.TAG_MESSAGE);
                } else {
                    pDialog.dismiss();
                    return json.getString(MainActivity.TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String message) {
            // dismiss the dialog after adding the case
            pDialog.dismiss();
            if (message != null) {
                Toast.makeText(AddCase.this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    /*---------------------------UPDATE CASE TO MYSQL DB---------------------------------------------------------*/

    class updateCase extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddCase.this);
            pDialog.setMessage("Updating Case \nPlease Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //getID parameters: (tablename, idname, compare to string, columnName, columnNumber)
            //everything can be found in the DBController.java
            String user_id = userControl.getID("users", "userId", mUser.getText().toString(), MainActivity.TAG_NAME, 0);
            String description = mDesc.getText().toString();
            String actionTaken = mActionTaken.getText().toString();
            String status = String.valueOf(mStatus.getSelectedItemPosition() + 1);

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("id", caseID));
            parameters.add(new BasicNameValuePair("user_id", user_id));
            parameters.add(new BasicNameValuePair("description", description));
            parameters.add(new BasicNameValuePair("actiontaken", actionTaken));
            parameters.add(new BasicNameValuePair("status", status));

            Log.d("REQUEST!", "starting");

            JSONObject json = jsonParser.makeHttpRequest(
                    UPDATE_CASE_URL, "POST", parameters);

            try {
                //check log cat for JSON response
                Log.d("Updating... ", json.toString());

                //Check for SUCCESS TAG
                success = json.getInt(MainActivity.TAG_SUCCESS);
                if (success == 1) {
                    //check log cat for JSON response
                    Log.d("Successfully Updated Case: ", json.toString());

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    updateCaseSQLite("20");
                    startActivity(intent);

                    finish();
                    return json.getString(MainActivity.TAG_MESSAGE);
                }else if(success == 0){
                    pDialog.dismiss();
                    return json.getString(MainActivity.TAG_MESSAGE);
                } else {
                    return json.getString(MainActivity.TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String message) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            if (message != null) {
                Toast.makeText(AddCase.this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    /*------------------------CODE TO UPLOAD IMAGES TO PHP SERVER------------------------------------*/
    //uploadImages from stunningco.de and stackoverflow
    static class uploadImage extends AsyncTask<ArrayList<String>, Void, Integer>{
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        String urlServer = INSERT_IMAGE_URL;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;
        int serverResponseCode;
        String serverResponseMessage;

        String id_case;
        public uploadImage(String id_case){
            this.id_case = id_case;
        }

        public Integer doInBackground(ArrayList<String>... params){
            try
            {
                ArrayList<String> pathsToOurFile = params[0];
                for(int i = pathsToOurFile.size()-1; i >= 0 ; --i) {
                    FileInputStream fileInputStream = new FileInputStream(new File(pathsToOurFile.get(i)));

                    URL url = new URL(urlServer);
                    connection = (HttpURLConnection) url.openConnection();

                    // Allow Inputs &amp; Outputs.
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    // Set HTTP method to POST.
                    connection.setRequestMethod("POST");

                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);


                    outputStream = new DataOutputStream(connection.getOutputStream());
                    //text to create a folder for the image(s)
                    outputStream.writeBytes("--" + boundary+lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"id\"" + lineEnd);
                    outputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    outputStream.writeBytes(lineEnd);
                    if(title.equalsIgnoreCase("add case")) {
                        outputStream.writeBytes(String.valueOf(Integer.parseInt(id_case) + 1) + lineEnd);
                    }else if(title.equalsIgnoreCase("edit case")){
                        outputStream.writeBytes(id_case + lineEnd);
                    }
                    //image
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathsToOurFile.get(i) + "\"" + lineEnd);
                    outputStream.writeBytes(lineEnd);


                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // Read file
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {
                        outputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    // Responses from the server (code and message)
                    serverResponseCode = connection.getResponseCode();
                    serverResponseMessage = connection.getResponseMessage();

                    fileInputStream.close();
                    outputStream.flush();
                    outputStream.close();
                }
            }
            catch (final Exception ex)
            {
                Log.e("Upload Image Failed: ", String.valueOf(ex));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Integer result){
            AddPicture.listOfImages = new ArrayList<>();
        }
    }

    /*-----------------------CHECK IF IMAGES EXIST------------------------------------------------*/
    class checkImages extends AsyncTask<String, Void, ArrayList<String>>{
        ArrayList<String> arrList;
        Context context;
        public checkImages(Context context){
            this.context = context;
        }

        @Override
        protected ArrayList<String> doInBackground(String... voids) {
            int success = 0;
            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("id", case_id));

            Log.d("REQUEST!", "checking if images exists");

            JSONObject json = jsonParser.makeHttpRequest(
                    CHECK_IMAGE_URL, "POST", parameters);
            if(json != null) {
                try {
                    success = json.getInt(MainActivity.TAG_SUCCESS);
                    if (success == 1) {
                        JSONArray paths = json.getJSONArray("image_paths");
                        arrList = new ArrayList<>();
                        //add the image paths to array list
                        for (int i = 0; i < paths.length(); i++) {
                            String fixedPath = fixStringPath(paths.getString(i), AddCase.this);
                            arrList.add(fixedPath);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddCase.this, "images exists", Toast.LENGTH_SHORT).show();
                            }
                        });

                        return arrList;
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddCase.this, "images don't exist", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return null;
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(AddCase.this)
                                .setIcon(R.drawable.nowifi)
                                .setTitle("Something is wrong with the connection")
                                .setMessage("Please check your internet connection to retrieve images \n " +
                                        "or turn off the wifi to use in offline mode")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //code for exit
                                        //android.os.Process.killProcess(android.os.Process.myPid());
                                        //System.exit(1);
                                        finish();
                                    }
                                })
                                .show();
                    }
                });
            }


            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if(result != null && result.size() != 0 && context != null) {
                path = fixStringPath(result.get(0), AddCase.this);
                new AsyncMethods.DownloadImageTask(mBitmapImage)
                        .execute(path);
                mFilePath.setText(path);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sharedPreference.setString(AddCase.this, "imgExists", "true");
                    }
                });
            }else if(result != null && result.size() != 0){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sharedPreference.setString(AddCase.this, "imgExists", "true");
                    }
                });
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sharedPreference.setString(AddCase.this, "imgExists", "false");
                    }
                });
            }
            /*if(result != null && context != null){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //AddPicture ap = (AddPicture) context;
                        AddPicture.listOfImages = result;
                        Toast.makeText(context, "wooooork", Toast.LENGTH_LONG).show();
                        Toast.makeText(context, String.valueOf(result.size())+" work please", Toast.LENGTH_LONG).show();
                        Toast.makeText(context, String.valueOf(AddPicture.listOfImages.size())+" worksadads please", Toast.LENGTH_LONG).show();
                    }
                });
            }*/
        }
    }
    //Fixes the path to make it a valid URL
    public static String fixStringPath(String path, Context c){
        path = path.replaceAll("[+^,\\\\\"\\]\\[]", "").replace("../../", "http://"+ MainActivity.TAG_IP +"/chd/");
        return path;
    }

}


