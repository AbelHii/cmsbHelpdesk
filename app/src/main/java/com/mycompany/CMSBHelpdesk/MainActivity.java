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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

    private ListView mCasesLV;
    private TextView stat, mSyncStatus;
    private SwipeRefreshLayout swipeLayout;
    private ListView lv;
    public static int checker = 0;
    public static String checkLog;

    //Variables for sync
    private String nameId = "", caseID ="", username="", mDescription="",actionT="", assigneeID="", statusID="", sync="";

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
    private static final String CASE_URL = "http://10.1.2.52/chd/public/abel/getCases.php";//http://abelhii.comli.com/getCases.php";
    private static String SYNC_URL = "";
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_CASES = "caseslist";
    public static final String TAG_USERS = "userslist";

    //case stuff
    public static final String TAG_USERNAME = "user";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_ACTION_TAKEN = "actiontaken";
    public static final String TAG_ASSIGNEE = "assignee";
    public static final String TAG_STATUS = "status";
    public static final String TAG_ID = "id";
    public static final String TAG_LOGIN_ID = "login_id";
    public static final String TAG_STATUS_ID = "status_id";
    public static final String TAG_SYNC = "sync";

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
        String ending = "'s Cases";

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
            getListView();

            //This just sets the title for MainActivity and for people who's names ends with 's'
            if(checkLog.substring(checkLog.length() -1).equals("s")) {ending = "' Cases";}
            setTitle(checkLog.substring(0, 1).toUpperCase() + checkLog.substring(1) +ending);

            //this is how it chooses which list to load
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
                    userList.check = controller.checkNumRows("users");
                    if(userList.check == 0){
                        new getUsers().execute();
                    }
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
                    TAG_USERNAME, TAG_STATUS, TAG_DESCRIPTION, TAG_SYNC},
                    new int[] { R.id.IDMain,
                            R.id.userMain, R.id.statusMain, R.id.descMain, R.id.syncStatus });
            setListAdapter(adapter);
            lv = (ListView)findViewById(android.R.id.list);
            lv.setAdapter(adapter);
            onListItemClick();

        }

        //To show that SQLite DB is not empty
        checker = controller.checkNumRows("cases");

        //To get the assignee id for adding case
        if(controller.getTableValues("cases", 6).size() == 0 && controller.getTableValues("cases", 0).size() == 0) {
            sharedPreference.setString(MainActivity.this, TAG_ID, "0");
        }
        else if(controller.getTableValues("cases", 6).size() != 0 && controller.getTableValues("cases", 0).size() != 0){
            sharedPreference.setString(MainActivity.this, TAG_ID, controller.getMaxId("cases"));
            sharedPreference.setString(MainActivity.this, TAG_LOGIN_ID, controller.getTableValues("cases", 6).get(0));
        }
        //controller.close();
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
        //this is so that the list doesn't stack:
        mainAdapter.notifyDataSetChanged();
        mainAdapter.clear();
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
                //Check if it needs to be synced or not so as not to update a case that hasn't been added yet
                intent.putExtra("caller", "editCase");
                Bundle bundle = new Bundle();
                bundle.putString(TAG_ID, controller.getTableValues("cases", 0).get(position));
                bundle.putString(TAG_STATUS, controller.getTableValues("cases", 2).get(position));
                bundle.putString(TAG_USERNAME, controller.getTableValues("cases", 3).get(position));
                bundle.putString(TAG_DESCRIPTION, controller.getTableValues("cases", 4).get(position));
                bundle.putString(TAG_ACTION_TAKEN, controller.getTableValues("cases", 5).get(position));
                bundle.putString(TAG_LOGIN_ID, controller.getTableValues("cases", 6).get(position));
                bundle.putString(TAG_STATUS_ID, controller.getTableValues("cases", 7).get(position));
                bundle.putString(TAG_SYNC, controller.getTableValues("cases", 8).get(position));
                intent.putExtras(bundle);

                startActivity(intent);
                //controller.close();

            }
        });

    }




    //--------------INITIALISE AND CHECKS---------------------------------------------------------------------
    //SMALL BUT USEFUL METHODS:
    //initialises variables and such:
    private void initialise(){
        stat = (TextView) findViewById(R.id.statusMain);
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

    //to disable the back button
    @Override
    public void onBackPressed(){
        MainActivity.this.finish();
        System.exit(0);
    }


    /*-------------------------------------ADAPTER FOR LISTVIEW--------------------------------------------------------------*/
    //CASE LIST ADAPTER
    private class CaseListAdapter extends ArrayAdapter {
        public CaseListAdapter() {
            super (MainActivity.this, R.layout.listview_item, android.R.id.list);
        }
        @Override
         public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (position % 2 == 1) {
                view.setBackgroundColor(Color.parseColor("#990000"));
            } else {
                view.setBackgroundColor(Color.CYAN);
            }

            return view;
        }
    }

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
            ArrayAdapter<String> adapt = new CaseListAdapter();
            adapt.notifyDataSetChanged();
            mCasesLV.setAdapter(adapt);
            return mCasesLV;
        }
        return mCasesLV;
    }
    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
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
                while(json == null && isNetworkConnected()){
                    try{
                        Thread.sleep(20);
                        json = jsonParser.makeHttpRequest(CASE_URL, "GET", parameters);
                        if(json == null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this,"Error check your internet connection", Toast.LENGTH_SHORT).show();
                                    MainActivity.this.finish();
                                }
                            });
                        }else
                            break;
                    }catch(InterruptedException e){}
                }
                //check log cat for JSON response
                Log.d("Cases: ", json.toString());
                //Check for SUCCESS TAG
                success = json.getInt(TAG_SUCCESS);

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
                        String actionTaken = c.getString(TAG_ACTION_TAKEN);
                        String login_id = c.getString(TAG_LOGIN_ID);
                        String status_id = c.getString(TAG_STATUS_ID);
                        String sync = "";
                        //create a new HashMap
                        HashMap<String, String> map = new HashMap<String, String>();

                        //add each child node to HashMap Key => value
                        map.put(TAG_ID, id);
                        map.put(TAG_USERNAME, user);
                        map.put(TAG_DESCRIPTION, desc);
                        map.put(TAG_ASSIGNEE, assignee);
                        map.put(TAG_STATUS, status);
                        map.put(TAG_ACTION_TAKEN, actionTaken);
                        map.put(TAG_LOGIN_ID, login_id);
                        map.put(TAG_STATUS_ID, status_id);
                        map.put(TAG_SYNC, sync);
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
            pDialog.dismiss();
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
                                        TAG_USERNAME, TAG_STATUS, TAG_DESCRIPTION, TAG_SYNC},
                                new int[] { R.id.IDMain,
                                        R.id.userMain, R.id.statusMain, R.id.descMain, R.id.syncStatus });
                        // updating listView
                        setListAdapter(adapter);
                    }
                });
                checker = controller.checkNumRows("cases");
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
        }

    }

    /*-------------------------------------#SYNC--------------------------------------------*/

    class syncDB extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            switch(sync.trim()){
                case "10":{
                    SYNC_URL = AddCase.ADD_CASE_URL;

                    //Building Parameters
                    parameters.add(new BasicNameValuePair("name", nameId));
                    parameters.add(new BasicNameValuePair("description", mDescription));
                    parameters.add(new BasicNameValuePair("actiontaken", actionT));
                    parameters.add(new BasicNameValuePair("assignee", assigneeID));
                    parameters.add(new BasicNameValuePair("status", statusID));

                    break;
                }
                case "20":{
                    SYNC_URL = AddCase.UPDATE_CASE_URL;

                    //Building Parameters
                    parameters.add(new BasicNameValuePair("id", caseID));
                    parameters.add(new BasicNameValuePair("user_id", nameId));
                    parameters.add(new BasicNameValuePair("description", mDescription));
                    parameters.add(new BasicNameValuePair("actiontaken", actionT));
                    parameters.add(new BasicNameValuePair("status", statusID));

                    break;
                }
                case "":{break;}
            }

            try {
                Log.d("request!", "starting");

                JSONObject json = jsonParser.makeHttpRequest(
                        SYNC_URL, "POST", parameters);

                //check log cat for JSON response
                Log.d("Inserting... ", json.toString());

                //Check for SUCCESS TAG
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    //check log cat for JSON response
                    Log.d("Successfully Synced Case: ", json.toString());

                    reloadActivity();
                    return json.getString(TAG_MESSAGE);
                } else {
                    return json.getString(TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
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
            intent.putExtra("caller", "addCase");
            startActivity(intent);
            return true;
        }
        else if(id == R.id.synchronise){
            ArrayList<String> ay = controller.getTableValues("cases", 8);
            //"10" is for add case and "20" is for update case:
            if(isNetworkConnected() && (ay.contains("10") || ay.contains("20"))){
                for (int i = ay.size()-1; i >= 0; i--) {
                    if (!ay.get(i).toString().equals("")) {
                        Toast.makeText(context, "Sync Databases IN", Toast.LENGTH_SHORT).show();
                        caseID = controller.getTableValues("cases", 0).get(i);
                        username = controller.getTableValues("cases", 3).get(i);
                        mDescription = controller.getTableValues("cases", 4).get(i);
                        actionT = controller.getTableValues("cases", 5).get(i);
                        assigneeID = controller.getTableValues("cases", 6).get(i);
                        statusID = controller.getTableValues("cases", 7).get(i);
                        sync = controller.getTableValues("cases", 8).get(i);

                        nameId = controller.getID("users", "userId", username, TAG_NAME, 0);

                        controller.updateSyncValue("cases", caseID, "", TAG_SYNC);
                        new syncDB().execute();
                    }
                }
                //Drop old table:
                controller.refreshCases("cases");
                //refresh and refill SQLite Database
                new getCases().execute();
                loadList();
                getSQLiteList();
                refreshAtTop();
                Toast.makeText(context, "Databases Synced", Toast.LENGTH_SHORT).show();
            }else if(!isNetworkConnected()){
                Toast.makeText(context, "Not Connected to the internet", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, "Databases are in Sync", Toast.LENGTH_SHORT).show();
            }
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
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MainActivity.this.finishAffinity();
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //gets Users from MySQL DB
    class getUsers extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            pDialog.setMessage("Updating Users List \nPlease Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{
                //get JSON string from URL
                JSONObject jsonUse = jsonParser.makeHttpRequest(userList.USER_URL, "GET", parameters);
                if(isNetworkConnected() == true) {
                    while(jsonUse == null){
                        try{
                            Thread.sleep(20);
                            jsonUse = jsonParser.makeHttpRequest(userList.USER_URL, "GET", parameters);
                            if(jsonUse == null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this,"Error connecting to the Database \n Check your internet connection", Toast.LENGTH_SHORT).show();
                                        MainActivity.this.finish();
                                    }
                                });
                            }else
                                break;
                        }catch(InterruptedException e){}
                    }

                    //check log cat for JSON response
                    Log.d("Users: ", jsonUse.toString());
                    //Check for SUCCESS TAG
                    success = jsonUse.getInt(MainActivity.TAG_SUCCESS);
                }
                if(success == 1) {
                    //users found, get array of users
                    userList.users = jsonUse.getJSONArray(MainActivity.TAG_USERS);

                    //Loop through all the users
                    for(int i =0; i < userList.users.length(); i++){
                        JSONObject u = userList.users.getJSONObject(i);

                        String id = u.getString(MainActivity.TAG_USERID);
                        String name = u.getString(MainActivity.TAG_NAME);
                        String company = u.getString(MainActivity.TAG_COMPANY);
                        String email = u.getString(MainActivity.TAG_EMAIL);
                        String telephone = u.getString(MainActivity.TAG_TELEPHONE);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(MainActivity.TAG_USERID, id);
                        maps.put(MainActivity.TAG_NAME, name);
                        maps.put(MainActivity.TAG_COMPANY, company);
                        maps.put(MainActivity.TAG_EMAIL, email);
                        maps.put(MainActivity.TAG_TELEPHONE, telephone);

                        //add this map to SQLite too
                        controller.insertUser(maps);
                    }

                    return jsonUse.getString(MainActivity.TAG_MESSAGE);
                }
                else {
                    return jsonUse.getString(MainActivity.TAG_MESSAGE);
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result){
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            if (result != null) {
                //MainActivity.this.setProgressBarIndeterminateVisibility(false);
                userList.check = controller.checkNumRows("users");
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
}