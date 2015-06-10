package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class userList extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {

    private ListView userLv;
    SimpleAdapter mAdapter;
    ArrayAdapter<String> adapt;
    EditText inputSearch;

    private SwipeRefreshLayout swipeLayout;

    // ArrayList for Listview
    ArrayList<HashMap<String, String>> useList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> listUser;

    //DB variables:
    DBController controlUser = new DBController(this);
    private ProgressDialog pDialog;
    public static final String USER_URL = "http://"+MainActivity.TAG_IP+"/chd/public/app/getUsers.php";
    public static int check = 0;
    JSONParser jsonParser = new JSONParser();
    public static JSONArray users = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        //Default Back Button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Choose User");

        initialise();
        //This is the process which loads the spinner depending on the situation
        if (check == 0 || check < 0) {
            //check if connected to internet or not
            if (isNetworkConnected()) {
                new getUsers().execute();
                //Loads the list from MySQL DB
                getSQLiteUsers();
                refreshAtTop();
                Toast.makeText(userList.this, "4", Toast.LENGTH_SHORT).show();
            } else {
                //Retrieve previously saved data
                Toast.makeText(userList.this, "User List Empty and No Internet Connection!" +
                                " \n Please Connect to the internet and refresh the app",
                        Toast.LENGTH_LONG).show();
            }
        } else if (isNetworkConnected() && check > 0) {
            getSQLiteUsers();
            refreshAtTop();
            Toast.makeText(userList.this, "5", Toast.LENGTH_SHORT).show();
        } else if (!isNetworkConnected()) {
            swipeLayout.setEnabled(false);
            getSQLiteUsers();
            Toast.makeText(userList.this, "6", Toast.LENGTH_SHORT).show();
        }

        onListItemClick();
    }

    /*---------------------------------------INITIALISE AND CHECKS----------------------------------------------------------------*/
    public void initialise(){
        useList = new ArrayList<HashMap<String,String>>();
        check = controlUser.checkNumRows("users");

        //Does the searching/filtering
        inputSearch = (EditText) findViewById(R.id.inputSearch);
        getText();
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                userList.this.mAdapter.getFilter().filter(cs);
                //userLv.setTextFilterEnabled(true);
                //userLv.setFilterText(cs.toString().trim());
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                //if(s.length() == 0) userLv.clearTextFilter();
                userList.this.adapt.getFilter().filter(s.toString());
            }
        });

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_containerU);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.black);
    }
    //Check if network is connected
    public boolean isNetworkConnected(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();// && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }
    public void getText(){
        String textValue = getIntent().getStringExtra("user");
        inputSearch.setText(textValue);
    }


    /*----------------------------------------REFRESH USERS------------------------------------------------------------------*/
    //Functionality for swipe to refresh
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Drop old table:
                controlUser.refreshCases("users");

                new getUsers().execute();
                getSQLiteUsers();
                refreshAtTop();

                //To update the userList:
                check = 0;
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }
    public void refreshAtTop() {
       userLv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (userLv == null || userLv.getChildCount() == 0) ?
                                0 : userLv.getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });
    }

