package com.osmaniyeliabdullah.prismio;

import android.app.Application;

import com.google.android.gms.games.PlayGamesSdk;

/**
 * Play Games SDK burada baslatilir.
 * Google'in belgesi bunu Application.onCreate icinde yapmayi sart kosuyor.
 */
public class PrismioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PlayGamesSdk.initialize(this);
    }
}
