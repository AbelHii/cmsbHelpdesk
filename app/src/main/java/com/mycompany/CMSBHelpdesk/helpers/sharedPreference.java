package com.mycompany.CMSBHelpdesk.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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

    public static void setStringSet(Context ctx, String key, Set<String> arr)
    {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putStringSet(key, arr);
        editor.commit();
    }
    public static Set getStringSet(Context ctx, String key) {
        Set<String> abc= new Set<String>() {
            @Override
            public boolean add(String s) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends String> strings) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> objects) {
                return false;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @NonNull
            @Override
            public Iterator<String> iterator() {
                return null;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> objects) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> objects) {
                return false;
            }

            @Override
            public int size() {
                return 0;
            }

            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @NonNull
            @Override
            public <T> T[] toArray(T[] ts) {
                return null;
            }
        };
        return getSharedPreferences(ctx).getStringSet(key, abc);
    }

    public static void delete(Context ctx){
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.clear();
        editor.commit();
    }
}