package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class AddNewUser extends AddCase implements View.OnClickListener {

    Button mcancel, addUser;
    EditText mNewName, mNewEmail, mNewTel;
    Spinner mCompany;

    ArrayList<String> companyList;

    ProgressDialog pDialog;
    JSONParser jsonParser = new JSONParser();

    private static String ADD_NEW_USER_URL = "http://abelhii.comli.com/addNewUser.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_user);

        getSQLiteUsers();
        initialise();

        populateSpinner(mCompany, companyList);

        backToMain(mcancel);
        backToMain(addUser);
    }

    public void initialise(){
        companyList = new ArrayList<String>();
        companyList = companyV;
        companyList = deleteDuplicates(companyList);

        mcancel = (Button)findViewById(R.id.cancelBtn);
        addUser = (Button)findViewById(R.id.addNewUser);
        mNewName = (EditText) findViewById(R.id.newName);
        mNewEmail = (EditText) findViewById(R.id.newEmail);
        mNewTel = (EditText) findViewById(R.id.newTel);
        mCompany = (Spinner) findViewById(R.id.newCompany);
    }

    //Deletes the duplicates in company
    public ArrayList deleteDuplicates(ArrayList<String> list){
        //This sorts it first to make deletion faster
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s, String s2) {
                return s.compareToIgnoreCase(s2);
            }
        });

        //Deleting the duplicate names
        for(int i= list.size()-2; i>0; i--){
            for(int j= i-1; j>0; j--){
                if(list.get(i).equalsIgnoreCase(list.get(j))){
                    list.remove(list.get(j));
                }
                if(list.get(i).equalsIgnoreCase("null")){
                    list.remove(list.get(i));
                }
            }
        }
        list.remove(list.get(0));
        return list;
    }
    //Check if a name already exists
    public boolean nameExists(String name, ArrayList nameList){
        for(int i = 0; i < nameList.size(); i++){
            if(name.trim().equalsIgnoreCase(nameList.get(i).toString().trim())) {
                Toast.makeText(getApplicationContext(),
                        name + " already exists", Toast.LENGTH_SHORT).show();
                return true;
            }else if(name.trim().equalsIgnoreCase("")){
                Toast.makeText(getApplicationContext(),
                        "Name: is empty", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    public void backToMain(View button){
        final Context context = this;

        switch(button.getId()){
            case R.id.cancelBtn:
                mcancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, AddCase.class);
                        startActivity(intent);
                        finish();
                    }
                });
            case R.id.addNewUser:
                addUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //checks if inputted name already exists
                        //if not carry on:
                        if(!nameExists(mNewName.getText().toString(), AddCase.spinnerUsernames)) {
                            if (isNetworkConnected()) {
                                new newUserAdd().execute();
                            } else {
                                userControl.insertValue("users", "name", mNewName.getText().toString());
                                Intent intent = new Intent(getApplicationContext(), AddCase.class);
                                startActivity(intent);
                                AddNewUser.this.finish();
                            }
                        }
                    }
                });
        }
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
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            String name = mNewName.getText().toString();
            String company = mCompany.getSelectedItem().toString();
            String email = mNewEmail.getText().toString();
            String telephone = mNewTel.getText().toString();

            try {
            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("name", name));
            parameters.add(new BasicNameValuePair("telephone", telephone));
            parameters.add(new BasicNameValuePair("email", email));
            parameters.add(new BasicNameValuePair("company", company));

            Log.d("request!", "starting");

            //make HTTP request
            JSONObject json = jsonParser.makeHttpRequest(
                    ADD_NEW_USER_URL, "GET", parameters);

            //check log cat for JSON response
            Log.d("Inserting... ", json.toString());

                //Check for SUCCESS TAG
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    //check log cat for JSON response
                    Log.d("Inserted New User: ", json.toString());

                    AddCase.check = 0;

                    Intent intent = new Intent(getApplicationContext(), AddCase.class);

                    userControl.insertValue("users", "name", name);

                    startActivity(intent);
                    finish();
                    return json.getString(TAG_MESSAGE);
                } else {
                    return json.getString(TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String message){
            // dismiss the dialog after getting all products
            if (message != null){

                Toast.makeText(AddNewUser.this, message, Toast.LENGTH_LONG).show();
            }
            pDialog.dismiss();
        }
    }


    //Populate Spinner
    private void populateSpinner(Spinner spin, ArrayList<String> spinnerItems) {
        ArrayList<String> sValues = spinnerItems;
        // Creating adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, sValues);
        // Drop down layout style
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spin.setAdapter(spinnerAdapter);
    }


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
        else if(id == R.id.backBtn){
            this.finish();
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

    }

    //Closes the keyboard of you tap anywhere else on the screen
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }


    //Check if network is connected
    public boolean isNetworkConnected(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();// && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }

}
