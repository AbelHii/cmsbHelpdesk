package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddCase extends MainActivity{

    public AutoCompleteTextView mUser;
    private Spinner mStatus;
    private String username, description, assignee, status, name, company, email, telephone;
    private Button maddCBtn, mSubmit;
    private TextView mAssignee, mId, mDesc, mCompany, mEmail, mTel;
    private spinnerMethods sM = new spinnerMethods();
    private spinnerValues sV = new spinnerValues();
    private SharedPreferences sp;
    private SharedPreferences.Editor e;
    public static int check = 0;


    // DB Class to perform DB related operations
    DBController userControl = new DBController(this);

    //Stores the values for AddCase and EditCase
    public static ArrayList<String> spinnerUsernames;
    public static ArrayList<String> companyV;
    public static ArrayList<String> emailV;
    public static ArrayList<String> telephoneV;

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<spinnerValues> spinnersList;
    // cases JSONArray
    JSONArray cases = null;

    private ProgressDialog pDialog;
    private static final String USER_URL = "http://abelhii.comli.com/getUsers.php";

    /*----------------------------ON CREATE--------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_case);
        setTitle("Add Case");

        //initialises the variables
        initialise();

        //Check to see if has to retrieve data or not (goes to Add Case or Edit Case)
        String caller = getIntent().getStringExtra("caller");
        if(caller != null) {
            switch (caller.toLowerCase()) {
                case "editcase":
                    setTitle("Edit Case");
                    retrieve();
                    break;
                case "addcase":
                    setTitle("Add Case");
                    break;
            }
        }

        //This is the process which loads the spinner depending on the situation
        check = userControl.checkNumRows("users");
        if (check == 0 || check < 0) {
            //check if connected to internet or not
            if (isNetworkConnected()) {
                new getUsers().execute();
                //Loads the list from MySQL DB
                getSQLiteUsers();
                Toast.makeText(this, "4", Toast.LENGTH_SHORT).show();
            }else {
                //Retrieve previously saved data
                Toast.makeText(this, "No Internet Connection!" +
                                " \n Please Connect to the internet and restart the app",
                        Toast.LENGTH_LONG).show();
            }
        }else if(isNetworkConnected() && check > 0){
            getSQLiteUsers();
            Toast.makeText(this, "5", Toast.LENGTH_SHORT).show();
        }else if(!isNetworkConnected()){
            getSQLiteUsers();
            Toast.makeText(this, "6", Toast.LENGTH_SHORT).show();
        }

        //Setting Assignee name
        String checkLog = sharedPreference.getString(this, "login");
        mAssignee.setText(checkLog.substring(0, 1).toUpperCase() + checkLog.substring(1));

        sM.changeColor(mStatus);
        //This just listens for when a button is clicked.
        addListenerOnButton();
    }

    //-----------------------IMPORTANT CODE!--------------------------------------------------------------------
    //Loads the spinner from the sqlite:
    public void getSQLiteUsers(){
        //get users for AddCase
        userControl.getAllUsers();

        //Populates the Spinners
        spinnerUsernames = userControl.getTableValues("users", 1);
        companyV = userControl.getTableValues("users", 2);
        populateAutoCompTV(mUser, spinnerUsernames);

        mUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUser.showDropDown();
            }
        });
        mUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                //"users" is the table name and getTableValues is in DBController
                companyV = userControl.getTableValues("users", 2);
                emailV = userControl.getTableValues("users", 3);
                telephoneV = userControl.getTableValues("users", 4);

                //name = spinnerUsernames.get(position);
                company = companyV.get(position);
                email = emailV.get(position);
                telephone = telephoneV.get(position);

                sV.setCompany(company);
                sV.setEmail(email);
                sV.setTelephone(telephone);
                //for the spinners dynamic property
                //This sV.onItemSelected auto fills the email and stuff depending on the user selected
                //sV.onItemSelected(mUser, mCompany, mEmail, mTel);
                Toast.makeText(getApplicationContext(), sV.getCompany(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //To show that SQLite DB is not empty
        check = userControl.checkNumRows("users");
    }


    /**
     * Adding AutoCompleteTextView data
     * */
    private void populateAutoCompTV(AutoCompleteTextView spin, ArrayList<String> spinnerItems) {
        ArrayList<String> sValues = spinnerItems;
        // Creating adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, sValues);
        // Drop down layout style
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spin.setAdapter(spinnerAdapter);
    }

    //----------------------------------------INITIALISE-------------------------------------------------------------------
    private void initialise(){
        //SharedPreference initialisation to save things:
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        e = sp.edit();

        spinnersList = new ArrayList<spinnerValues>();

        mUser = (AutoCompleteTextView) findViewById(R.id.spinnerNames);
        //mAssignee = (Spinner) findViewById(R.id.spinnerAssignee);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);
        mDesc = (TextView)findViewById (R.id.actionTaken);
        mCompany = (TextView)findViewById(R.id.company);
        mEmail = (TextView) findViewById(R.id.email);
        mTel = (TextView)findViewById(R.id.tel);

        mAssignee = (TextView) findViewById((R.id.assigneeName));

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("form");
        tabSpec.setContent(R.id.contactInfo);
        tabSpec.setIndicator("Contact Info");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("form");
        tabSpec.setContent(R.id.caseParticulars);
        tabSpec.setIndicator("Case Particulars");
        tabHost.addTab(tabSpec);
    }


    /*--------------------ADD LISTENER TO BUTTON CLICKS----------------------------------------------*/
    public void addListenerOnButton() {

        final Context context = this;

        //Submit button
        mSubmit = (Button) findViewById(R.id.submitBtn);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                //addCase();
                Toast.makeText(AddCase.this, mUser.getText().toString(), Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        });

        maddCBtn = (Button)findViewById(R.id.addContactBtn);
        maddCBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Toast.makeText(getApplicationContext(), "Add New User", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, AddNewUser.class);
                finish();
                startActivity(intent);
            }
        });

    }
    public void addCase(){

    }


 /*-----------------------RETRIEVE VALUES FROM MAINACTIVITY AND SET THEM IN ADDCASE------------------*/
    public void retrieve(){
        Bundle bund = getIntent().getExtras();
        //Retrieving details from when list item is clicked in main activity
        username = bund.getString(MainActivity.TAG_USERNAME);
        description = bund.getString(MainActivity.TAG_DESCRIPTION);
        assignee = bund.getString(MainActivity.TAG_ASSIGNEE);
        status = bund.getString(MainActivity.TAG_STATUS);

        mUser.setText(username);
        mDesc.setText(description);
    }

    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //gets Users from MySQL DB
    class getUsers extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddCase.this);
            pDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            pDialog.setMessage("Updating Users List \nPlease Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{
                //get JSON string from URL
                JSONObject jsonUse = jsonParser.makeHttpRequest(USER_URL, "GET", parameters);
                if(isNetworkConnected() == true) {
                    //check log cat for JSON response
                    Log.d("Users: ", jsonUse.toString());
                    //Check for SUCCESS TAG
                    success = jsonUse.getInt(TAG_SUCCESS);
                }
                if(success == 1) {
                    //users found, get array of users
                    users = jsonUse.getJSONArray(TAG_USERS);

                    //Loop through all the users
                    for(int i =0; i < users.length(); i++){
                        JSONObject u = users.getJSONObject(i);

                        String id = u.getString(TAG_USERID);
                        String name = u.getString(TAG_NAME);
                        String company = u.getString(TAG_COMPANY);
                        String email = u.getString(TAG_EMAIL);
                        String telephone = u.getString(TAG_TELEPHONE);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(TAG_USERID, id);
                        maps.put(TAG_NAME, name);
                        maps.put(TAG_COMPANY, company);
                        maps.put(TAG_EMAIL, email);
                        maps.put(TAG_TELEPHONE, telephone);

                        //add this map to SQLite too
                        userControl.insertUser(maps);
                    }

                    return jsonUse.getString(TAG_MESSAGE);
                }
                else {
                    return jsonUse.getString(TAG_MESSAGE);
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result){
            // dismiss the dialog after getting all products
            if (result != null) {
                AddCase.this.setProgressBarIndeterminateVisibility(false);
                runOnUiThread(new Runnable() {
                    public void run() {
                        //get users for AddCase
                        userControl.getAllUsers();
                        spinnerUsernames = userControl.getTableValues("users", 1);
                        companyV = userControl.getTableValues("users", 2);
                        emailV = userControl.getTableValues("users", 3);
                        telephoneV = userControl.getTableValues("users", 4);

                        populateAutoCompTV(mUser, spinnerUsernames);

                    }
                });
                check = userControl.checkNumRows("users");
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
            pDialog.dismiss();
        }
    }



    //-----------------------------------IMPORTANT CODE!---------------------------------------------------
    //This stuff is just the action bar for like settings and stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
            return true;
        }
        if (id == R.id.backBtn) {
            this.finish();
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            this.finish();
            startActivity(intent);
            return true;
        }
        if(id == R.id.log_out) {
            controller.refreshCases("cases");
            sharedPreference.delete(this);
            this.finish();
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Closes the keyboard if you tap anywhere else on the screen!!
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }

}
