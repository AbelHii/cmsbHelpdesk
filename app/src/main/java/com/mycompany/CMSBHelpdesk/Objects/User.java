package com.mycompany.CMSBHelpdesk.objects;

/**
 * Created by Abel on 19/06/2015.
 */
public class User {
    private String userID, name, company, email, telephone;

    public User(String userID, String name, String company, String email, String telephone){
        this.userID = userID;
        this.name = name;
        this.company = company;
        this.email = email;
        this.telephone = telephone;
    }

    public String getUserID(){return userID;}
    public String getName(){
        return name;
    }
    public String getCompany(){
        return company;
    }
    public String getEmail(){
        return email;
    }
    public String getTelephone(){
        return telephone;
    }

}
