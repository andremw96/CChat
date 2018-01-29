package com.example.andre.cchat;

/**
 * Created by Wijaya_PC on 27-Jan-18.
 */

public class Chats {

    private String user_status;

    public Chats() { }

    public Chats(String user_status) {
        this.user_status = user_status;
    }

    public String getUser_status() {
        return user_status;
    }

    public void setUser_status(String user_status) {
        this.user_status = user_status;
    }
}
