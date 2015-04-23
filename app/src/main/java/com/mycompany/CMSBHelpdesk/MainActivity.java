package com.mycompany.CMSBHelpdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

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

    //store list:
    private File mainList;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    //DB stuff:
    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    ArrayList<HashMap<String, String>> casesList;
    // cases JSONArray
    JSONArray cases = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //To exit the app if no internet connection at login:
        if (getIntent().getBooleanExtra("EXIT", false)) {
            this.finish();
        }

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
            if(isNetworkConnected() == true) {
                new getCases().execute();
                //onRefresh();
                //getListView
                ListView lv = getListView();
                ArrayAdapter<HashMap<String, String>> mainAdapter = new ArrayAdapter<HashMap<String, String>>(this, android.R.layout.simple_list_item_1, casesList);
                lv.setAdapter(mainAdapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view, int position,
                                            long arg3) {

                    }
                });



                //Swipe to reload functionality:
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
            else{
                //Retrieve previously saved data
                swipeLayout.setEnabled(false);
                ListView lv = getListView();
                try {
                    lv = (ListView) inputStream.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //registerForContextMenu(getListView());
                //retrieve();
                //addingCase(getId, getDesc, getUser, getAssignees, getStatus);
            }
        }

    }

    //Check if network is connected
    public boolean isNetworkConnected(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }

    private void initialise(){
        mainList = new File(getDir("data", MODE_PRIVATE), "map");
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(mainList));
            inputStream = new ObjectInputStream(new FileInputStream(mainList));
        } catch (IOException e) {
            e.printStackTrace();
        }

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }
    //Functionality for swipe to refresh
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
                new getCases().execute();
            }
        }, 5000);
    }

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

    public ListView getListView() {
        if(mCasesLV == null){
            mCasesLV = (ListView)findViewById(android.R.id.list);
            ArrayAdapter<String> adapter = new CaseListAdapter();
            mCasesLV.setAdapter(adapter);
        }
        return mCasesLV;
    }

    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }

    //CASE LIST ADAPTER
    private class CaseListAdapter extends ArrayAdapter {

        public CaseListAdapter() {
            super (MainActivity.this, R.layout.listview_item, android.R.id.list);
        }
    //Trying to alternate the row colours in the listview
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            if (position % 2 == 1) {
                view.setBackgroundColor(Color.BLUE);
            } else {
                view.setBackgroundColor(Color.CYAN);
            }

/**
            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.listview_item, parent, false);
            }
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
            stat.setText(currentCase.getStatus());*/

            return view;
        }
    }

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
                        //to save the data
                        try {
                            outputStream.writeObject(map);
                            outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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

    //This stuff is just the action bar for like settings and stuff
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

}