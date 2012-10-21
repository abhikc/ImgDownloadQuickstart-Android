package com.kinvey.starter;

import android.app.Application;

import com.kinvey.KCSClient;
import com.kinvey.KinveySettings;

/**
 * Store global state. In this case, the single instance of KCS.
 * 
 */
public class ImgDownloadQuickstartApp extends Application {

    private KCSClient service;

	// Enter your Kinvey app credentials
    private static final String APP_KEY = "<your_app_key>";
    private static final String APP_SECRET = "<your_app_secret>";
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    private void initialize() {
		// Enter your app credentials here
		service = KCSClient.getInstance(this.getApplicationContext(), new KinveySettings(APP_KEY, APP_SECRET));
    }

    public KCSClient getKinveyService() {
        return service;
    }

}
