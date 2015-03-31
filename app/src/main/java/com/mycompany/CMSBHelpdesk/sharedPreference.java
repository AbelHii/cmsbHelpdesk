package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Abel on 30/03/2015.
 */
public class sharedPreference
{
    private static sharedPreference applicationContext;

    static SharedPreferences getSharedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static void setString(Context ctx, String key, String str)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(key, str);
        editor.commit();
    }
    public static String getString(Context ctx, String key)
    {
        return getSharedPreferences(ctx).getString(key, "");
    }

    public static void setInt(Context ctx, String key, int num)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putInt(key, num);
        editor.commit();
    }
    public static int getInt(Context ctx, String key) {
        return getSharedPreferences(ctx).getInt(key, 0);
    }

    public static void delete(Context ctx){
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.clear();
        editor.commit();
    }
}