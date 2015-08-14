package com.mycompany.CMSBHelpdesk.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mycompany.CMSBHelpdesk.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Abel on 05/05/2015.
 */
public class DBController extends SQLiteOpenHelper{

    public DBController(Context applicationcontext){
        super(applicationcontext, "androidsqlite.db", null, 1);
    }

    public SQLiteDatabase openDB(){
        return this.getWritableDatabase();
    }

    //Creates Table
    @Override
    public void onCreate(SQLiteDatabase database) {
        String cases, users, companies;

        //Cases
        cases = "CREATE TABLE cases (id INTEGER PRIMARY KEY AUTOINCREMENT, assignee TEXT, " +
                "status TEXT, user TEXT, description TEXT, actiontaken TEXT, login_id TEXT, status_id TEXT, sync TEXT, image TEXT)";
        database.execSQL(cases);

        //Users
        users = "CREATE TABLE users (userId INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, company TEXT, " +
                "email TEXT, telephone TEXT, division_id TEXT)";
        database.execSQL(users);

        //Companies
        companies = "CREATE TABLE companies (companyId INTEGER PRIMARY KEY AUTOINCREMENT, companyName TEXT, enabled TEXT)";
        database.execSQL(companies);
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
        values.put(MainActivity.TAG_ACTION_TAKEN, queryValues.get(MainActivity.TAG_ACTION_TAKEN));
        values.put(MainActivity.TAG_LOGIN_ID, queryValues.get(MainActivity.TAG_LOGIN_ID));
        values.put(MainActivity.TAG_STATUS_ID, queryValues.get(MainActivity.TAG_STATUS_ID));
        values.put(MainActivity.TAG_SYNC, queryValues.get(MainActivity.TAG_SYNC));
        values.put(MainActivity.TAG_IMAGE, queryValues.get(MainActivity.TAG_IMAGE));

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
        values.put(MainActivity.TAG_DIVISION_ID, queryValues.get(MainActivity.TAG_DIVISION_ID));
        database.insert("users", null, values);
        database.close();
    }
    /**
     * Inserts Companies into SQLite DB
     * @param queryValues
     */
    public void insertCompany(HashMap<String, String> queryValues){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        //user Spinner List
        values.put(MainActivity.TAG_COMPANY_ID, queryValues.get(MainActivity.TAG_COMPANY_ID));
        values.put(MainActivity.TAG_COMPANY_NAME, queryValues.get(MainActivity.TAG_COMPANY_NAME));
        values.put(MainActivity.TAG_ENABLED, queryValues.get(MainActivity.TAG_ENABLED));
        database.insert("companies", null, values);
        database.close();
    }

