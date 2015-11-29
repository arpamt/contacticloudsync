package com.granita.contacticloudsync;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Daniel on 24/10/2015.
 */
public class About extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);

        Tracker t = ((SyncForICloud) getApplication()).getTracker(
                SyncForICloud.TrackerName.APP_TRACKER);
        t.setScreenName("Sync for iCloud Contacts: About");
        t.send(new HitBuilders.AppViewBuilder().build());

        WebView aboutView = (WebView)findViewById(R.id.about_webview);
        aboutView.loadUrl("file:///android_asset/about.html");


    }
}