/*-----------------------------------------------ON LIST ITEM CLICK----------------------------------------------------------------------------*/
    public void onListItemClick(){
        //when list item is clicked go to add case to edit that item
        //and send data to add case using bundles
        userLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int position, long id) {
                ArrayList<HashMap<String, String>> map = listUser;
                Object list = p.getItemAtPosition(position);
                map.add(position, (HashMap<String, String>) list);

                Intent intent = new Intent(userList.this, AddCase.class);
                intent.putExtra("caller", "userList");

                Bundle bundle = new Bundle();
                bundle.putString(MainActivity.TAG_USERID, map.get(position).get(MainActivity.TAG_USERID));
                bundle.putString(MainActivity.TAG_NAME, map.get(position).get(MainActivity.TAG_NAME));
                bundle.putString(MainActivity.TAG_COMPANY, map.get(position).get(MainActivity.TAG_COMPANY));
                bundle.putString(MainActivity.TAG_EMAIL, map.get(position).get(MainActivity.TAG_EMAIL));
                bundle.putString(MainActivity.TAG_TELEPHONE, map.get(position).get(MainActivity.TAG_TELEPHONE));
                intent.putExtras(bundle);

                setResult(Activity.RESULT_OK, intent);
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
                finish();
            }
        });
    }

    /*-------------------------------------------------GET SQLITE USERS--------------------------------------------------------------*/
    public void getSQLiteUsers(){
        getListView();
        // Get Cases records from SQLite DB
        listUser = controlUser.getAllUsers();
        // If users exists in SQLite DB
        if (listUser.size() != 0) {

            mAdapter = new SimpleAdapter(
                    userList.this, listUser,
                    R.layout.listview_item, new String[] { MainActivity.TAG_NAME,
                    MainActivity.TAG_COMPANY, MainActivity.TAG_EMAIL,
                    MainActivity.TAG_TELEPHONE},
                    new int[] { R.id.usernameMain,
                            R.id.companyMain, R.id.emailMain,
                            R.id.telMain});
            setListAdapter(mAdapter);
            //userLv.setAdapter(mAdapter);
        }
        controlUser.close();
    }
    public ListView getListView() {
        if(userLv == null){
            userLv = (ListView)findViewById(android.R.id.list);
            adapt = new UserListAdapter();
            userLv.setAdapter(adapt);
            return userLv;
        }
        return userLv;
    }
    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }
   // THIS IS THE USERLIST ADAPTER FOR THE LISTVIEW
    private class UserListAdapter extends ArrayAdapter implements Filterable {
        private Filter filter;

        public UserListAdapter() {
            super (userList.this, R.layout.listview_item, android.R.id.list);
        }

        @Override
        public Filter getFilter() {

            filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                    // Now we have to inform the adapter about the new list filtered
                    useList = (ArrayList<HashMap<String, String>>) results.values;
                    if (results.count == 0)
                        notifyDataSetInvalidated();
                    else {
                        for (int i=0; i<useList.size(); i++)
                        {
                            String s = (String)useList.get(i).get(MainActivity.TAG_NAME);
                            add(s);
                        }
                        notifyDataSetChanged();
                    }
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<HashMap<String, String>> FilteredArrayNames = new ArrayList<HashMap<String, String>>();

                    // perform your search here using the searchConstraint String.

                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < controlUser.getTableValues("users", 1).size(); i++) {
                        ArrayList<String> dataNames = controlUser.getTableValues("users", 1);
                        if (dataNames.get(i).trim().toLowerCase().contains(constraint.toString().toLowerCase().trim())){
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(MainActivity.TAG_NAME, dataNames.get(i));
                            FilteredArrayNames.add(map);
                        }
                    }

                    results.count = FilteredArrayNames.size();
                    results.values = FilteredArrayNames;
                    Log.e("VALUES", results.values.toString());

                    //controlUser.close();
                    return results;
                }
            };

            return filter;
        }
    }


    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //gets Users from MySQL DB
    class getUsers extends AsyncTask<String, String, String> {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(userList.this);
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
                JSONObject jsonUse = jsonParser.makeHttpRequest(USER_URL, "GET", parameters);
                if(isNetworkConnected() == true) {
                    while(jsonUse == null){
                        try{
                            Thread.sleep(20);
                            jsonUse = jsonParser.makeHttpRequest(USER_URL, "GET", parameters);
                            if(jsonUse == null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(userList.this,"Error connecting to the Database \n Check your internet connection", Toast.LENGTH_SHORT).show();
                                        userList.this.finish();
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
                    users = jsonUse.getJSONArray(MainActivity.TAG_USERS);

                    //Loop through all the users
                    for(int i =0; i < users.length(); i++){
                        JSONObject u = users.getJSONObject(i);

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
                        controlUser.insertUser(maps);
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
            if (result != null) {
                userList.this.setProgressBarIndeterminateVisibility(false);
                runOnUiThread(new Runnable() {
                    public void run() {
                        //get users for AddCase
                        controlUser.getAllUsers();
                    }
                });
                check = controlUser.checkNumRows("users");
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
            pDialog.dismiss();
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
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
        if (id == android.R.id.home) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
            this.finish();
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        /**if(id == R.id.log_out) {
            controlUser.refreshCases("cases");
            sharedPreference.delete(this);
            Intent intent = new Intent(context, Settings.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.finishAffinity();
            startActivity(intent);
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }
}
