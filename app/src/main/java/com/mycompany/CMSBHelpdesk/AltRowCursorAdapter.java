package com.mycompany.CMSBHelpdesk;
/**
 *
 */

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

/**
 * @author C'dine
 * A simple cursor adapter. Only variation is that it displays alternate rows
 *  in alternate colors.
 */
public class AltRowCursorAdapter extends SimpleCursorAdapter{

    private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") };
    //private int[] colors = new int[] { R., 0x30808080 };
    public AltRowCursorAdapter(Context context, int layout, Cursor c,
                                     String[] from, int[] to) {
        super(context, layout, c, from, to);
    }
    /**
     * Display rows in alternating colors
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        int colorPos = position % colors.length;
        view.setBackgroundColor(colors[colorPos]);
        return view;
    }
}