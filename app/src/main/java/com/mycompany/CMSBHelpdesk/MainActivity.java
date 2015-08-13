package com.mycompany.CMSBHelpdesk;

/**
 * @author Abel Hii 2015
 *
 * SQLite Tutorial from: http://programmerguru.com/android-tutorial/how-to-sync-remote-mysql-db-to-sqlite-on-android/
 *
 * Note: Everything from AsyncMethods class and anything using AsyncTask
 *       runs on a seperate thread from the main (runs in the background, while the main thread continues in the foreground)
 *       So if you want to get a return value or something from one of those methods you need to wait for the task to complete
 *       and get it in the "onPostExecute" method.
 *       OR
 *       you can get the return value directly from the execute (if its small) by doing something like:
 *       AsyncMethod.example().execute().get();
 *
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import com.mycompany.CMSBHelpdesk.helpers.AsyncMethods;
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
import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener{

    private ListView mCasesLV;
    private SwipeRefreshLayout swipeLayout;
    private CaseListAdapter adapter;
    public static int checker;
    public static String checkLog;
    ArrayList<HashMap<String,String>> casemap;
    ArrayList<String> syncColumn;
    //Variables for sync
    private String nameId = "", caseID ="", username="", mDescription="",actionT="", assigneeID="", statusID="", sync="";
    public static String caseid = "0";

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
    public static String ADD_CASE_URL = "";
    public static String UPDATE_CASE_URL = "";
    public static String INSERT_IMAGE_URL = "";
    public static String CHECK_IMAGE_URL = "";
    public static String USER_URL = "";
    public static String ADD_NEW_USER_URL = "";
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_CASES = "caseslist";
    public static final String TAG_USERS = "userslist";
    public static final String TAG_DIVISIONS = "divisionList";

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
    public static final String TAG_IMAGE = "image";

    //user stuff
    public static final String TAG_USERID = "userId";
    public static final String TAG_NAME = "name";
    public static final String TAG_COMPANY = "company";
    public static final String TAG_EMAIL = "email";
    public static final String TAG_TELEPHONE = "telephone";
    public static final String TAG_DIVISION_ID = "division_id";

    //Company stuff
    public static final String TAG_COMPANY_ID = "companyId";
    public static final String TAG_COMPANY_NAME = "companyName";
    public static final String TAG_ENABLED = "enabled";

    public static String colorAB = "#FEA000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar bar = getSupportActionBar();
        getSupportActionBar().setIcon(R.drawable.cms_logo);
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(colorAB)));

        checkLog = sharedPreference.getString(this, "login").trim();
        TAG_IP = sharedPreference.getString(this, "ip").trim();
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
            CASE_URL = "http://" + TAG_IP + "/public/app/getCases.php";
            if(checkLog.equalsIgnoreCase("admin")){
                CASE_URL = "http://" + TAG_IP + "/public/app/getCasesAdmin.php";
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
                    userList.check = controller.checkNumRows("users");
                    if (userList.check == 0) {
                        new AsyncMethods.getUsers(MainActivity.this, jsonParser, pDialog, controller).execute();
                    }
                    sharedPreference.setInt(this, "log", 0);
                }else {
                    swipeLayout.setEnabled(false);
                    Toast.makeText(this, "No Internet Connection!" +
                                    "\nPlease Connect to the internet and restart the app",
                            Toast.LENGTH_LONG).show();
                }
            //Retrieve previously saved data from SQLite
            } else if (internetCheck.isNetworkConnected(this) && checker > 0) {
                getSQLiteList();
                refreshAtTop();
            } else if (!internetCheck.isNetworkConnected(this)) {
                swipeLayout.setEnabled(false);
                getSQLiteList();
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
                caseid="0";
                casemap = controller.getAllCases();
                syncColumn = returnColumn(casemap);
                if(internetCheck.isNetworkConnected(MainActivity.this) && (syncColumn.contains("10") || syncColumn.contains("20"))) {
                    Toast.makeText(getApplicationContext(), "There are cases that need to be synced", Toast.LENGTH_LONG).show();

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
                intent.putExtra("caller", "editcase");
                Bundle bundle = new Bundle();
                String idd = controller.getTableValues("cases", 0).get(position);
                bundle.putString(TAG_ID, idd);
                bundle.putString(TAG_ASSIGNEE, controller.getTableValues("cases", 1).get(position));
                bundle.putString(TAG_STATUS, controller.getTableValues("cases", 2).get(position));
                bundle.putString(TAG_USERNAME, controller.getTableValues("cases", 3).get(position));
                bundle.putString(TAG_DESCRIPTION, controller.getTableValues("cases", 4).get(position));
                bundle.putString(TAG_ACTION_TAKEN, controller.getTableValues("cases", 5).get(position));
                bundle.putString(TAG_LOGIN_ID, controller.getTableValues("cases", 6).get(position));
                bundle.putString(TAG_STATUS_ID, controller.getTableValues("cases", 7).get(position));
                bundle.putString(TAG_SYNC, controller.getTableValues("cases", 8).get(position));
                bundle.putString(TAG_IMAGE, controller.getTableValues("cases", 9).get(position));
                intent.putExtras(bundle);

                //sets the bitmap image if image exists in sqlite
                AddPicture.filePathTemp = controller.getValue("cases", MainActivity.TAG_IMAGE, idd);

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
        //initialise the urls here because you need to get TAG_IP first:
        ADD_CASE_URL = "http://"+ MainActivity.TAG_IP +"/public/app/AddCase.php";
        UPDATE_CASE_URL = "http://"+ MainActivity.TAG_IP +"/public/app/UpdateCase.php";
        INSERT_IMAGE_URL = "http://"+ MainActivity.TAG_IP +"/public/app/insertImage.php";
        CHECK_IMAGE_URL = "http://"+ MainActivity.TAG_IP +"/public/app/checkImages.php";
        USER_URL = "http://"+MainActivity.TAG_IP+"/public/app/getUsers.php";
        ADD_NEW_USER_URL = "http://"+MainActivity.TAG_IP+"/public/app/addNewUser.php";

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
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
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
                                    Toast.makeText(getApplicationContext(),"Error check your internet connection", Toast.LENGTH_SHORT).show();
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
    public static int counter;
    class syncDB extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            //syncColumn is the sync column in the cases table from the SQLite
            counter = syncColumn.size()-1;
            while(counter >= 0){
                if (!syncColumn.get(counter).equalsIgnoreCase("") && ((syncColumn.contains("10") || syncColumn.contains("20")))) {
                    caseID = casemap.get(counter).get(TAG_ID);
                    username = casemap.get(counter).get(TAG_USERNAME);
                    mDescription = casemap.get(counter).get(TAG_DESCRIPTION);
                    actionT = casemap.get(counter).get(TAG_ACTION_TAKEN);
                    assigneeID = casemap.get(counter).get(TAG_LOGIN_ID);
                    statusID = casemap.get(counter).get(TAG_STATUS_ID);
                    sync = String.valueOf(syncColumn.get(counter));
                    JSONObject json;

                    //tempCaseID is for getting the correct id for where the image is stored in SQLite
                    //Because AddCase is going to change caseID to the max id and the image is stored in the old caseID
                    String tempCaseID = caseID;
                    nameId = controller.getID("users", "userId", username, TAG_NAME);

                    if(sync.equals("10")){
                        SYNC_URL = ADD_CASE_URL;

                        //Building Parameters
                        parameters.add(new BasicNameValuePair("name", nameId));
                        parameters.add(new BasicNameValuePair("description", mDescription));
                        parameters.add(new BasicNameValuePair("actiontaken", actionT));
                        parameters.add(new BasicNameValuePair("assignee", assigneeID));
                        parameters.add(new BasicNameValuePair("status", statusID));

                        json = jsonParser.makeHttpRequest(
                                SYNC_URL, "POST", parameters);
                        try {
                            success = json.getInt(TAG_SUCCESS);
                            if(success == 1) {
                                caseID = json.getString("newID");
                            }
                            count++;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(sync.equals("20")){
                        SYNC_URL = UPDATE_CASE_URL;

                        //Building Parameters
                        parameters.add(new BasicNameValuePair("id", caseID));
                        parameters.add(new BasicNameValuePair("user_id", nameId));
                        parameters.add(new BasicNameValuePair("description", mDescription));
                        parameters.add(new BasicNameValuePair("actiontaken", actionT));
                        parameters.add(new BasicNameValuePair("status", statusID));

                        jsonParser.makeHttpRequest(
                                SYNC_URL, "POST", parameters);
                        count++;
                    }


                    //To sync images if images exists:
                    String image = null;
                    image = controller.getValue("cases", "image", tempCaseID);
                    if(image != null){
                        ArrayList<String> paths = AddPicture.getFilePaths(image, "imageArrayAdd");
                        AddCase.uploadImage ui = new AddCase.uploadImage(null, caseID);
                        ui.execute(paths);
                        ui.onPostExecute(counter--);
                    }else{
                        --counter;
                    }
                }else{
                    --counter;
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
            Toast.makeText(getApplicationContext(), "Synced "+ count +" Cases", Toast.LENGTH_SHORT).show();
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
        //"10" is for add case and "20" is for update case:
        if(internetCheck.isNetworkConnected(MainActivity.this) && (syncColumn.contains("10") || syncColumn.contains("20"))){
            if(internetCheck.connectionCheck(MainActivity.this))
            new syncDB().execute();
        }else if(!(syncColumn.contains("10") || syncColumn.contains("20"))){
            Toast.makeText(getApplicationContext(), "Databases are in Sync", Toast.LENGTH_SHORT).show();
        }else if(!internetCheck.connectionCheck(MainActivity.this)){
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(context, userList.class);
            intent.putExtra("caller", "addcase");
            startActivity(intent);
            //Animation that slides to next activity
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
            return true;
        }
        else if(id == R.id.synchronise){
            casemap = controller.getAllCases();
            syncColumn = returnColumn(casemap);
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

  }