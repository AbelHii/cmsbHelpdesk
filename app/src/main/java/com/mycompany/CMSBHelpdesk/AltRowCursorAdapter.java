package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author C'dine
 * A simple cursor adapter. Only variation is that it displays alternate rows
 *  in alternate colors.
 */
public class AltRowCursorAdapter extends ArrayAdapter<HashMap<String, String>>{
    private String[] list;

    public AltRowCursorAdapter(Context context, int textViewResourceId,
                             ArrayList<HashMap<String, String>> objects) {
        super(context, textViewResourceId, objects);
        list = new String[objects.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = String.valueOf(objects.get(i));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = (TextView)super.getView(position, convertView, parent);
        if(position%2==0)
        {
            view.setBackgroundColor(Color.parseColor("#FFD700"));
        }
        return view;
    }
}