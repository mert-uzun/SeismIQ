package com.seismiq.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;

public class SeismIQApplication extends Application {

    private static SeismIQApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        try {
            // Initialize Amplify for AWS Cognito authentication
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());
            Log.i("SeismIQApp", "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e("SeismIQApp", "Could not initialize Amplify", error);
        }
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}