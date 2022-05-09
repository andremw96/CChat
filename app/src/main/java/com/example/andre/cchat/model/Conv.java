package com.example.andre.cchat.model;

/**
 * Created by Wijaya_PC on 27-Jan-18.
 */

public class Conv
{
    public boolean seen;
    public long timestamp;

    public Conv() { }

    public Conv(boolean seen, long timestamp) {
        this.seen = seen;
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
