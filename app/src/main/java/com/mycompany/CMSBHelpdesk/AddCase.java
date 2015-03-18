package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Array;


public class AddCase extends MainActivity{

    private Spinner mUser, mAssignee, mStatus;
    private Button maddCBtn, mSubmit;
    private TextView mId, mDesc;
    int j = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_case);

        //Initialises the variables (just to make it look neater)
        initialise();

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
                //mId.setId(j++);

                String descriptions = mDesc.getText().toString();
                String users = mUser.getSelectedItem().toString();
                String assignees = mAssignee.getSelectedItem().toString();
                String statuses = mStatus.getSelectedItem().toString();

                //intent.putExtra("keyId", mId.getText());
                intent.putExtra("key", descriptions);
                intent.putExtra("key1", users);
                intent.putExtra("key2", assignees);
                intent.putExtra("key3", statuses);

                setResult(RESULT_OK, intent);
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

    private void initialise(){

        mDesc = (TextView)findViewById (R.id.actionTaken);
        mUser = (Spinner) findViewById(R.id.spinnerNames);
        mAssignee = (Spinner) findViewById(R.id.spinnerAssignee);
        mStatus = (Spinner) findViewById(R.id.spinnerStatus);

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
