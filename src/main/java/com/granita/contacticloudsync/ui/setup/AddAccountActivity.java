/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.ui.setup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.granita.contacticloudsync.Constants;
import com.granita.contacticloudsync.R;
import com.granita.contacticloudsync.SyncForICloud;

public class AddAccountActivity extends Activity {

    //custom start
    InterstitialAd interstitial;
    AdRequest adRequest;

    public void displayInterstitial() {

        if (interstitial.isLoaded()) {
            interstitial.show();
        }
        //else
        //Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
    }
    //custom end


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //custom start
        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId("ca-app-pub-3701736585096715/2313567381");

        // Create ad request.
        adRequest = new AdRequest.Builder().build();

        //custom end

		setContentView(R.layout.setup_add_account);

        //custom start
        Tracker t = ((SyncForICloud) getApplication()).getTracker(
                SyncForICloud.TrackerName.APP_TRACKER);
        t.setScreenName("Sync for iCloud Contacts: Add Account");
        t.send(new HitBuilders.AppViewBuilder().build());

        //custom end

		if (savedInstanceState == null) {	// first call
			getFragmentManager().beginTransaction()
				.add(R.id.right_pane, new LoginEmailFragment())
				.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.setup_add_account, menu);
		return true;
	}

	public void showHelp(MenuItem item) {
	}

}
