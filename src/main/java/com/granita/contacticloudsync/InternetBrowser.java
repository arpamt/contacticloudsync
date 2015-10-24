package com.granita.contacticloudsync;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.granita.contacticloudsync.SyncForICloud.TrackerName;

public class InternetBrowser extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.internet_browser);
		
		Tracker t = ((SyncForICloud) getApplication()).getTracker(
	            TrackerName.APP_TRACKER);
		t.setScreenName("Sync for iCloud Contacts: Browser");
        t.send(new HitBuilders.AppViewBuilder().build());
		
		WebView webview = (WebView)findViewById(R.id.webView1);
		webview.loadUrl("http://www.me.com/");
		webview.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1944.0 Safari/537.36");
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
            	view.loadUrl(url);
                return true;
            }
        });
	}
}
