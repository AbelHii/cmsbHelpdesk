package com.mycompany.CMSBHelpdesk;

/**
 * Created by Abel on 09/06/2015.
 */
public class Case {
    private String username ;
    private String description;
    private String actiontaken;
    private String assignee;
    private String status;
    private String id;
    private String login_id;
    private String status_id;
    private String sync;

    public Case(String id, String username, String description, String status, String sync){
        this.id = id;
        this.username = username;
        this.description = description;
        this.status = status;
        this.sync = sync;
    }

    //GETTERS:
    public String getID(){
        return id;
    }
    public String getUsername(){
        return username;
    }
    public String getDescription(){
        return description;
    }
    public String getStatus(){
        return status;
    }
    public String getSync(){
        return sync;
    }

    //SETTERS:
    public void setID(String id){
        this.id = id;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public void setDescription(String description){
        this.description = description;
    }
    public void setStatus(String status){
        this.status = status;
    }

}
