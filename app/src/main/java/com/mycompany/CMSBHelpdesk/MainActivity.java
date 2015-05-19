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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public static int checker = 0;
    public static String checkLog;

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
    JSONArray users = null;

    private ProgressDialog pDialog;
    private static final String CASE_URL = "http://abelhii.comli.com/getCases.php";//"http://abelhii.freeoda.com/getCases.php";
    private static final String USER_URL = "http://abelhii.comli.com/getUsers.php";
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_CASES = "caseslist";
    public static final String TAG_USERS = "userslist";

    //case stuff
    public static final String TAG_USERNAME = "user";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_ASSIGNEE = "assignee";
    public static final String TAG_STATUS = "status";
    public static final String TAG_ID = "id";

    //user stuff
    public static final String TAG_USERID = "userId";
    public static final String TAG_NAME = "name";
    public static final String TAG_COMPANY = "company";
    public static final String TAG_EMAIL = "email";
    public static final String TAG_TELEPHONE = "telephone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkLog = sharedPreference.getString(this, "login");
        if(checkLog != "") {
            setTitle(checkLog.substring(0, 1).toUpperCase() + checkLog.substring(1));
        }
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

            checker = controller.checkNumRows("cases");
            if (checker == 0 || checker < 0) {
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
            }else if(isNetworkConnected() && checker > 0){
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
        // Get Cases records from SQLite DB
        ArrayList<HashMap<String, String>> listCase = controller.getAllCases();

        // If users exists in SQLite DB
        if (listCase.size() != 0) {

            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, listCase,
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
        }

        //To show that SQLite DB is not empty
        checker = controller.checkNumRows("cases");
        controller.close();
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
            controller.refreshCases("cases");
            controller.refreshCases("users");
            //refresh and refill SQLite Database
            new getCases().execute();
            loadList();
            getSQLiteList();
            refreshAtTop();

            //To update the spinner user list too:
            AddCase.check = 0;
            swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }
    //Swipe to reload only when you're at the top of the list functionality:
    public void refreshAtTop() {
        mCasesLV.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
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
                Intent intent = new Intent(MainActivity.this, AddCase.class);
                intent.putExtra("caller", "EditCase");
                Bundle bundle = new Bundle();
                bundle.putString(TAG_ID, controller.getTableValues("cases", 0).get(position));
                bundle.putString(TAG_STATUS, controller.getTableValues("cases", 2).get(position));
                bundle.putString(TAG_USERNAME, controller.getTableValues("cases", 3).get(position));
                bundle.putString(TAG_DESCRIPTION, controller.getTableValues("cases", 4).get(position));
                intent.putExtras(bundle);

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
        swipeLayout.setColorScheme(android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.black);
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
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();// && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }

    public void statusColorChange(){
        String status = stat.getText().toString();

        switch(status.toLowerCase()){
            case "in progress":
                stat.setBackgroundResource(R.color.accent_material_light);
                break;
            case "pending close":
                stat.setBackgroundResource(R.color.accent_material_light);
                break;
        }
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
            checkLog = sharedPreference.getString(MainActivity.this, "login");
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("username", checkLog));
            try{
                //get JSON string from URL
                JSONObject json = jsonParser.makeHttpRequest(CASE_URL, "GET", parameters);
                if(isNetworkConnected() == true) {
                    //check log cat for JSON response
                    Log.d("Cases: ", json.toString());
                    //Check for SUCCESS TAG
                    success = json.getInt(TAG_SUCCESS);
                }
                if(success == 1) {
                    //cases found, get array of cases
                    cases = json.getJSONArray(TAG_CASES);

                    //loop through all the cases:
                    for (int i = 0; i < cases.length(); i++) {
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
                        map.put(TAG_USERNAME, user);
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
    }

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
            ArrayAdapter<String> adapt = new CaseListAdapter();
            mCasesLV.setAdapter(adapt);
            return mCasesLV;
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
            controller.refreshCases("cases");
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