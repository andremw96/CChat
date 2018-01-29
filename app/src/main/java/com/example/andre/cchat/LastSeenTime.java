package com.example.andre.cchat;

import android.app.Application;
import android.content.Context;

/**
 * Created by Wijaya_PC on 26-Jan-18.
 */

public class LastSeenTime extends Application
{
    private static final int SECCOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECCOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimeAgo(long time, Context ctx)
    {
        if (time < 1000000000000L)
        {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if(time > now || time <= 0)
        {
            return null;
        }

        final long diff = now - time;
        if(diff < MINUTE_MILLIS)
        {
            return "baru saja";
        }
        else if (diff < 2 * MINUTE_MILLIS)
        {
            return "1 menit lalu";
        }
        else if (diff < 50 * MINUTE_MILLIS)
        {
            return diff / MINUTE_MILLIS + " menit lalu";
        }
        else if (diff < 90 * MINUTE_MILLIS)
        {
            return "1 jam lalu";
        }
        else if (diff < 24 * HOUR_MILLIS)
        {
            return diff / HOUR_MILLIS + " jam lalu";
        }
        else if (diff < 48 * HOUR_MILLIS)
        {
            return "kemarin";
        }
        else
        {
            return diff / DAY_MILLIS + " hari lalu";
        }

    }
}
