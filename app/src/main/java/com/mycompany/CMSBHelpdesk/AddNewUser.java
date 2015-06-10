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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AddNewUser extends AddCase implements View.OnClickListener {

    Button mcancel, addUser;
    EditText mNewName, mNewEmail, mNewTel;
    Spinner mNewCompany;

    Set<String> companyList;

    ProgressDialog pDialog;
    JSONParser jsonParser = new JSONParser();

    private static String ADD_NEW_USER_URL = "http://10.1.2.52/chd/public/abel/addNewUser.php";//http://abelhii.comli.com/addNewUser.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_user);

        //getSQLiteUsers();
        initialise();

        populateSpinner(mNewCompany, companyList);
        mNewCompany.setSelection(1);

        backToMain(mcancel);
        backToMain(addUser);
    }

    public void initialise(){
        //converting companyV to Set because it doesn't allow duplicates:
        ArrayList<String> companyV1 = companyV;
        companyList = new HashSet<>(companyV1);

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
                                intent.putExtra("caller", "addCase");
                                AddNewUser.this.finish();
                                startActivity(intent);
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
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            String name = mNewName.getText().toString();
            String company = mNewCompany.getSelectedItem().toString();
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

                    userList.check = 0;

                    Intent intent = new Intent(getApplicationContext(), AddCase.class);
                    intent.putExtra("caller", "addCase");
                    userControl.openDB();
                    userControl.insertValue("users", "name", name);
                    userControl.close();
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
            pDialog.dismiss();
            if (message != null){
                Toast.makeText(AddNewUser.this, message, Toast.LENGTH_LONG).show();
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
            Intent intent = new Intent(context, AddCase.class);
            AddNewUser.this.finish();
            startActivity(intent);
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            AddNewUser.this.finish();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

}