    /**
     * Get list of Cases from SQLite DB as Array List
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
                //case List
                //Doing this for id to auto increment when i add a new case offline:
                map.put(MainActivity.TAG_ID, cursor.getString(0));
                map.put(MainActivity.TAG_ASSIGNEE, cursor.getString(1));
                map.put(MainActivity.TAG_STATUS, cursor.getString(2));
                map.put(MainActivity.TAG_USERNAME,cursor.getString(3));
                map.put(MainActivity.TAG_DESCRIPTION, cursor.getString(4));
                map.put(MainActivity.TAG_ACTION_TAKEN, cursor.getString(5));
                map.put(MainActivity.TAG_LOGIN_ID, cursor.getString(6));
                map.put(MainActivity.TAG_STATUS_ID, cursor.getString(7));
                map.put(MainActivity.TAG_SYNC, cursor.getString(8));
                map.put(MainActivity.TAG_IMAGE, cursor.getString(9));

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

        String selectQuery = "SELECT * FROM users ORDER BY name";
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
                map.put(MainActivity.TAG_DIVISION_ID, cursor.getString(5));
                userList.add(map);

            } while (cursor.moveToNext());
        }
        database.close();
        return userList;
    }


    //Get Table Values for AddCase
    public ArrayList<String> getTableValues(String table, int num){
        SQLiteDatabase database = this.getWritableDatabase();
        ArrayList<String> spin;
        spin = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ table;
        Cursor cursor = database.rawQuery(selectQuery, null);

        //Start from last id because of auto increment in cases table!
        if(cursor.moveToLast()){
            do {
                //Gets the string at the specified num:
                spin.add(cursor.getString(num));
            }while(cursor.moveToPrevious());
        }

        database.close();
        return spin;
    }

    //get a value
    public String getValue(String table, String column, String caseId){
        SQLiteDatabase database = this.getWritableDatabase();
        String s = null;
        String selectQuery = "SELECT "+column+" FROM "+table+" WHERE id ="+caseId;

        Cursor cursor = database.rawQuery(selectQuery, null);

        if(cursor.moveToFirst())
            s = cursor.getString(0);

        database.close();
        return s;
    }

    //get a column
    public ArrayList<String> getColumn(String table, String column){
        SQLiteDatabase database = this.getWritableDatabase();
        ArrayList<String> list = new ArrayList();
        String selectQuery = "SELECT "+column+" FROM "+table;
        Cursor cursor = database.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0).toString());
            }while(cursor.moveToNext());
        }

        database.close();
        return list;
    }

    //Insert String to table
    public void insertValue(String table, String column, String val){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(column, val);
        //String query = "INSERT INTO "+table+" (name, company, email, telephone) "+
                //"VALUES ('"+name+"', null, null, null)";

        //database.execSQL(query);
        database.insert(table, column, values);
    }

    public void insertOneCase(String id, String status, String user, String desc, String aT, String logID, String statID, String sync, String image){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(MainActivity.TAG_ID, id);
        values.put(MainActivity.TAG_STATUS, status);
        values.put(MainActivity.TAG_USERNAME, user);
        values.put(MainActivity.TAG_DESCRIPTION, desc);
        values.put(MainActivity.TAG_ACTION_TAKEN, aT);
        values.put(MainActivity.TAG_LOGIN_ID, logID);
        values.put(MainActivity.TAG_STATUS_ID, statID);
        values.put(MainActivity.TAG_SYNC, sync);
        values.put(MainActivity.TAG_IMAGE, image);

        database.insert("cases", null, values);
        database.close();
    }

    public void updateOneCase(String id, String status, String user, String desc, String aT, String logID, String statID, String sync, String image){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(MainActivity.TAG_STATUS, status);
        values.put(MainActivity.TAG_USERNAME, user);
        values.put(MainActivity.TAG_DESCRIPTION, desc);
        values.put(MainActivity.TAG_ACTION_TAKEN, aT);
        values.put(MainActivity.TAG_LOGIN_ID, logID);
        values.put(MainActivity.TAG_STATUS_ID, statID);
        values.put(MainActivity.TAG_SYNC, sync);
        values.put(MainActivity.TAG_IMAGE, image);

        database.update("cases", values, "id = "+id, null);
        database.close();
    }

    public void updateImage(String id, String image){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(MainActivity.TAG_IMAGE, image);

        database.update("cases", values, "id = "+id, null);
        database.close();
    }


    public void updateSyncValue(String table, String id, String sync, String TAG){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TAG, sync);

        database.update(table, values, "id = "+id, null);
        database.close();
    }

    public List<String> getUsersData(String name){
        SQLiteDatabase database = this.getWritableDatabase();
        List<String> data = new ArrayList<>(3);
        String query = "SELECT company, email, telephone FROM users WHERE name = '"+name+"'";

        Cursor cursor = database.rawQuery(query, null);
        if(cursor.moveToFirst()){
            do{
                data.add(0, cursor.getString(0));
                data.add(1, cursor.getString(1));
                data.add(2, cursor.getString(2));
            }while(cursor.moveToNext());
        }

        return data;
    }

    public String getID(String table, String id, String name, String columnName){
        SQLiteDatabase database = this.getWritableDatabase();
        String value = "";
        String query = "SELECT "+id+" FROM "+table+" WHERE "+columnName+" = '"+name+"'";

        Cursor cursor = database.rawQuery(query, null);
        if(cursor.moveToFirst()){
            value = cursor.getString(0);
        }

        database.close();
        return value;
    }

    //gets the most recent id:
    public String getMaxId(String table){
        SQLiteDatabase database = this.getWritableDatabase();
        String id = "";
        String query = "SELECT MAX(id) AS id FROM "+table;
        Cursor cursor = database.rawQuery(query, null);

        if(cursor.moveToFirst())
            id = cursor.getString(0);


        database.close();
        return id;
    }

    public String getMinId(String table){
        SQLiteDatabase database = this.getWritableDatabase();
        String id = "";
        String query = "SELECT MIN(id) AS id FROM "+table;
        Cursor cursor = database.rawQuery(query, null);

        if(cursor.moveToFirst())
            id = cursor.getString(0);

        database.close();
        return id;
    }


    public void refreshCases(String table){
        SQLiteDatabase database = this.getWritableDatabase();
        String dropQuery = "", refreshQuery= "", dropQuery2 = "", refreshQuery2 = "";

        switch(table){
            case "cases":
                dropQuery = "DROP TABLE IF EXISTS cases";
                refreshQuery = "CREATE TABLE cases (id INTEGER PRIMARY KEY AUTOINCREMENT, assignee TEXT, " +
                        "status TEXT, user TEXT, description TEXT, actiontaken TEXT, login_id TEXT, status_id TEXT, sync TEXT, image TEXT)";
                break;
            case "users":
                dropQuery = "DROP TABLE IF EXISTS users";
                refreshQuery = "CREATE TABLE users (userId INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, company TEXT, " +
                        "email TEXT, telephone TEXT, division_id TEXT)";

                //refresh companies with users:
                dropQuery2 = "DROP TABLE IF EXISTS companies";
                refreshQuery2 = "CREATE TABLE companies (companyId INTEGER PRIMARY KEY AUTOINCREMENT, companyName TEXT, enabled TEXT)";
                database.execSQL(dropQuery2);
                database.execSQL(refreshQuery2);
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
        database.close();
        return rows;
    }
}
