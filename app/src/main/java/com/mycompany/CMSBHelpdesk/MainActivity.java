package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private List<Case> casesArray = new ArrayList<Case>();
    private ListView mCasesLV;
    private TextView stat;
    private String getDesc, getUser, getAssignees, getStatus, getId = "";
    private FragmentManager fm;
    //TODO: IMPLEMENT DBADAPTER!!!
    private DbAdapter mDbHelper;
    private int j = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check if user was logged in before:
        //if not go to login page, else continue.

        String checkLog = sharedPreference.getString(this, "login");
        String checkPass = sharedPreference.getString(this, "pass");
        if(checkLog.equals("") || checkPass.equals("")){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else{
            initialise();

            if(fm.findFragmentById(android.R.id.content) == null){
                SListFragment list = new SListFragment();
                //fm.beginTransaction().add(android.R.id.content, list).commit();
            }
            retrieve();
            addingCase(getId, getDesc, getUser, getAssignees, getStatus);
        }

    }

    public void retrieve(){
        //String getD = getIntent().getStringExtra("key");
        getDesc = sharedPreference.getString(this,"key");
        //String getU = getIntent().getStringExtra("key1");
        getUser = sharedPreference.getString(this,"key1");
        //String getA = getIntent().getStringExtra("key2");
        getAssignees = sharedPreference.getString(this,"key2");
        //String getS = getIntent().getStringExtra("key3");
        getStatus = sharedPreference.getString(this,"key3");
    }

    public void addingCase(String gId, String gD, String gU, String gA, String gS){
        casesArray.add(new Case(gId, gD, gU, gA, gS));
        populateList();
    }
    private void initialise(){
        mCasesLV = (ListView) findViewById(R.id.listView1);
        fm = getFragmentManager();
    }
    //Dynamically update list
    public void populateList(){
        ArrayAdapter<Case> adapter = new CaseListAdapter();
        mCasesLV.setAdapter(adapter);
    }
    //CASE LIST ADAPTER
    private class CaseListAdapter extends ArrayAdapter<Case> {
        public CaseListAdapter() {
            super (MainActivity.this, R.layout.listview_item, casesArray);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            if(view == null)
                view = getLayoutInflater().inflate(R.layout.listview_item, parent, false);

            Case currentCase = casesArray.get(position);

            TextView ident = (TextView) view.findViewById(R.id.IDMain);
            ident.setText(currentCase.getId() + j++);
            TextView descrip = (TextView)  view.findViewById(R.id.descMain);
            descrip.setText(currentCase.getDesc());
            TextView userr = (TextView)  view.findViewById(R.id.userMain);
            userr.setText(currentCase.getUser());
            TextView assigned = (TextView)  view.findViewById(R.id.assigneeMain);
            assigned.setText(currentCase.getAssignee());
            stat = (TextView) view.findViewById(R.id.statusMain);
            stat.setText(currentCase.getStatus());

            return view;
        }
    }


    //This is where the database stuff and bundles and intents goes to connect with the listview!:
    public static class SListFragment extends ListFragment {

    }

    //This stuff is just the action bar for like settings and stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        if(id == R.id.add_case){
            Intent intent = new Intent(context, AddCase.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.action_settings){
            Intent intent = new Intent(context, Settings.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.log_out) {
            sharedPreference.delete(this);
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}