package com.mycompany.CMSBHelpdesk;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.widget.Toast.*;

/**
 * Created by Abel on 30/03/2015.
 * To make the spinner more dynamic and auto fill the users details when selected.
 *
 */
public class spinnerMethods{

    //TODO: FIX method setTextV and OnItemSelected to be more useful
    public void onItemSelected(final Spinner blu, final TextView comp, final TextView email, final TextView tel){//, final Context c){
        blu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterViewCompat, View view, int i, long l) {
                String selItem = blu.getItemAtPosition(i).toString();

               // Toast.makeText(getApplicationContext(c), selItem, LENGTH_SHORT).show();

                setTextV(selItem, "abel", comp, email, tel, "CMS", "abelhii@gmail.com", "12345");
                setTextV(selItem, "josh", comp, email, tel, "CMS Roads", "joshy@gmail.com", "54321");
                setTextV(selItem, "art", comp, email, tel, "MCD", "arty@gmail.com", "00001234");
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterViewCompat) {

            }
        });
    }

    public void setTextV(String selectedItem, String name, TextView c,TextView e, TextView t, String valc, String vale, String valt){
        if(selectedItem.equalsIgnoreCase(name)) {
            c.setText(valc);
            e.setText(vale);
            t.setText(valt);
        }
    }

    public void changeColor(final Spinner red){
        red.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selItem = red.getItemAtPosition(position).toString();
                if (selItem.equalsIgnoreCase("not started")) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.parseColor("#ff0000"));
                } else if (selItem.equalsIgnoreCase("in progress")) {
                    //((TextView) parent.getChildAt(1)).setTextColor(Color.parseColor("#00ff00"));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }



    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return null;
    }

   /*public Context getApplicationContext(Context applicationContext) {
        return applicationContext;
    }*/
}
