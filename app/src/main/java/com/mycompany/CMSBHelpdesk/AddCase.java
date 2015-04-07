package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AddCase extends MainActivity{

    private Spinner mUser, mAssignee, mStatus;
    private Button maddCBtn, mSubmit;
    private TextView mId, mDesc, mCompany, mEmail, mTel;
    private spinnerMethods sM = new spinnerMethods();
    private String[] mNameList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_case);

        //Initialises the variables (just to make it look neater)
        initialise();

        Intent intent = this.getIntent();

        if(intent.equals(AddNewUser.class)){
            retrieve();
        }
        //for the spinners dynamic property
        sM.onItemSelected(mUser, mCompany, mEmail, mTel);
        sM.changeColor(mStatus);

        //This just listens for when a button is clicked.
        addListenerOnButton();
    }


    public void addListenerOnButton() {

        final Context context = this;

        //Submit button
        mSubmit = (Button) findViewById(R.id.submitBtn);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                addCase();
                startActivity(intent);
            }
        });

        maddCBtn = (Button)findViewById(R.id.addContactBtn);
        maddCBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Toast.makeText(getApplicationContext(), "Add New User", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, AddNewUser.class);
                startActivity(intent);
            }
        });
    }

    public void addCase(){
        //Intent intent = new Intent(this, MainActivity.class);
        String descriptions = mDesc.getText().toString();
        String users = mUser.getSelectedItem().toString();
        String assignees = mAssignee.getSelectedItem().toString();
        String statuses = mStatus.getSelectedItem().toString();

        sharedPreference.setString(this,"key" ,descriptions);
        sharedPreference.setString(this,"key1" ,users);
        sharedPreference.setString(this,"key2" ,assignees);
        sharedPreference.setString(this,"key3" ,statuses);
        /*
        intent.putExtra("key", descriptions);
        intent.putExtra("key1", users);
        intent.putExtra("key2", assignees);
        intent.putExtra("key3", statuses);
        */
        //setResult(RESULT_OK, intent);
        //startActivity(intent);
    }

    //Adapter to input new user into the Users Spinner
    public void nUser(Spinner spin, String nam){
        //this.mNameList = new String[] {nam};

        ArrayAdapter<CharSequence> spinnerAA = ArrayAdapter.createFromResource(this, R.array.nameList ,android.R.layout.simple_spinner_item);
        //SimpleCursorAdapter spinnerAA = new SimpleCursorAdapter(this,android.R.layout.simple_spinner_item,c,from,to );
        spinnerAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAA.insert(nam, mNameList.length+1);
        spin.setAdapter(spinnerAA);
    }
    public void retrieve(){
        String getNewU = sharedPreference.getString(this, "newUse");
        Toast.makeText(getApplicationContext(), getNewU, Toast.LENGTH_SHORT).show();
        //nUser(mUser, getNewU);
    }


    private void initialise(){

        mNameList = getResources().getStringArray(R.array.nameList);

        mDesc = (TextView)findViewById (R.id.actionTaken);
        mUser = (Spinner) findViewById(R.id.spinnerNames);
        mAssignee = (Spinner) findViewById(R.id.spinnerAssignee);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);
        mCompany = (TextView)findViewById(R.id.company);
        mEmail = (TextView) findViewById(R.id.email);
        mTel = (TextView)findViewById(R.id.tel);

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
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Closes the keyboard of you tap anywhere else on the screen!!
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }

}
