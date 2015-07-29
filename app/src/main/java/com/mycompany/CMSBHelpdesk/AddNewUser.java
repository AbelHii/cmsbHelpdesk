package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.internetCheck;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AddNewUser extends AddCase{

    Button mcancel, addUser;
    EditText mNewName, mNewEmail, mNewTel;
    Spinner mNewCompany;

    public static ArrayList<String> usernamesList, companyV;

    Set<String> companyList;

    ProgressDialog pDialog;
    JSONParser jsonParser = new JSONParser();

    private static String ADD_NEW_USER_URL = "http://"+MainActivity.TAG_IP+"/chd/public/app/addNewUser.php";//http://abelhii.comli.com/addNewUser.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_user);
        setTitle("Add New User");

        //getSQLiteUsers();
        initialise();

        populateSpinner(mNewCompany, companyList);
        mNewCompany.setSelection(1);

        onClickListener();
    }

    public void initialise(){
        //converting companyV to Set because it gets rid of duplicates:
        companyV = userControl.getColumn("users", MainActivity.TAG_COMPANY);
        companyList = new HashSet<>(companyV);

        mcancel = (Button)findViewById(R.id.cancelBtn);
        addUser = (Button)findViewById(R.id.addNewUser);

        mNewName = (EditText) findViewById(R.id.newName);
        mNewEmail = (EditText) findViewById(R.id.newEmail);
        mNewTel = (EditText) findViewById(R.id.newTel);
        mNewCompany = (Spinner) findViewById(R.id.newCompany);
    }

    //Sorts list
    public List<String> sortList(List<String> list){
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s, String s2) {
                return s.compareToIgnoreCase(s2);
            }
        });
        return list;
    }
    //Check if a name already exists or if nothing is selected for company
    public boolean nameExists(String name, ArrayList nameList){
        if(Arrays.asList(nameList).contains(name)) {
            Toast.makeText(getApplicationContext(),
                    name + " already exists", Toast.LENGTH_SHORT).show();
            return true;
        }else if(name.trim().equalsIgnoreCase("")){
            Toast.makeText(getApplicationContext(),
                    "Name: is empty", Toast.LENGTH_SHORT).show();
            return true;
        }else if(mNewCompany.getSelectedItem() == null){
            Toast.makeText(getApplicationContext(),
                    "Company: is empty", Toast.LENGTH_SHORT).show();
            return true;
        }else if(mNewEmail.getText().toString().trim().equals("")){
            Toast.makeText(getApplicationContext(),
                    "Email: is empty", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void onClickListener(){
        mcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddNewUser.this.finish();
            }
        });

        addUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //checks if inputted name already exists
                //if not carry on:
                if(!nameExists(mNewName.getText().toString(), usernamesList) && internetCheck.isNetworkConnected(AddNewUser.this)){
                    new newUserAdd().execute();
                }/*else {
                    userControl.insertValue("users", "name", mNewName.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), AddCase.class);
                    intent.putExtra("caller", "addCase");
                    AddNewUser.this.finish();
                    startActivity(intent);
                }*/
            }
        });
    }


    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //add New User from MySQL DB
    class newUserAdd extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddNewUser.this);
            pDialog.setMessage("Adding New User \nPlease Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            String name = mNewName.getText().toString();
            String company = userControl.getID("users", MainActivity.TAG_DIVISION_ID, mNewCompany.getSelectedItem().toString(), MainActivity.TAG_COMPANY);
            //String.valueOf(mNewCompany.getSelectedItemPosition()+1);
            String email = mNewEmail.getText().toString();
            String telephone = mNewTel.getText().toString();

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("name", name));
            parameters.add(new BasicNameValuePair("telephone", telephone));
            parameters.add(new BasicNameValuePair("email", email));
            parameters.add(new BasicNameValuePair("company", company));

            //create a new HashMap
            HashMap<String, String> maps = new HashMap<String, String>();

            maps.put(MainActivity.TAG_NAME, name);
            maps.put(MainActivity.TAG_TELEPHONE, telephone);
            maps.put(MainActivity.TAG_EMAIL, email);
            maps.put(MainActivity.TAG_COMPANY, mNewCompany.getSelectedItem().toString());

            Log.d("request!", "starting");

            //make HTTP request
            JSONObject json = jsonParser.makeHttpRequest(
                    ADD_NEW_USER_URL, "POST", parameters);
            try {
                //Check for SUCCESS TAG
                success = json.getInt(MainActivity.TAG_SUCCESS);
                if (success == 1) {
                    //check log cat for JSON response
                    Log.d("Inserted New User: ", json.toString());

                    userList.check = 0;

                    Intent intent = new Intent(getApplicationContext(), AddCase.class);
                    intent.putExtra("caller", "addCase");
                    userControl.openDB();
                    userControl.insertUser(maps);
                    userControl.close();
                    startActivity(intent);
                    AddNewUser.this.finish();
                    return json.getString(MainActivity.TAG_MESSAGE);
                } else {
                    return json.getString(MainActivity.TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String message){
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            if (message != null){
                Toast.makeText(AddNewUser.this, message, Toast.LENGTH_LONG).show();
                userControl.refreshCases("users");
                userList.check = 0;
            }
        }
    }


    //Populate Spinner
    private void populateSpinner(Spinner spin, Set<String> spinnerItems) {
        ArrayList<String> sValues = new ArrayList<String>(spinnerItems);
        // Creating adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, sValues);
        // Drop down layout style
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spin.setAdapter(spinnerAdapter);
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
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if(imm != null)
                imm.hideSoftInputFromWindow(null, 0);
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
