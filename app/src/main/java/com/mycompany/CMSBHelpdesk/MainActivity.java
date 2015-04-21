package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends ActionBarActivity{

    private List<Case> casesArray = new ArrayList<Case>();
    private ListView mCasesLV;
    private TextView stat;
    private String getDesc, getUser, getAssignees, getStatus, getId = "";
    private int j = 0;
    private SharedPreferences sp;

    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<HashMap<String, String>> casesList;

    private ProgressDialog pDialog;
    private static final String CASE_URL = "http://abelhii.comli.com/getCases.php";//"http://abelhii.freeoda.com/getCases.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    private static final String TAG_CASES = "caseslist";
    private static final String TAG_USERNAME = "user";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_ASSIGNEE = "assignee";
    private static final String TAG_STATUS = "status";
    private static final String TAG_ID = "id";
    // cases JSONArray
    JSONArray cases = null;

    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;

    private DbAdapter mDbHelper;
    private Cursor mCursor;
    private ListAdapter listAdapter;
    //private SList list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check if user was logged in before:
        //if not go to login page, else continue.
        String checkLog = sharedPreference.getString(this, "login");
        String checkPass = sharedPreference.getString(this, "pass");
        if(checkLog.equals("") || checkPass.equals("")){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        else{
            initialise();

            casesList = new ArrayList<HashMap<String, String>>();

            new getCases().execute();
            //getListView
            ListView lv = getListView();
            ArrayAdapter<HashMap<String, String>> adapter = new ArrayAdapter<HashMap<String, String>>(this, android.R.layout.simple_list_item_1, casesList);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int arg2,
                                        long arg3) {
                }
            });
            //mDbHelper.open();

//            list = new SList();
  //          list.fillData();
    //        list.registerForContextMenu(list.getListView());

            retrieve();
            addingCase(getId, getDesc, getUser, getAssignees, getStatus);

            }

        }



    private void initialise(){
        //mCasesLV = (ListView) findViewById(R.id.list);
        mDbHelper = new DbAdapter(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
        }
        return mCasesLV;
    }

    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }


    public void retrieve(){
        getDesc = sp.getString("desc", null);
        getUser = sp.getString("user", null);
        getAssignees = sp.getString("assign", null);
        getStatus = sp.getString("stat", null);
    }

    public void addingCase(String gId, String gD, String gU, String gA, String gS){
        casesArray.add(new Case(gId, gD, gU, gA, gS));
        populateList();
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

    class getCases extends AsyncTask<String, String, String> {

        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            MainActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(String... params) {
            int success;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{

                //get JSON string from URL
                JSONObject json = jsonParser.makeHttpRequest(CASE_URL, "GET", parameters);

                //check log cat for JSON response
                Log.d("Cases: ", json.toString());
                //Check for SUCCESS TAG
                success = json.getInt(TAG_SUCCESS);

                if(success == 1){
                    //cases found, get array of cases
                    cases = json.getJSONArray(TAG_CASES);

                    //loop through all the cases:
                    for(int i = 0; i<cases.length(); i++){
                        JSONObject c = cases.getJSONObject(i);

                        //Store each json item in variable
                        String id = c.getString(TAG_ID);
                        String user = c.getString(TAG_USERNAME);
                        String desc = c.getString(TAG_DESCRIPTION);
                        String assignee = c.getString(TAG_ASSIGNEE);
                        String status = c.getString(TAG_STATUS);

                        //create a new HashMap
                        HashMap<String, String> map = new HashMap<String, String>();

                        //add each child node to HashMap Key => value
                        map.put(TAG_ID, id);
                        map.put(TAG_USERNAME,user);
                        map.put(TAG_DESCRIPTION, desc);
                        map.put(TAG_ASSIGNEE, assignee);
                        map.put(TAG_STATUS, status);
                        //add HashList to ArrayList
                        casesList.add(map);
                    }
                    return json.getString(TAG_MESSAGE);
                }
                else {
                    return json.getString(TAG_MESSAGE);
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

                MainActivity.this
                        .setProgressBarIndeterminateVisibility(false);
                // updating UI from Background Thread
                runOnUiThread(new Runnable() {
                    public void run() {
                        /**
                         * Updating parsed JSON data into ListView
                         * */
                        ListAdapter adapter = new SimpleAdapter(
                                MainActivity.this, casesList,
                                R.layout.listview_item, new String[] { TAG_ID,
                                TAG_USERNAME}, new int[] { R.id.IDMain,
                                R.id.userMain });
                        // updating listView
                        setListAdapter(adapter);
                    }
                });
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }



    /**
    //DB things: (need to put it into a list fragment class to use the list methods)
    public class SList extends ListFragment{


        private void fillData() {
            // Get all of the rows from the database and create the item list
            mCursor = mDbHelper.fetchAllCases();
            startManagingCursor(mCursor);

            // Create an array to specify the fields we want to display in the list (only TITLE)
            String[] from = new String[]{DbAdapter.KEY_USER};

            // and an array of the fields we want to bind those fields to (in this case just text1)
            int[] to = new int[]{R.id.users};

            // Now create a simple cursor adapter and set it to display
            SimpleCursorAdapter cases =
                    new SimpleCursorAdapter(MainActivity.this, listview_item, mCursor, from, to);
            setListAdapter(cases);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            Cursor c = mCursor;
            c.moveToPosition(position);
            Intent i = new Intent(MainActivity.this, AddCase.class);
            i.putExtra(DbAdapter.KEY_ROWID, id);
            i.putExtra(DbAdapter.KEY_USER, c.getString(
                    c.getColumnIndexOrThrow(DbAdapter.KEY_USER)));
            i.putExtra(DbAdapter.KEY_DESC, c.getString(
                    c.getColumnIndexOrThrow(DbAdapter.KEY_DESC)));
            startActivityForResult(i, ACTIVITY_EDIT);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);
            Bundle extras = intent.getExtras();
            switch(requestCode) {
                case ACTIVITY_CREATE:
                    String user = extras.getString(DbAdapter.KEY_USER);
                    String desc = extras.getString(DbAdapter.KEY_DESC);
                    mDbHelper.createCase(user, desc);
                    fillData();
                    break;
                case ACTIVITY_EDIT:
                    Long rowId = extras.getLong(DbAdapter.KEY_ROWID);
                    if (rowId != null) {
                        String editUser = extras.getString(DbAdapter.KEY_USER);
                        String editDesc = extras.getString(DbAdapter.KEY_DESC);
                        mDbHelper.updateNote(rowId, editUser, editDesc);
                    }
                    fillData();
                    break;
            }
        }

    }

    private void addCase(){
        Intent i = new Intent(this, AddCase.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
*/
    //This stuff is just the action bar for like settings and stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Context context = this;

        //noinspection SimplifiableIfStatement
        if(id == R.id.add_case){
            Intent intent = new Intent(context, AddCase.class);
            startActivity(intent);
            //addCase();
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