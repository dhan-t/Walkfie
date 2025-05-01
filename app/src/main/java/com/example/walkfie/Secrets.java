package com.example.walkfie; // Replace with your actual package name

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Secrets {

    private static final String TAG = "Secrets";

    public static String getGoogleWebClientId(Context context) {
        return getProperty(context, "GOOGLE_WEB_CLIENT_ID");
    }
    public static String getFacebookAppId(Context context) {
        return getProperty(context, "FACEBOOK_APP_ID");
    }

    public static String getFbLoginProtocolScheme(Context context) {
        return getProperty(context, "FB_LOGIN_PROTOCOL_SCHEME");
    }

    public static String getFacebookClientToken(Context context) {
        return getProperty(context, "FACEBOOK_CLIENT_TOKEN");
    }

    private static String getProperty(Context context, String key) {
        try {
            Properties properties = new Properties();
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("local.properties"); //This should be in your assets

            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            Log.e(TAG, "Error reading local.properties", e);
            return null;
        }
    }
}