package com.mycompany.CMSBHelpdesk;

/**
 * @author Abel Hii 2015
 *
 * SQLite Tutorial from: http://programmerguru.com/android-tutorial/how-to-sync-remote-mysql-db-to-sqlite-on-android/
 *
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.internetCheck;
import com.mycompany.CMSBHelpdesk.helpers.sharedPreference;
import com.mycompany.CMSBHelpdesk.objects.Case;

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
    private SwipeRefreshLayout swipeLayout;
    private CaseListAdapter adapter;
    public static int checker;
    public static String checkLog;
    ArrayList<HashMap<String,String>> casemap;
    ArrayList<String> syncColumn;
    //Variables for sync
    private String nameId = "", caseID ="", username="", mDescription="",actionT="", assigneeID="", statusID="", sync="";

    // DB Class to perform DB related operations
    DBController controller = new DBController(this);

    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<HashMap<String, String>> casesList;
    // cases JSONArray
    JSONArray cases = null;

    private ProgressDialog pDialog;
    public static String TAG_IP = "";
    private static String CASE_URL = "";
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

    public static String colorAB = "#FEA000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(colorAB)));

        checkLog = sharedPreference.getString(this, "login");
        TAG_IP = sharedPreference.getString(this, "ip");
        String ending = "'s Cases";


        //check if user was logged in before:
        //if not go to login page, else continue.
        String checkPass = sharedPreference.getString(this, "pass");
        String oneTimeSetup = sharedPreference.getString(this, "oneTS");
        if (oneTimeSetup.equals("")) {
            Intent intentOneTime = new Intent(MainActivity.this, SetupActivity.class);
            startActivity(intentOneTime);
            this.finish();
        }
        else if (checkLog.equals("") || checkPass.equals("")) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            this.finish();
        } else {
            CASE_URL = "http://" + TAG_IP + "/chd/public/app/getCases.php";
            if(checkLog.equalsIgnoreCase("admin")){
                CASE_URL = "http://" + TAG_IP + "/chd/public/app/getCasesAdmin.php";
                setTitle("Welcome Admin");
            }else{
                //This just sets the title for MainActivity and for people who's names ends with 's'
                if (checkLog.substring(checkLog.length() - 1).equals("s")) {
                    ending = "' Cases";
                }
                setTitle(checkLog.substring(0, 1).toUpperCase() + checkLog.substring(1) + ending);
            }
            initialise();
            getListView();

            //this is how it chooses which list to load
            Settings.newLogin = sharedPreference.getInt(MainActivity.this, "log");
            checker = controller.checkNumRows("cases");
            if (checker == 0 || checker < 0 || Settings.newLogin == 100) {
                //check if connected to internet or not
                if (internetCheck.connectionCheck(MainActivity.this)) {
                    new getCases().execute();
                    pDialog.dismiss();
                    onListItemClick();
                    refreshAtTop();
                    //Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
                    userList.check = controller.checkNumRows("users");
                    if (userList.check == 0) {
                        //Toast.makeText(this, "1.5", Toast.LENGTH_SHORT).show();
                        new getUsers().execute();
                    }
                    sharedPreference.setInt(this, "log", 0);
                }else {
                    swipeLayout.setEnabled(false);
                    //Retrieve previously saved data
                    Toast.makeText(this, "No Internet Connection!" +
                                    "\nPlease Connect to the internet and restart the app",
                            Toast.LENGTH_LONG).show();
                }
            } else if (internetCheck.isNetworkConnected(this) && checker > 0) {
                getSQLiteList();
                refreshAtTop();
                //Toast.makeText(this, "2", Toast.LENGTH_SHORT).show();
            } else if (!internetCheck.isNetworkConnected(this)) {
                swipeLayout.setEnabled(false);
                getSQLiteList();
                //Toast.makeText(this, "3", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void getSQLiteList(){
        // Get Cases records from SQLite DB
        ArrayList<HashMap<String, String>> listCase = controller.getAllCases();
        ArrayList<Case> c = new ArrayList<Case>();
        // If users exists in SQLite DB
        if (listCase.size() != 0) {
            for(int i = 0; i<listCase.size(); i++) {
                c.add(new Case(listCase.get(i).get(TAG_ID),
                        listCase.get(i).get(TAG_USERNAME),
                        listCase.get(i).get(TAG_DESCRIPTION),
                        listCase.get(i).get(TAG_STATUS),
                        listCase.get(i).get(TAG_SYNC)));
            }
            adapter.addAll(c);
            setListAdapter(adapter);
            //lv = (ListView)findViewById(android.R.id.list);
            //mCasesLV.setAdapter(adapter);
            onListItemClick();
        }

        //To show that SQLite DB is not empty
        checker = controller.checkNumRows("cases");
    }

    //--------------REFRESH SPINNER-----------------------------------------------------------------------------
    //Functionality for swipe to refresh
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                casemap = controller.getAllCases();
                syncColumn = returnColumn(casemap);
                if(internetCheck.connectionCheck(MainActivity.this) && (syncColumn.contains("10") || syncColumn.contains("20"))) {
                    Toast.makeText(MainActivity.this, "There are cases that need to be synced", Toast.LENGTH_LONG).show();

                    swipeLayout.setRefreshing(false);
                }else {
                    //Drop old table:
                    controller.refreshCases("cases");
                    //refreshesList
                    refreshList();
                    //refresh and refill SQLite Database
                    new getCases().execute();
                    onListItemClick();
                    refreshAtTop();

                    swipeLayout.setRefreshing(false);
                }
            }
        }, 4500);
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

    //This is to make sure that the list updates properly instead of stacking ontop of the old one
    public void refreshList(){
        if(adapter!=null && casesList!=null) {
            casesList.clear();
            adapter.clear();
        }
    }



    //--------------DISPLAY LIST!-----------------------------------------------------------------------------
    public void onListItemClick(){
        //when list item is clicked go to add case to edit that item
        //and send data to add case using bundles
        mCasesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position,
                                    long arg3) {


                Intent intent = new Intent(MainActivity.this, AddCase.class);
                //Check if it needs to be synced or not so as not to update a case that hasn't been added yet
                intent.putExtra("caller", "editCase");
                Bundle bundle = new Bundle();
                bundle.putString(TAG_ID, controller.getTableValues("cases", 0).get(position));
                bundle.putString(TAG_ASSIGNEE, controller.getTableValues("cases", 1).get(position));
                bundle.putString(TAG_STATUS, controller.getTableValues("cases", 2).get(position));
                bundle.putString(TAG_USERNAME, controller.getTableValues("cases", 3).get(position));
                bundle.putString(TAG_DESCRIPTION, controller.getTableValues("cases", 4).get(position));
                bundle.putString(TAG_ACTION_TAKEN, controller.getTableValues("cases", 5).get(position));
                bundle.putString(TAG_LOGIN_ID, controller.getTableValues("cases", 6).get(position));
                bundle.putString(TAG_STATUS_ID, controller.getTableValues("cases", 7).get(position));
                bundle.putString(TAG_SYNC, controller.getTableValues("cases", 8).get(position));
                intent.putExtras(bundle);

                startActivity(intent);

                //Animation that slides to next activity
                overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
            }
        });
    }




    //--------------INITIALISE AND CHECKS---------------------------------------------------------------------
    //SMALL BUT USEFUL METHODS:
    //initialises variables and such:
    private void initialise(){
        ArrayList<Case> emptyC = new ArrayList<>();
        adapter = new CaseListAdapter(MainActivity.this, R.id.list_item, emptyC);

        casesList = new ArrayList<HashMap<String, String>>();

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.black);
    }

    //to disable the back button
    @Override
    public void onBackPressed(){
        MainActivity.this.finish();
        System.exit(0);
    }


    /*-------------------------------------ADAPTER FOR LISTVIEW--------------------------------------------------------------*/
    //CASE LIST ADAPTER
    private class CaseListAdapter extends ArrayAdapter<Case>{
        private Context context;
        private ArrayList<Case> items;
        public CaseListAdapter(Context context, int listItemId, ArrayList<Case> data) {
            super (MainActivity.this, listItemId, data);
            this.context = context;
            this.items = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = convertView;
            if(view == null){
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                view = vi.inflate(R.layout.list_item, null);
            }

            Case c = items.get(position);
            if(c != null){
                TextView mId = (TextView) view.findViewById(R.id.IDMain);
                TextView mUser = (TextView) view.findViewById(R.id.userMain);
                TextView mDesc = (TextView) view.findViewById(R.id.descMain);
                TextView mStatus = (TextView) view.findViewById(R.id.statusMain);
                TextView mSync = (TextView) view.findViewById(R.id.syncStatus);
                if(mId != null){mId.setText(c.getID());}
                if(mUser != null){mUser.setText(c.getUsername());}
                if(mDesc != null){mDesc.setText(c.getDescription());}
                if(mStatus != null){
                    mStatus.setText(c.getStatus());
                    switch(c.getStatus()){
                        case "Not Started":
                            mStatus.setTextColor(Color.parseColor("#cc0000"));
                            break;
                        case "In Progress":
                            mStatus.setTextColor(Color.parseColor("#12af83"));
                            break;
                        case "Waiting for Vendor":
                            mStatus.setTextColor(Color.parseColor("#ddbb00"));
                            break;
                        case "Differed":
                            mStatus.setTextColor(Color.parseColor("#af1283"));
                            break;
                        case "Pending Close":
                            mStatus.setTextColor(Color.parseColor("#1abef9"));
                            break;
                        case "Closed":
                            mStatus.setTextColor(Color.parseColor("#5a5a5a"));
                            break;
                    }
                }
                if(mSync != null){mSync.setText(c.getSync());}
            }


            if(position % 2 == 0){
                view.setBackgroundResource(R.drawable.mycolors);
            }else {
                view.setBackgroundResource(R.drawable.mycolors2);
            }
            return view;
        }
    }

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
            return mCasesLV;
        }
        return mCasesLV;
    }
    public void setListAdapter(CaseListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
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
                while(json == null && internetCheck.connectionCheck(MainActivity.this)){
                    try{
                        Thread.sleep(20);
                        json = jsonParser.makeHttpRequest(CASE_URL, "GET", parameters);
                        if(json == null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this,"Error check your internet connection", Toast.LENGTH_SHORT).show();
                                    finish();
                                    System.exit(0);
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

        @Override
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
                        ArrayList<Case> c = new ArrayList<Case>();
                        for(int i = 0; i<casesList.size(); i++) {
                            c.add(new Case(casesList.get(i).get(TAG_ID),
                                    casesList.get(i).get(TAG_USERNAME),
                                    casesList.get(i).get(TAG_DESCRIPTION),
                                    casesList.get(i).get(TAG_STATUS),
                                    casesList.get(i).get(TAG_SYNC)));
                        }
                        adapter.addAll(c);
                        setListAdapter(adapter);
                    }
                });
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
        }

    }

    /*-------------------------------------#SYNC--------------------------------------------*/

    int count = 0;
    class syncDB extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for (int i = syncColumn.size()-1; i >= 0; --i) {
                if (!syncColumn.get(i).equalsIgnoreCase("") && ((syncColumn.contains("10") || syncColumn.contains("20")))) {
                    caseID = casemap.get(i).get(TAG_ID);
                    username = casemap.get(i).get(TAG_USERNAME);
                    mDescription = casemap.get(i).get(TAG_DESCRIPTION);
                    actionT = casemap.get(i).get(TAG_ACTION_TAKEN);
                    assigneeID = casemap.get(i).get(TAG_LOGIN_ID);
                    statusID = casemap.get(i).get(TAG_STATUS_ID);
                    sync = String.valueOf(syncColumn.get(i));

                    nameId = controller.getID("users", "userId", username, TAG_NAME, 0);

                    if(sync.equals("10")){
                        SYNC_URL = AddCase.ADD_CASE_URL;

                        //Building Parameters
                        parameters.add(new BasicNameValuePair("name", nameId));
                        parameters.add(new BasicNameValuePair("description", mDescription));
                        parameters.add(new BasicNameValuePair("actiontaken", actionT));
                        parameters.add(new BasicNameValuePair("assignee", assigneeID));
                        parameters.add(new BasicNameValuePair("status", statusID));

                        jsonParser.makeHttpRequest(
                                SYNC_URL, "POST", parameters);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Add Sync "+ count++, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else if(sync.equals("20")){
                        SYNC_URL = AddCase.UPDATE_CASE_URL;

                        //Building Parameters
                        parameters.add(new BasicNameValuePair("id", caseID));
                        parameters.add(new BasicNameValuePair("user_id", nameId));
                        parameters.add(new BasicNameValuePair("description", mDescription));
                        parameters.add(new BasicNameValuePair("actiontaken", actionT));
                        parameters.add(new BasicNameValuePair("status", statusID));

                        jsonParser.makeHttpRequest(
                                SYNC_URL, "POST", parameters);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Update Sync "+ count++, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            }
            return null;
        }

        @Override
        public void onPostExecute(String result){
            controller.refreshCases("cases");
            refreshList();
            new getCases().execute();
            refreshAtTop();
            Toast.makeText(MainActivity.this, "Databases Synced", Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<String> returnColumn(ArrayList<HashMap<String,String>> map){
        ArrayList<String> ay = new ArrayList<String>();
        for(int i = 0; i<map.size(); i++){
            ay.add(map.get(i).get(TAG_SYNC));
        }
        return ay;
    }
    //SYNC METHOD
    public void synchronise(){
        casemap = controller.getAllCases();
        syncColumn = returnColumn(casemap);
        //"10" is for add case and "20" is for update case:
        if(internetCheck.isNetworkConnected(MainActivity.this) && (syncColumn.contains("10") || syncColumn.contains("20"))){
            if(internetCheck.connectionCheck(MainActivity.this))
            new syncDB().execute();
        }else if(!(syncColumn.contains("10") || syncColumn.contains("20"))){
            Toast.makeText(this, "Databases are in Sync", Toast.LENGTH_SHORT).show();
        }else if(!internetCheck.connectionCheck(MainActivity.this)){
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
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
            //To set the case id for adding case if cases are empty
            if(controller.getTableValues("cases", 0).isEmpty())
                sharedPreference.setString(MainActivity.this, TAG_ID, "0");
            else if(controller.getTableValues("cases", 0) != null)
                sharedPreference.setString(MainActivity.this, TAG_ID, controller.getMaxId("cases"));
            Intent intent = new Intent(context, AddCase.class);
            intent.putExtra("caller", "addCase");
            startActivity(intent);
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
            return true;
        }
        else if(id == R.id.synchronise){
            synchronise();
            return true;
        }
        else if(id == R.id.action_settings){
            Intent intent = new Intent(context, Settings.class);
            startActivity(intent);
            overridePendingTransition(R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom);
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
                JSONObject jsonUse = jsonParser.makeHttpRequest(userList.USER_URL, "GET", parameters);
                if(internetCheck.isNetworkConnected(MainActivity.this) == true) {
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

        @Override
        protected void onPostExecute(String result){
            // dismiss the dialog after getting all users
            pDialog.dismiss();
        }
    }
}