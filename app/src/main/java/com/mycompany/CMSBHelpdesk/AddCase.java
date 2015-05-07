package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

public class AddCase extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

    private Spinner mUser, mAssignee, mStatus;
    private String username, description, assignee, status;
    private Button maddCBtn, mSubmit;
    private TextView mId, mDesc, mCompany, mEmail, mTel;
    private Long mRowId;
    private spinnerMethods sM = new spinnerMethods();
    private spinnerValues sV = new spinnerValues();
    private String[] mNameList;
    private SharedPreferences sp;
    private SharedPreferences.Editor e;

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<spinnerValues> spinnersList;
    // cases JSONArray
    JSONArray cases = null;

    private ProgressDialog pDialog;
    private static final String SPIN_URL = "http://abelhii.comli.com/getUsers.php";//"http://abelhii.freeoda.com/getCases.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_case);
        setTitle("Add Case");
        //initialises the variables
        initialise();


        //Check to see if has to retrieve data or not (goes to Add Case or Edit Case)
        String caller = getIntent().getStringExtra("caller");
        if(caller.equals("EditCase")) {
            setTitle("Edit Case");
            retrieve();
            //setData();
        }
        else if(caller.equals("AddCase")){
            setTitle("Add Case");
            retrieveN();
        }
        //for the spinners dynamic property
        //sV.onItemSelected(mUser, mCompany, mEmail, mTel);
        sM.changeColor(mStatus);

        //This just listens for when a button is clicked.
        addListenerOnButton();
    }


    /**
     * Adding spinner data
     * */
    private void populateSpinner() {
        List<String> labels = new ArrayList<String>();

        for (int i = 0; i < spinnersList.size(); i++) {
            labels.add(spinnersList.get(i).getName().toString());
        }

        // Creating adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels);

        // Drop down layout style - list view with radio button
        spinnerAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        mUser.setAdapter(spinnerAdapter);
    }

    private void initialise(){

        //SharedPreference initialisation to save things:
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        e = sp.edit();

        spinnersList = new ArrayList<spinnerValues>();

        mNameList = getResources().getStringArray(R.array.nameList);

        mUser = (Spinner) findViewById(R.id.spinnerNames);
        mAssignee = (Spinner) findViewById(R.id.spinnerAssignee);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);
        mDesc = (TextView)findViewById (R.id.actionTaken);
        mCompany = (TextView)findViewById(R.id.company);
        mEmail = (TextView) findViewById(R.id.email);
        mTel = (TextView)findViewById(R.id.tel);

        //mUser.setOnItemSelectedListener(AddCase.this);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("form");
        tabSpec.setContent(R.id.contactInfo);
        tabSpec.setIndicator("Contact Info");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("form");
        tabSpec.setContent(R.id.itDeptRef);
        tabSpec.setIndicator("IT Dept. Ref.");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("form");
        tabSpec.setContent(R.id.caseParticulars);
        tabSpec.setIndicator("Case Particulars");
        tabHost.addTab(tabSpec);
    }

    public void addListenerOnButton() {

        final Context context = this;

        //Submit button
        mSubmit = (Button) findViewById(R.id.submitBtn);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                //addCase();
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

        mUser.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //fetches list of Names from MySQL
                new GetSpinners().execute();
                view.clearFocus();
                return true;
            }
        });
    }
    public void addCase(){
        //Intent intent = new Intent(this, MainActivity.class);

        String descriptions = mDesc.getText().toString();
        String users = mUser.getSelectedItem().toString();
        String assignees = mAssignee.getSelectedItem().toString();
        String statuses = mStatus.getSelectedItem().toString();

        e.putString("desc", descriptions);
        e.putString("user", users);
        e.putString("assign", assignees);
        e.putString("stat", statuses);
        e.commit();
    }
    //Adapter to input new user into the Users Spinner
    public void nUser(Spinner spin, String nam){
        //this.mNameList = new String[] {nam};

        ArrayAdapter<CharSequence> spinnerAA = ArrayAdapter.createFromResource(this, R.array.nameList ,android.R.layout.simple_spinner_item);
        //SimpleCursorAdapter spinnerAA = new SimpleCursorAdapter(this,android.R.layout.simple_spinner_item,c,from,to );
        spinnerAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAA.insert(nam, mNameList.length);
        spin.setAdapter(spinnerAA);
    }
    public void retrieve(){
        String get = sharedPreference.getString(this, "key");
        username = getIntent().getExtras().getString(MainActivity.TAG_USERNAME);
        description = getIntent().getExtras().getString(MainActivity.TAG_DESCRIPTION);
        assignee = getIntent().getExtras().getString(MainActivity.TAG_ASSIGNEE);
        status = getIntent().getExtras().getString(MainActivity.TAG_STATUS);
        Toast.makeText(getApplicationContext(), get, Toast.LENGTH_SHORT).show();
    }
    public void retrieveN(){
        String getNewU = sharedPreference.getString(this, "newUse");
        Toast.makeText(getApplicationContext(), "Add a Case", Toast.LENGTH_SHORT).show();
        //nUser(mUser, getNewU);
    }



    private class GetSpinners extends AsyncTask<Void,Void,Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddCase.this, R.style.MyTheme);
            pDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int success = 0;

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try {
                JSONObject json = jsonParser.makeHttpRequest(SPIN_URL, "GET", parameters);
                JSONObject jsonObj = new JSONObject(String.valueOf(json));

                //check log cat for JSON response
                Log.d("Cases: ", json.toString());
                //Check for SUCCESS TAG
                success = json.getInt(MainActivity.TAG_SUCCESS);

                if(success == 1) {
                    if (jsonObj != null) {
                        JSONArray categories = jsonObj
                                .getJSONArray("categories");

                        for (int i = 0; i < categories.length(); i++) {
                            JSONObject getObj = (JSONObject) categories.get(i);
                            spinnerValues spun = new spinnerValues(getObj.getInt("id"),
                                    getObj.getInt("telephone"),
                                    getObj.getString("name"),
                                    getObj.getString("email"),
                                    getObj.getInt("company_id"));
                            spinnersList.add(spun);
                        }

                        JSONObject getObj = (JSONObject) categories.get(2);
                        Toast.makeText(getApplicationContext(),getObj.getString("email"), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (pDialog.isShowing())
            {pDialog.dismiss();}
            populateSpinner();
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        Toast.makeText(
                getApplicationContext(),
                adapterView.getItemAtPosition(position).toString() + " Selected" ,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

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
