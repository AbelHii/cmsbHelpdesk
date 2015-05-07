package com.mycompany.CMSBHelpdesk;

/**
 * @author Abel Hii
 *
 * SQLite Tutorial from: http://programmerguru.com/android-tutorial/how-to-sync-remote-mysql-db-to-sqlite-on-android/
 *
 * Note:
 * loadList() only works properly when the ArrayList casesList is instantiated
 * after class new getCases().execute() retrieves the json data from MySQL and passes it on
 *
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {

    private List<Case> casesArray = new ArrayList<Case>();
    private ListView mCasesLV;
    private TextView stat;
    private String getDesc, getUser, getAssignees, getStatus, getId = "";
    private int j = 0;
    private SwipeRefreshLayout swipeLayout;
    private SharedPreferences sp;
    private ListView lv;
    public static String checker = "false";

    // DB Class to perform DB related operations
    DBController controller = new DBController(this);
    // Progress Dialog Object
    ProgressDialog prgDialog;
    HashMap<String, String> queryValues;

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<HashMap<String, String>> casesList;
    // cases JSONArray
    JSONArray cases = null;

    private ProgressDialog pDialog;
    private static final String CASE_URL = "http://abelhii.comli.com/getCases.php";//"http://abelhii.freeoda.com/getCases.php";
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_CASES = "caseslist";
    public static final String TAG_USERNAME = "user";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_ASSIGNEE = "assignee";
    public static final String TAG_STATUS = "status";
    public static final String TAG_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String checkLog = sharedPreference.getString(this, "login");
        setTitle(checkLog);
        //To exit the app if no internet connection at login:
        if (getIntent().getBooleanExtra("EXIT", false)) {
            this.finish();
        }

        //check if user was logged in before:
        //if not go to login page, else continue.
        String checkPass = sharedPreference.getString(this, "pass");
        if(checkLog.equals("") || checkPass.equals("")){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        else {
            initialise();
            casesList = new ArrayList<HashMap<String, String>>();

            checker = sharedPreference.getString(this, "checker");
            if (checker.equals("false")) {
                //check if connected to internet or not
                if (isNetworkConnected()) {
                    new getCases().execute();
                    //Loads the list from MySQL DB
                    loadList();
                    getSQLiteList();
                    refreshAtTop();
                    Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
                }else {
                    swipeLayout.setEnabled(false);
                    //Retrieve previously saved data
                    Toast.makeText(this, "No Internet Connection!" +
                                    " \n Please Connect to the internet and restart the app",
                            Toast.LENGTH_LONG).show();
                }
            }else if(isNetworkConnected() && checker.equals("true")){
                getSQLiteList();
                refreshAtTop();
                Toast.makeText(this, "2", Toast.LENGTH_SHORT).show();
            }
            else if(!isNetworkConnected()){
                swipeLayout.setEnabled(false);
                getSQLiteList();
                Toast.makeText(this, "3", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void getSQLiteList(){
        getListView();
        // Get User records from SQLite DB
        ArrayList<HashMap<String, String>> userList = controller.getAllCases();
        // If users exists in SQLite DB
        if (userList.size() != 0) {

            Toast.makeText(getBaseContext(), "work please", Toast.LENGTH_LONG).show();
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, userList,
                    R.layout.list_item, new String[] { TAG_ID,
                    TAG_USERNAME, TAG_ASSIGNEE,
                    TAG_STATUS, TAG_DESCRIPTION},
                    new int[] { R.id.IDMain,
                            R.id.userMain, R.id.assigneeMain,
                            R.id.statusMain, R.id.descMain });
            setListAdapter(adapter);
            lv = (ListView)findViewById(android.R.id.list);
            lv.setAdapter(adapter);
            onListItemClick();
            //Display Sync status of SQLite DB
            Toast.makeText(getApplicationContext(), controller.getSyncStatus(), Toast.LENGTH_LONG).show();
        }
        // Initialize Progress Dialog properties
        prgDialog = new ProgressDialog(this);
        prgDialog.setMessage("Transferring Data from Remote MySQL DB and Syncing SQLite. Please wait...");
        prgDialog.setCancelable(false);

        //To show that SQLite DB is not empty
        checker = "true";
        sharedPreference.setString(this, "checker", checker);
    }


    public void statusColour(){
        //stat.setText(TAG_STATUS);
        if(stat.getText().toString().toLowerCase().contains("not".toLowerCase())){
            stat.setBackgroundColor(0xFF000000);
        }else if(stat.getText().toString().toLowerCase().contains("in".toLowerCase())){
            stat.setBackgroundColor(0x00FF0000);
        }else if(stat.getText().toString().toLowerCase().contains("pen".toLowerCase())){
            stat.setBackgroundColor(0x0000FF00);
        }
    }

    //--------------REFRESH SPINNER-----------------------------------------------------------------------------
    //Functionality for swipe to refresh
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Drop old table:
                controller.refreshCases();
                //refresh and refill SQLite Database
                new getCases().execute();
                loadList();
                getSQLiteList();
                refreshAtTop();
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }
    //Swipe to reload only when you're at the top of the list functionality:
    public void refreshAtTop() {
        mCasesLV.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (mCasesLV == null || mCasesLV.getChildCount() == 0) ?
                                0 : mCasesLV.getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });
    }



    //--------------DISPLAY LIST!-----------------------------------------------------------------------------
    //Load List from MySQL DB
    public void loadList(){
        lv = getListView();
        final ArrayAdapter<HashMap<String, String>> mainAdapter = new ArrayAdapter<HashMap<String, String>>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                casesList);
        mainAdapter.clear();
        mainAdapter.notifyDataSetChanged();
        lv.setAdapter(mainAdapter);
        onListItemClick();
    }
    public void onListItemClick(){
        //when list item is clicked go to add case to edit that item
        //and send data to add case using bundles
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position,
                                    long arg3) {
                sharedPreference.setString(MainActivity.this, "key", "We're in!");
                Intent intent = new Intent(MainActivity.this, AddCase.class);
                intent.putExtra("caller", "EditCase");
                Bundle bundle = new Bundle();
                intent.putExtra(TAG_ID, bundle);
                intent.putExtra(TAG_USERNAME, bundle);
                intent.putExtra(TAG_DESCRIPTION, bundle);
                intent.putExtra(TAG_ASSIGNEE, bundle);
                intent.putExtra(TAG_STATUS, bundle);

                startActivity(intent);
            }
        });
    }




    //--------------INITIALISE AND CHECKS---------------------------------------------------------------------
    //SMALL BUT USEFUL METHODS:
    //initialises variables and such:
    private void initialise(){
        stat = (TextView) findViewById(R.id.statusMain);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }
    // Reload MainActivity
    public void reloadActivity() {
        Intent objIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(objIntent);
    }
    //Check if network is connected
    public boolean isNetworkConnected(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }







    //--------------IMPORTANT CODE!-----------------------------------------------------------------------------
    //To retrieve JSON and connect to MYSQL database
    class getCases extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this, R.style.MyTheme);
            pDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            pDialog.setCancelable(false);
            pDialog.show();
            //MainActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{

                //get JSON string from URL
                JSONObject json = jsonParser.makeHttpRequest(CASE_URL, "GET", parameters);
                if(isNetworkConnected() == true) {
                    //check log cat for JSON response
                    Log.d("Cases: ", json.toString());
                    //Check for SUCCESS TAG
                    success = json.getInt(TAG_SUCCESS);
                }
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

                        //add this map to SQLite too
                        controller.insertCase(map);
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
                                R.layout.list_item, new String[] { TAG_ID,
                                        TAG_USERNAME, TAG_ASSIGNEE,
                                        TAG_STATUS, TAG_DESCRIPTION},
                                new int[] { R.id.IDMain,
                                        R.id.userMain, R.id.assigneeMain,
                                        R.id.statusMain, R.id.descMain });
                        // updating listView
                        setListAdapter(adapter);
                    }
                });
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
            pDialog.dismiss();
        }
    }

    //CASE LIST ADAPTER
    private class CaseListAdapter extends ArrayAdapter {

        public CaseListAdapter() {
            super (MainActivity.this, R.layout.listview_item, android.R.id.list);
        }
        //Trying to alternate the row colours in the listview
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            //View view = super.getView(position, convertView, parent);
            view = view.findViewById(R.id.list_item);
            if (position % 2 == 0)
                view.setBackgroundColor(0x30ffffff);
            else
                view.setBackgroundColor(0x30808080);

            return view;
        }
    }
    private int[] colors = new int[] { 0x30ffffff, 0x30ff2020, 0x30808080 };

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
            ArrayAdapter<String> adapt = new CaseListAdapter();
            mCasesLV.setAdapter(adapt);
        }
        return mCasesLV;
    }
    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }





    //--------------IMPORTANT CODE!-----------------------------------------------------------------------------
    //IMPORTANT NEEDED FOR ACTIVITY TO WORK PROPERLY
    //Below is just the action bar for like settings and stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
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
            intent.putExtra("caller", "AddCase");
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
            this.finish();
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    //Old Methods Not in use:
    public void retrieve(){
        getDesc = sp.getString("desc", null);
        getUser = sp.getString("user", null);
        getAssignees = sp.getString("assign", null);
        getStatus = sp.getString("stat", null);
    }
    public void addingCase(String gId, String gD, String gU, String gA, String gS){
        casesArray.add(new Case(gId, gD, gU, gA, gS));
        //populateList:
        mCasesLV = (ListView)findViewById(android.R.id.list);
        ArrayAdapter<Case> adapter = new CaseListAdapter();
        mCasesLV.setAdapter(adapter);
    }



}