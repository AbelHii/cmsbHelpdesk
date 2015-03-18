package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    List<Case> casesArray = new ArrayList<Case>();
    ListView mCasesLV;
    TextView stat;
    String getDesc, getUser, getAssignees, getStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialise();
        retrieve();

        addingCase(getDesc, getUser, getAssignees, getStatus);
    }

    public void retrieve(){
        String getD = this.getIntent().getStringExtra("key");
        getDesc = getD;
        String getU = this.getIntent().getStringExtra("key1");
        getUser = getU;
        String getA = this.getIntent().getStringExtra("key2");
        getAssignees = getA;
        String getS = this.getIntent().getStringExtra("key3");
        getStatus = getS;

    }

    public void addingCase(String gD, String gU, String gA, String gS){
        casesArray.add(new Case(gD, gU, gA, gS));
        populateList();
    }

    private void initialise(){
        mCasesLV = (ListView) findViewById(R.id.listView1);
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

           // TextView ident = (TextView) view.findViewById(R.id.IDMain);
           // ident.setText(currentCase.getId());
            TextView descrip = (TextView)  view.findViewById(R.id.descMain);
            descrip.setText(currentCase.getDesc());
            TextView userr = (TextView)  view.findViewById(R.id.userMain);
            userr.setText(currentCase.getUser());
            TextView assigned = (TextView)  view.findViewById(R.id.assigneeMain);
            assigned.setText(currentCase.getAssignee());
            stat = (TextView)  view.findViewById(R.id.statusMain);
            stat.setText(currentCase.getStatus());

            return view;
        }
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
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
