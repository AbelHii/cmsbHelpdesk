package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Scroller;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.sharedPreference;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddCase extends ActionBarActivity {

    public Button mUser;
    public ImageButton mInfo;
    private Spinner mStatus;
    private String id = "0", name, username = "", description,actionT, assigneeID, statusID, caseID, company, email, telephone, sync = "", id_user;
    private Button maddCBtn, mSubmit;
    private TextView mIDCase, mContactName, mAssignee, mCompany, mEmail, mTel;
    private TextView mActionTaken, mDesc;
    private SharedPreferences sp;
    private SharedPreferences.Editor e;

    //To check whether its in addcase or editcase
    String caller, title = "Add Case";

    // DB Class to perform DB related operations
    DBController userControl = new DBController(this);

    //Stores the values for AddCase and EditCase
    public static ArrayList<String> spinnerUsernames;
    public static ArrayList<String> companyV;

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();

    private ProgressDialog pDialog;
    public static String ADD_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/AddCase.php";
    public static String UPDATE_CASE_URL = "http://"+ MainActivity.TAG_IP +"/chd/public/app/UpdateCase.php";
    /*----------------------------ON CREATE--------------------------------------------------------*/
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
                    break;
                case "addcase":
                    setTitle(title);
                    mIDCase.setText(String.valueOf(Integer.parseInt(id)+1));
                    break;
            }
        }

        //This just listens for when a button is clicked.
        addListenerOnButton();
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

        mUser = (Button) findViewById(R.id.spinnerNames);//(AutoCompleteTextView) findViewById(R.id.spinnerNames);
        mInfo = (ImageButton) findViewById(R.id.moreInfo);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);

        mIDCase = (TextView) findViewById(R.id.IDCase);
        mContactName = (TextView) findViewById(R.id.contactName);
        mCompany = (TextView)findViewById(R.id.company);
        mEmail = (TextView) findViewById(R.id.email);
        mTel = (TextView)findViewById(R.id.tel);
        //mAssignee = (TextView) findViewById((R.id.assigneeName));

        mDesc = (TextView)findViewById (R.id.caseDesc);
        mActionTaken = (TextView) findViewById (R.id.actionTaken);

        //getting the most recent case id and assignee id
        id = sharedPreference.getString(AddCase.this, MainActivity.TAG_ID);
        assigneeID = sharedPreference.getString(AddCase.this, MainActivity.TAG_LOGIN_ID);

    }

    //Check if network is connected
    public boolean isNetworkConnected(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();// && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }

    /*--------------------ADD LISTENER TO BUTTON ---------------------------------------------------------*/
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
        mDesc.setBackgroundResource(R.drawable.on_btn_click);
        mActionTaken.setBackgroundResource(R.drawable.on_btn_click);
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
                LayoutInflater layoutInflater
                        = (LayoutInflater)getBaseContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = layoutInflater.inflate(R.layout.contact_info, null);
                final PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        AbsoluteLayout.LayoutParams.WRAP_CONTENT,
                        AbsoluteLayout.LayoutParams.WRAP_CONTENT);
                if(mUser.getText().toString().equals("") || mUser.getText().toString().equals(null)) {
                }else {
                    setTexts(popupView);
                }
                popupWindow.setAnimationStyle(R.style.popupAnimation);
                popupWindow.setBackgroundDrawable(new BitmapDrawable());
                popupWindow.setOutsideTouchable(true);
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, -300);

                //popupWindow.showAsDropDown(mInfo, 50, -100);
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
                    if (!mUser.getText().toString().trim().equalsIgnoreCase("")) {
                        mSubmit.setTextAppearance(AddCase.this, R.style.submitButton);
                        //Logic for adding case:
                        if (title.trim().equalsIgnoreCase("add Case")) { //ADD
                            if (isNetworkConnected()) {
                                id_user = userControl.getID("users", "userId", mUser.getText().toString(), MainActivity.TAG_NAME, 0);
                                new addCase().execute();
                                Toast.makeText(AddCase.this, "Added Case", Toast.LENGTH_LONG).show();
                            } else if (!isNetworkConnected()) {
                                Intent intent = new Intent(context, MainActivity.class);
                                addCaseSQLite("10");
                                Toast.makeText(AddCase.this, "Adding Case For Sync ", Toast.LENGTH_LONG).show();
                                startActivity(intent);
                                finish();
                            }
                            //Logic for editing case:
                        } else if (title.trim().equalsIgnoreCase("edit Case")) { //EDIT
                            Intent intent = new Intent(context, MainActivity.class);
                            if (isNetworkConnected()) {
                                new updateCase().execute();
                                Toast.makeText(AddCase.this, "Updated Case", Toast.LENGTH_LONG).show();
                            } else if (!isNetworkConnected() && sync.equals("10")) {
                                updateCaseSQLite("10");
                                Toast.makeText(AddCase.this, "Adding Case For Sync", Toast.LENGTH_LONG).show();
                                startActivity(intent);
                                finish();
                            } else if (!isNetworkConnected() && (sync.equals("") || sync.equals("20"))) {
                                updateCaseSQLite("20");
                                Toast.makeText(AddCase.this, "Updating Case For Sync", Toast.LENGTH_LONG).show();
                                startActivity(intent);
                                finish();
                            }
                        }
                    } else {
                        mSubmit.setTextColor(Color.parseColor("#CC0000"));
                        Toast.makeText(AddCase.this, "Name is Empty", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (1) : {
                if (resultCode == Activity.RESULT_OK) {
                    String newText = data.getStringExtra("result");
                    mDesc.setText(newText);
                }
                break;
            }case (2) : {
                if (resultCode == Activity.RESULT_OK) {
                    String newText = data.getStringExtra("result");
                    mActionTaken.setText(newText);
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

    @Override
    public void onBackPressed(){
        this.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /*----------------------------------SQLITE EDITS-------------------------------*/
    //Adds Case to SQLite:
    public void addCaseSQLite(String num){
        if(!isNetworkConnected()){
            sync = num;
        }
        String identification = String.valueOf(Integer.parseInt(id)+1);

        //insertOneCase(String id, String assignee, String status, String user, String desc, String aT, String logID, String statID)
        userControl.insertOneCase(identification,
                mStatus.getSelectedItem().toString(),
                mUser.getText().toString(),
                mDesc.getText().toString(),
                mActionTaken.getText().toString(),
                assigneeID,
                String.valueOf(mStatus.getSelectedItemId() + 1),
                sync);
    }
    //Updates SQLite Cases
    public void updateCaseSQLite(String num){
        if(!isNetworkConnected()){
            sync = num;
        }

        userControl.updateOneCase(caseID,
                mStatus.getSelectedItem().toString(),
                mUser.getText().toString(),
                mDesc.getText().toString(),
                mActionTaken.getText().toString(),
                assigneeID,
                String.valueOf(mStatus.getSelectedItemPosition()+1),
                sync);
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
        id = bund.getString(MainActivity.TAG_ID);
        description = bund.getString(MainActivity.TAG_DESCRIPTION);
        actionT = bund.getString(MainActivity.TAG_ACTION_TAKEN);
        username = bund.getString(MainActivity.TAG_USERNAME);
        statusID = bund.getString(MainActivity.TAG_STATUS_ID);
        assigneeID = bund.getString(MainActivity.TAG_LOGIN_ID);
        sync = bund.getString(MainActivity.TAG_SYNC);

        //This just sets the static values according to the user
        //SET TEXT
        mUser.setText(username);
        mUser.setTextAppearance(this, R.style.bigFont);
        mDesc.setText(description);
        mActionTaken.setText(actionT);
        mStatus.setSelection(Integer.parseInt(statusID) - 1);
        mIDCase.setText(id);

        id_user = userControl.getID("users", "userId", mUser.getText().toString(), MainActivity.TAG_NAME, 0);

        //mAssignee.setText(assignee.substring(0, 1).toUpperCase() + assignee.substring(1));
    }

    public void retrieveUserList(Intent data){
        Bundle b = data.getExtras();

        name = b.getString(MainActivity.TAG_NAME);
        //company = b.getString(MainActivity.TAG_COMPANY);
        //email = b.getString(MainActivity.TAG_EMAIL);
        //telephone = b.getString(MainActivity.TAG_TELEPHONE);

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
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }if (id == android.R.id.home) {
            this.finish();
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    /*---------------------------IMPORTANT CODE!---------------------------------------------------------*/
    //update Case to MySQL DB
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
            if (message != null) {
                Toast.makeText(AddCase.this, message, Toast.LENGTH_LONG).show();
            }
            pDialog.dismiss();
        }
    }
}


