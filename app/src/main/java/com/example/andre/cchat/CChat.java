package com.example.andre.cchat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by Wijaya_PC on 26-Jan-18.
 */

public class CChat implements Application.ActivityLifecycleCallbacks
{
    private int numStarted;
    private long userEnteredTime;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (numStarted == 0)
        {
            userEnteredTime = System.currentTimeMillis();
        }
        numStarted++;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        numStarted--;
        if(numStarted == 0)
        {
            long timeInApp = System.currentTimeMillis() - userEnteredTime;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
