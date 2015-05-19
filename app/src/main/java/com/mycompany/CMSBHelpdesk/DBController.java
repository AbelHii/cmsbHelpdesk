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

import java.lang.reflect.Array;
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
        String cases, users;

        //Cases
        cases = "CREATE TABLE cases (id INTEGER PRIMARY KEY AUTOINCREMENT, assignee TEXT, " +
                "status TEXT, user TEXT, description TEXT)";
        database.execSQL(cases);

        //Users
        users = "CREATE TABLE users (userId INTEGER, name TEXT, company TEXT, " +
                "email TEXT, telephone TEXT)";
        database.execSQL(users);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
        String query;
        query = "DROP TABLE IF EXISTS cases, users";
        database.execSQL(query);
        onCreate(database);
    }

    /**
     * Inserts Case into SQLite DB
     */
    public void insertCase(HashMap<String, String> queryValues){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        //Case List
        values.put(MainActivity.TAG_ID, queryValues.get(MainActivity.TAG_ID));
        values.put(MainActivity.TAG_USERNAME, queryValues.get(MainActivity.TAG_USERNAME));
        values.put(MainActivity.TAG_DESCRIPTION, queryValues.get(MainActivity.TAG_DESCRIPTION));
        values.put(MainActivity.TAG_ASSIGNEE, queryValues.get(MainActivity.TAG_ASSIGNEE));
        values.put(MainActivity.TAG_STATUS, queryValues.get(MainActivity.TAG_STATUS));

        database.insert("cases", null, values);
        database.close();
    }

    /**
     * Inserts User into SQLite DB
     * @param queryValues
     */
    public void insertUser(HashMap<String, String> queryValues){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        //user Spinner List
        values.put(MainActivity.TAG_USERID, queryValues.get(MainActivity.TAG_USERID));
        values.put(MainActivity.TAG_NAME, queryValues.get(MainActivity.TAG_NAME));
        values.put(MainActivity.TAG_COMPANY, queryValues.get(MainActivity.TAG_COMPANY));
        values.put(MainActivity.TAG_EMAIL, queryValues.get(MainActivity.TAG_EMAIL));
        values.put(MainActivity.TAG_TELEPHONE, queryValues.get(MainActivity.TAG_TELEPHONE));
        database.insert("users", null, values);
        database.close();
    }


    /**
     * Get list of Cases from SQLite DB as Array List
     * @return
     */
    public ArrayList<HashMap<String, String>> getAllCases() {
        ArrayList<HashMap<String, String>> caseList;
        caseList = new ArrayList<HashMap<String, String>>();

        ArrayList<String> spin;
        spin = new ArrayList<>();

        String selectQuery = "SELECT * FROM cases ORDER BY id DESC";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                //create a new HashMap
                HashMap<String, String> map = new HashMap<String, String>();

                //add each child node to HashMap Key => value
                //case List
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
     * Get list of Users from SQLite DB as Array List
     * @return
     */
    public ArrayList<HashMap<String, String>> getAllUsers() {
        ArrayList<HashMap<String, String>> userList;
        userList = new ArrayList<HashMap<String, String>>();

        ArrayList<String> spin;
        spin = new ArrayList<>();

        String selectQuery = "SELECT * FROM users ORDER BY userId";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                //create a new HashMap
                HashMap<String, String> map = new HashMap<String, String>();

                //add each child node to HashMap Key => value
                //users Spinner List
                map.put(MainActivity.TAG_USERID, cursor.getString(0));
                map.put(MainActivity.TAG_NAME, cursor.getString(1));
                map.put(MainActivity.TAG_COMPANY, cursor.getString(2));
                map.put(MainActivity.TAG_EMAIL, cursor.getString(3));
                map.put(MainActivity.TAG_TELEPHONE, cursor.getString(4));
                userList.add(map);

            } while (cursor.moveToNext());
        }
        database.close();
        return userList;
    }


    //Get Table Values for AddCase
    public ArrayList<String> getTableValues(String table, int num){
        ArrayList<String> spin;
        spin = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ table;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do {
                //Gets the string at the specified num:
                spin.add(cursor.getString(num));
            }while(cursor.moveToNext());
        }

        database.close();
        return spin;
    }

    //Insert String to table
    public void insertValue(String table, String column, String name){
        ContentValues values = new ContentValues();
        values.put(column, name);
        //String query = "INSERT INTO "+table+" (name, company, email, telephone) "+
                //"VALUES ('"+name+"', null, null, null)";
        SQLiteDatabase database = this.getWritableDatabase();

        //database.execSQL(query);
        database.insert(table, column, values);

        database.close();
    }


    public void refreshCases(String table){
        SQLiteDatabase database = this.getWritableDatabase();
        String dropQuery = "", refreshQuery= "";

        switch(table){
            case "cases":
                dropQuery = "DROP TABLE IF EXISTS cases";
                refreshQuery = "CREATE TABLE cases (id INTEGER PRIMARY KEY AUTOINCREMENT, assignee TEXT, " +
                        "status TEXT, user TEXT, description TEXT)";
                break;
            case "users":
                dropQuery = "DROP TABLE IF EXISTS users";
                refreshQuery = "CREATE TABLE users (userId INTEGER, name TEXT, company TEXT, " +
                        "email TEXT, telephone TEXT)";
                break;
        }
        database.execSQL(dropQuery);
        database.execSQL(refreshQuery);
        database.close();
    }

    //Checks if SQLite is empty or not
    public int checkNumRows(String table){
        SQLiteDatabase database = this.getWritableDatabase();
        String query = "SELECT count(*) FROM " + table;
        Cursor cursor = database.rawQuery(query, null);

        //set rows to be the number of rows in the specified table
        cursor.moveToFirst();
        int rows = cursor.getInt(0);
        return rows;
    }
}
