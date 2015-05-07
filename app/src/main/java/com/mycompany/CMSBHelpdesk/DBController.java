package com.mycompany.CMSBHelpdesk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Abel on 05/05/2015.
 */
public class DBController extends SQLiteOpenHelper{

    public DBController(Context applicationcontext){
        super(applicationcontext, "androidsqlite.db", null, 1);
    }

    //Creates Table
    @Override
    public void onCreate(SQLiteDatabase database) {
        String query;
        query = "CREATE TABLE cases (id INTEGER , assignee TEXT, status TEXT, user TEXT, description TEXT)";
        database.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
        String query;
        query = "DROP TABLE IF EXISTS cases";
        database.execSQL(query);
        onCreate(database);
    }

    /**
     * Inserts Case into SQLite DB
     */
    public void insertCase(HashMap<String, String> queryValues){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MainActivity.TAG_ID, queryValues.get(MainActivity.TAG_ID));
        values.put(MainActivity.TAG_USERNAME, queryValues.get(MainActivity.TAG_USERNAME));
        values.put(MainActivity.TAG_DESCRIPTION, queryValues.get(MainActivity.TAG_DESCRIPTION));
        values.put(MainActivity.TAG_ASSIGNEE, queryValues.get(MainActivity.TAG_ASSIGNEE));
        values.put(MainActivity.TAG_STATUS, queryValues.get(MainActivity.TAG_STATUS));
        database.insert("cases", null, values);
        database.close();
    }

    /**
     * Get list of Users from SQLite DB as Array List
     * @return
     */
    public ArrayList<HashMap<String, String>> getAllCases() {
        ArrayList<HashMap<String, String>> caseList;
        caseList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM cases ORDER BY id DESC";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                //create a new HashMap
                HashMap<String, String> map = new HashMap<String, String>();

                //add each child node to HashMap Key => value
                map.put(MainActivity.TAG_ID, cursor.getString(0));
                map.put(MainActivity.TAG_ASSIGNEE, cursor.getString(1));
                map.put(MainActivity.TAG_STATUS, cursor.getString(2));
                map.put(MainActivity.TAG_USERNAME,cursor.getString(3));
                map.put(MainActivity.TAG_DESCRIPTION, cursor.getString(4));
                caseList.add(map);

            } while (cursor.moveToNext());
        }
        database.close();
        return caseList;
    }

    /**
     * Compose JSON out of SQLite records
     * @return
     */
    public String composeJSONfromSQLite(){
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT  * FROM cases where updateStatus = '"+"no"+"'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                //create a new HashMap
                HashMap<String, String> map = new HashMap<String, String>();

                //add each child node to HashMap Key => value
                map.put(MainActivity.TAG_ID, cursor.getString(0));
                map.put(MainActivity.TAG_USERNAME,cursor.getString(1));
                map.put(MainActivity.TAG_DESCRIPTION, cursor.getString(2));
                map.put(MainActivity.TAG_ASSIGNEE, cursor.getString(3));
                map.put(MainActivity.TAG_STATUS, cursor.getString(4));
                wordList.add(map);
            } while (cursor.moveToNext());
        }
        database.close();
        Gson gson = new GsonBuilder().create();
        //Use GSON to serialize Array List to JSON
        return gson.toJson(wordList);
    }

    /**
     * Get Sync status of SQLite
     * @return
     */
    public String getSyncStatus(){
        String msg = null;
        if(this.dbSyncCount() == 0){
            msg = "SQLite and Remote MySQL DBs are in Sync!";
        }else{
            msg = "DB Sync needed\n";
        }
        return msg;
    }

    /**
     * Get SQLite records that are yet to be Synced
     * @return
     */
    public int dbSyncCount(){
        int count = 0;
        String selectQuery = "SELECT  * FROM cases";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        database.close();
        return count;
    }

    /**
     * Update Sync status against each User ID
     * @param id
     * @param status
     */
    public void updateSyncStatus(String id, String status){
        SQLiteDatabase database = this.getWritableDatabase();
        String updateQuery = "UPDATE cases set updateStatus = '"+ status +"' where userId="+"'"+ id +"'";
        Log.d("query", updateQuery);
        database.execSQL(updateQuery);
        database.close();
    }

    public void refreshCases(){
        SQLiteDatabase database = this.getWritableDatabase();
        String dropQuery = "DROP TABLE IF EXISTS cases";
        String refreshQuery = "CREATE TABLE cases (id INTEGER PRIMARY KEY, assignee TEXT, status TEXT, user TEXT, description TEXT)";
        database.execSQL(dropQuery);
        database.execSQL(refreshQuery);
        database.close();
    }

}
