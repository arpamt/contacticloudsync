/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.granita.contacticloudsync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.granita.contacticloudsync.ui.settings.SettingsActivity;
import com.granita.contacticloudsync.ui.setup.AddAccountActivity;
import com.granita.contacticloudsync.util.IabHelper;
import com.granita.contacticloudsync.util.IabResult;
import com.granita.contacticloudsync.util.Inventory;
import com.granita.contacticloudsync.util.Purchase;

public class MainActivity extends Activity {

    Button adButton;
    View fragment2;
    Button newAccountButton;
    //Start premium features
    private static final String TAG = "this is a tag";
    IabHelper mHelper;
    static final String ITEM_SKU = "com.granita.contacticloudsync";
    boolean mIsPremium = false;

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {

            if (result.isFailure()) {
                // handle error here
            }
            else {
                // does the user have the premium upgrade?
                mIsPremium = inventory.hasPurchase(ITEM_SKU);
                if(mIsPremium) {
                    SharedPreferences sharedPref = getSharedPreferences("localPreferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(getString(R.string.HIDE_AD_BOOLEAN), true);
                    editor.commit();
                }
                else
                {
                    SharedPreferences sharedPref = getSharedPreferences("localPreferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(getString(R.string.HIDE_AD_BOOLEAN), false);
                    editor.commit();
                }

            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase)
        {
            if (result.isFailure()) {
                // Handle error
                return;
            }
            else if (purchase.getSku().equals(ITEM_SKU)) {
                SharedPreferences sharedPref = getSharedPreferences("localPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.HIDE_AD_BOOLEAN), true);
                editor.commit();
                adButton = (Button)findViewById(R.id.button3);
                fragment2 = findViewById(R.id.fragment2);
                adButton.setVisibility(View.GONE);
                fragment2.setVisibility(View.GONE);
            }

        }
    };



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    //End premium features

    //custom start
    @Override
    public void onStart() {
        super.onStart();
        final SharedPreferences settings =
                getSharedPreferences("localPreferences", MODE_PRIVATE);
        if (settings.getBoolean("isFirstRun", true)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cookies")
                    .setMessage("We use device identifiers to personalise content and ads, to provide social media features and to analyse our traffic. We also share such identifiers and other information from your device with our social media, advertising and analytics partners.")
                    .setNeutralButton("Close message", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settings.edit().putBoolean("isFirstRun", false).commit();
                        }
                    }).show();
        }
    }

    //custom end
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Tracker t = ((SyncForICloud) getApplication()).getTracker(
                SyncForICloud.TrackerName.APP_TRACKER);
        t.setScreenName("Sync for iCloud Contacts: Home screen");
        t.send(new HitBuilders.AppViewBuilder().build());

        //Start premium features
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApj1h7FufGw/Q5fQxcZz4kJxR7NcU67+Srp7kDjwyBZCBlqvw4O9tbNAo9rdqNBjaL/tQBuian0VSFO9i14qo3nbasRBrusI/+sZO1m6RJimC6hY1xv6yyidSR00JlxGrIO7QtR9tewejsrOlckz8Wh+D4mRP+g+tS0LY6TOExBKuKuAWRzGWA5NbD2TmIf25/2Gnk/e5q3PQBCxZ1WS7q8ak/YJkWpuqtXPfvAQ1ZujanTBzLQB0/ogvhy9t0XcVChUAZXvPGOQsXXX5QZmhZ5f1wyifg65TY0HjpoXT5I+ecQUdr+ChZzYkCrHltc9yn5hoTw0b6r5DOTEF46U1AwIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "In-app Billing setup failed: " +
                            result);
                } else {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                    Log.d(TAG, "In-app Billing is set up OK");
                }
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("localPreferences", Context.MODE_PRIVATE);
        if(sharedPref.getBoolean(getString(R.string.HIDE_AD_BOOLEAN), false)){
            adButton = (Button)findViewById(R.id.button3);
            adButton.setVisibility(View.GONE);
        }

        //End premium features
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.onAboutTitle: {
                Intent intent = new Intent(this, About.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onAboutTitle() {

    }

    //Start premium features
    public void onClickRemoveAds(View view){


        mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001,
                mPurchaseFinishedListener, "mypurchasetoken");
    }
    //End premium features

    public void onClickiCloud(View view) {
        Intent intent = new Intent(this, InternetBrowser.class);
        startActivity(intent);
    }

    public void onClickAddCalendar(View view) {
        Intent intent = new Intent(this, AddAccountActivity.class);
        startActivity(intent);
    }

    public void onClickModifyCalendar(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickMail(View view)
    {
        String packageName = "com.granita.icloudmail";
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
        {
	        /* we found the activity now start the activity */
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else
        {
	        /* bring user to the market or let them choose an app? */
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id="+packageName));
            startActivity(intent);
        }
    }

    public void onClickTasks(View view) {
        String packageName = "com.granita.tasks";
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
        {
	        /* we found the activity now start the activity */
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else {
	        /* bring user to the market or let them choose an app? */
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        }
    }


    public void onClickContacts(View view) {
        String packageName = "com.granita.caldavsync";
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
        {
	        /* we found the activity now start the activity */
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else
        {
	        /* bring user to the market or let them choose an app? */
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id="+packageName));
            startActivity(intent);
        }
    }

    public void onClickTapItFast(View view) {
        String packageName = "com.danthe.tapitfast";
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
        {
	        /* we found the activity now start the activity */
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else
        {
	        /* bring user to the market or let them choose an app? */
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id="+packageName));
            startActivity(intent);
        }
    }
}
