package com.mycompany.CMSBHelpdesk;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Abel on 04/03/2015.
 */
public class Case implements Parcelable{

    private String _user, _assignee, _desc, _status;
    private int _id;

    public Case (String desc, String user, String assignee, String status){
        //_id = id;
        _desc = desc;
        _user = user;
        _assignee = assignee;
        _status = status;
    }

    public Case(Parcel p){
        //this._id = p.readInt();
        this._desc = p.readString();
        this._user = p.readString();
        this._assignee = p.readString();
        this._status = p.readString();
    }

    public String getUser(){return _user;}
    public void setUser(String user){
        _user = user;
    }

    public String getAssignee(){return _assignee;}
    public void setAssignee(String assignee){
        _assignee = assignee;
    }

    public String getDesc(){return _desc;}
    public void setDesc(String desc){
        _desc = desc;
    }

    public int getId(){return _id;}
    public void setId(int id){
        _id = id;
    }

    public String getStatus(){return _status;}
    public void setStatus(String status){
        _status = status;
    }


    @Override
    public int describeContents(){
        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int flag){
        //parcel.writeInt(_id);
        parcel.writeString(_desc);
        parcel.writeString(_user);
        parcel.writeString(_assignee);
        parcel.writeString(_status);
    }


    public static Parcelable.Creator<Case> CREATOR = new Parcelable.Creator<Case>(){
        public Case createFromParcel(Parcel source){
            return new Case(source);
        }

        public Case[] newArray(int size){
            return new Case[size];
        }
    };

}
