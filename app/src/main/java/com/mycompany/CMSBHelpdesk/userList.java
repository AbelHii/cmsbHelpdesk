package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.CMSBHelpdesk.helpers.DBController;
import com.mycompany.CMSBHelpdesk.helpers.JSONParser;
import com.mycompany.CMSBHelpdesk.helpers.internetCheck;
import com.mycompany.CMSBHelpdesk.helpers.sharedPreference;
import com.mycompany.CMSBHelpdesk.objects.User;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Abel Hii 2015
 *
 * Note: this works a bit like the MainActivity except it displays the list of users and has a search functionality
 *       to search through the list of users
 */

public class userList extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {

    public ListView userLv;
    EditText inputSearch;
    static UserListAdapter userAdapter;
    String textValue;

    public SwipeRefreshLayout swipeLayout;

    // ArrayList for Listview
    static ArrayList<HashMap<String, String>> listUser;
    ArrayList<User> filteredData = new ArrayList<>();

    //DB variables:
    DBController controlUser = new DBController(this);
    public static int check = 0;
    JSONParser jsonParser = new JSONParser();
    public static JSONArray users = null;
    public static JSONArray companies = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        //set actionbar colour
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.colorAB)));

        //Default Back Button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Choose User");

        initialise();
        check = controlUser.checkNumRows("users");
        //This is the process which loads the spinner depending on the situation
        if (check == 0 || check < 0) {
            //check if connected to internet or not
            if (internetCheck.connectionCheck(userList.this)) {
                new getUsers().execute();
                //Loads the list from MySQL DB
                getSQLiteUsers();
                refreshAtTop();
                //Toast.makeText(userList.this, "4", Toast.LENGTH_SHORT).show();
            } else {
                //Retrieve previously saved data
                Toast.makeText(userList.this, "User List Empty and No Internet Connection!" +
                                " \n Please Connect to the internet and refresh the list",
                        Toast.LENGTH_LONG).show();
            }
        } else if (internetCheck.isNetworkConnected(userList.this) && check > 0) {
            getSQLiteUsers();
            refreshAtTop();
            //Toast.makeText(userList.this, "5", Toast.LENGTH_SHORT).show();
        } else if (!internetCheck.isNetworkConnected(userList.this)) {
            swipeLayout.setEnabled(false);
            getSQLiteUsers();
            //Toast.makeText(userList.this, "6", Toast.LENGTH_SHORT).show();
        }
        onListItemClick();
    }




    /*---------------------------------------INITIALISE AND CHECKS----------------------------------------------------------------*/
    public void initialise(){
        check = controlUser.checkNumRows("users");

        //Initialise filter functionality:
        ArrayList<User> emptyU = new ArrayList<>();
        userAdapter = new UserListAdapter(this, emptyU);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_containerU);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.black);
    }

    /*----------------------------------------REFRESH USERS------------------------------------------------------------------*/
    //Functionality for swipe to refresh (works the same as in MainActivity)
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Drop old table:
                inputSearch.setText("");
                controlUser.refreshCases("users");
                refreshList();
                //get new table
                new getUsers().execute();
                getSQLiteUsers();

                //To update the userList:
                check = 0;
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }
    private void refreshAtTop() {
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
    //This is to make sure that the list updates properly instead of stacking ontop of the old one
    public void refreshList(){
        if(userAdapter!=null && listUser!=null) {
            listUser.clear();
            userAdapter.clear();
        }
    }





/*-----------------------------------------------ON LIST ITEM CLICK----------------------------------------------------------------------------*/
    public void onListItemClick(){
        //when list item is clicked go to add case to edit that item
        //and send data to add case using bundles
        userLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int position, long id) {
                Intent intent = new Intent(userList.this, AddCase.class);

                Bundle bundle = new Bundle();
                bundle.putString(MainActivity.TAG_USERID, filteredData.get(position).getUserID());
                bundle.putString(MainActivity.TAG_NAME, filteredData.get(position).getName());
                if(textValue != null) {
                    if (!textValue.equals(listUser.get(position).get(MainActivity.TAG_NAME).toString()))
                        bundle.putString("nameChange", "true");
                }
                intent.putExtras(bundle);

                //Check to see if has to retrieve data or not (goes to Add Case)
                AddCase.caller = getIntent().getStringExtra("caller");
                if(AddCase.caller != null) {
                    //To set the case id for adding case if cases are empty
                    if(controlUser.getTableValues("cases", 0).isEmpty())
                        sharedPreference.setString(userList.this, MainActivity.TAG_ID, "0");
                    else if(controlUser.getTableValues("cases", 0) != null) {
                        if(sharedPreference.getString(userList.this, MainActivity.TAG_ID).equals("") && internetCheck.isNetworkConnected(userList.this)) {
                            MainActivity.caseid = controlUser.getMaxId("cases");
                            sharedPreference.setString(userList.this, MainActivity.TAG_ID, MainActivity.caseid);
                        }else if(!internetCheck.isNetworkConnected(userList.this)){
                            MainActivity.caseid = sharedPreference.getString(userList.this, MainActivity.TAG_ID);
                            sharedPreference.setString(userList.this, MainActivity.TAG_ID, MainActivity.caseid);
                        }
                    }
                    intent.putExtra("caller", "addcase");
                    startActivityForResult(intent, Activity.RESULT_OK, bundle);
                }

                //Closes keyboard when finished:
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

            ArrayList<User> u = new ArrayList<User>();
            for(int i = 0; i<listUser.size(); i++){
                u.add(new User(listUser.get(i).get(MainActivity.TAG_USERID),
                        listUser.get(i).get(MainActivity.TAG_NAME),
                        listUser.get(i).get(MainActivity.TAG_COMPANY),
                        listUser.get(i).get(MainActivity.TAG_EMAIL),
                        listUser.get(i).get(MainActivity.TAG_TELEPHONE)));
            }
            userAdapter.addAll(u);
            setListAdapter(userAdapter);
            doFilter();
            check = controlUser.checkNumRows("users");
            userAdapter.notifyDataSetChanged();
            onListItemClick();
        }
        controlUser.close();
    }



    /*--------------------------------------------USER LIST ADAPTER ---------------------------------------------------------------*/
    public ListView getListView() {
        if(userLv == null){
            userLv = (ListView)findViewById(android.R.id.list);
            return userLv;
        }
        return userLv;
    }
    public void setListAdapter(ListAdapter listAdapter) {
        getListView().setAdapter(listAdapter);
    }

    static class ViewHolder {
        TextView mUsername;
        TextView mCompany;
        TextView mEmail;
        TextView mTelephone;
    }

    public void doFilter(){
        //Does the searching/filtering
        inputSearch = (EditText) findViewById(R.id.inputSearch);
        textValue = getIntent().getStringExtra("user");
        inputSearch.setText(textValue);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                // When user changed the Text
                userList.this.userAdapter.getFilter().filter(cs.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /*-------------------------IMPORTANT CODE!---------------------------------------------------------------------------*/
    // THIS IS THE USER LIST ADAPTER FOR THE LIST VIEW
    // IT DOES THE FILTERING FOR THE SEARCH BAR AS WELL AS STYLE THE LIST VIEW
    private class UserListAdapter extends ArrayAdapter<User> implements Filterable {
        private Filter filter;
        private ArrayList<User> items = new ArrayList<>();
        private Context context;

       public UserListAdapter(Context context, ArrayList<User> data) {
            super (userList.this, R.layout.listview_item, data);
            this.context = context;
            this.items = data;
            filteredData = data;
        }

        @Override
        public int getCount() {
            return filteredData.size();
        }

        @Override
        public User getItem(int position) {
            return filteredData.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = convertView;
            ViewHolder holder;
            if(view == null){
                LayoutInflater vi = LayoutInflater.from(getContext());
                view = vi.inflate(R.layout.listview_item, parent, false);

                holder = new ViewHolder();
                holder.mUsername = (TextView) view.findViewById(R.id.usernameMain);
                holder.mCompany = (TextView) view.findViewById(R.id.companyMain);
                holder.mEmail = (TextView) view.findViewById(R.id.emailMain);
                holder.mTelephone = (TextView) view.findViewById(R.id.telMain);

                view.setTag(holder);
            }else{
                holder = (ViewHolder) view.getTag();
            }

            User u = filteredData.get(position);
            holder.mUsername.setText(u.getName());
            holder.mCompany.setText(u.getCompany());
            holder.mEmail.setText(u.getEmail());
            holder.mTelephone.setText(u.getTelephone());

            if(position % 2 == 0){
                view.setBackgroundResource(R.drawable.mycolors);
            }else {
                view.setBackgroundResource(R.drawable.mycolors2);
            }

            return view;
        }

        @Override
        public Filter getFilter() {
            filter = new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                    // Now we have to inform the adapter about the new list filtered
                    filteredData = (ArrayList<User>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    String filterString = constraint.toString().toLowerCase();
                    FilterResults results = new FilterResults();
                    ArrayList<User> FilteredArrayNames = new ArrayList<User>();

                    // perform your search here using the searchConstraint String.
                    for (int i = 0; i < items.size(); i++) {

                        //Allow users to search specific data:
                        User dataNames = items.get(i);
                        String name = dataNames.getName();
                        String comp = dataNames.getCompany();
                        String email = dataNames.getEmail();
                        String tel = dataNames.getTelephone();

                        if (name.toLowerCase().contains(filterString) || comp.toLowerCase().contains(filterString)
                                || email.toLowerCase().contains(filterString) || tel.toLowerCase().contains(filterString)){
                            FilteredArrayNames.add(dataNames);
                        }
                    }
                    results.count = FilteredArrayNames.size();
                    results.values = FilteredArrayNames;
                    Log.e("VALUES", results.values.toString());

                    return results;
                }
            };

            return filter;
        }
    }


    /*---------IMPORTANT CODE!----------------------------------------------------------------*/
    //gets Users from MySQL DB
    class getUsers extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            int success = 0;

            //Building Parameters
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            try{
                JSONObject jsonUse = null;
                //get JSON string from URL
                if(!MainActivity.USER_URL.equals(""))
                jsonUse = jsonParser.makeHttpRequest(MainActivity.USER_URL, "GET", parameters);
                if(internetCheck.isNetworkConnected(userList.this) == true) {
                    while(jsonUse == null){
                        try{
                            Thread.sleep(20);
                            jsonUse = jsonParser.makeHttpRequest(MainActivity.USER_URL, "GET", parameters);
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
                    companies = jsonUse.getJSONArray(MainActivity.TAG_DIVISIONS);

                    //Loop through all the users
                    for(int i =0; i < users.length(); i++){
                        JSONObject u = users.getJSONObject(i);

                        String id = u.getString(MainActivity.TAG_USERID);
                        String name = u.getString(MainActivity.TAG_NAME);
                        String company = u.getString(MainActivity.TAG_COMPANY);
                        String email = u.getString(MainActivity.TAG_EMAIL);
                        String telephone = u.getString(MainActivity.TAG_TELEPHONE);
                        String division_id = u.getString(MainActivity.TAG_DIVISION_ID);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(MainActivity.TAG_USERID, id);
                        maps.put(MainActivity.TAG_NAME, name);
                        maps.put(MainActivity.TAG_COMPANY, company);
                        maps.put(MainActivity.TAG_EMAIL, email);
                        maps.put(MainActivity.TAG_TELEPHONE, telephone);
                        maps.put(MainActivity.TAG_DIVISION_ID, division_id);

                        listUser.add(maps);

                        //add this map to SQLite too
                        controlUser.insertUser(maps);
                    }

                    //Loop through all the companies
                    for(int j =0; j < companies.length(); j++){
                        JSONObject u = companies.getJSONObject(j);

                        String id = u.getString(MainActivity.TAG_COMPANY_ID);
                        String name = u.getString(MainActivity.TAG_COMPANY_NAME);
                        String enabled = u.getString(MainActivity.TAG_ENABLED);

                        //create a new HashMap
                        HashMap<String, String> maps = new HashMap<String, String>();

                        maps.put(MainActivity.TAG_COMPANY_ID, id);
                        maps.put(MainActivity.TAG_COMPANY_NAME, name);
                        maps.put(MainActivity.TAG_ENABLED, enabled);

                        //add this map to Company SQLite too
                        if(enabled.equals("1")) {
                            controlUser.insertCompany(maps);
                        }
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
                        ArrayList<User> u = new ArrayList<User>();
                        for(int i = 0; i<listUser.size(); i++){
                            u.add(new User(listUser.get(i).get(MainActivity.TAG_USERID),
                                    listUser.get(i).get(MainActivity.TAG_NAME),
                                    listUser.get(i).get(MainActivity.TAG_COMPANY),
                                    listUser.get(i).get(MainActivity.TAG_EMAIL),
                                    listUser.get(i).get(MainActivity.TAG_TELEPHONE)));
                        }

                        userAdapter.addAll(u);
                        setListAdapter(userAdapter);
                        doFilter();
                        check = controlUser.checkNumRows("users");
                    }
                });
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }



/*-------------------ACTION BAR ACTIVITY -----------------------------------------------------*/

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
        /*if (id == R.id.action_settings) {
            Intent intent = new Intent(context, Settings.class);
            startActivity(intent);
            return true;
        }*/
        if (id == android.R.id.home) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if(imm != null)
            imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
            this.finish();
            return true;
        }
        if(id==R.id.addNewUser){
            if(internetCheck.isNetworkConnected(this)) {
                Intent intent = new Intent(context, AddNewUser.class);
                startActivity(intent);
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
            }else{
                Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
