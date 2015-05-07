package com.mycompany.CMSBHelpdesk;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by Abel on 29/04/2015.
 */
public class spinnerValues {

    private int id, telephone, company;
    private String name, email;

    public spinnerValues(){}

    public spinnerValues(int id, int telephone, String name, String email, int company){
        this.id = id;
        this.telephone = telephone;
        this.name = name;
        this.email = email;
        this.company = company;
    }

    //setters:
    public void setId(int id){this.id = id;}
    public void setTelephone(int telephone){this.telephone = telephone;}
    public void setName(String name){this.name = name;}
    public void setEmail(String email){this.email = email;}
    public void setCompany(int company){this.company = company;}

    //getters:
    public int getId(){return this.id;}
    public int getTelephone(){return this.telephone;}
    public String getName(){return this.name;}
    public String getEmail(){return this.email;}
    public int getCompany(){return this.company;}

    public void onItemSelected(final Spinner blu, final TextView comp, final TextView emaill, final TextView tel){
        blu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterViewCompat, View view, int i, long l) {
                String selItem = blu.getItemAtPosition(i).toString();

                setTextV(selItem, name, comp, emaill, tel, company, email, telephone);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterViewCompat) {

            }
        });
    }

    public void setTextV(String selectedItem, String name, TextView c,TextView e, TextView t, int valc, String vale, int valt){
        if(selectedItem.equalsIgnoreCase(name)) {
            c.setText(valc);
            e.setText(vale);
            t.setText(valt);
        }
    }
}
