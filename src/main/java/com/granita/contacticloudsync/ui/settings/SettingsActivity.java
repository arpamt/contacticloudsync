/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.granita.contacticloudsync.ui.settings;

import android.accounts.Account;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.granita.contacticloudsync.R;

public class SettingsActivity extends Activity {
	private final static String KEY_SELECTED_ACCOUNT = "selected_account";

	boolean tabletLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_activity);

		tabletLayout = findViewById(R.id.right_pane) != null;
		if (tabletLayout) {
			SettingsScopeFragment scope = (SettingsScopeFragment)getFragmentManager().findFragmentById(R.id.settings_scope);
			scope.setLayout(true);
		}
	}

	void showAccountSettings(Account account) {
		if (tabletLayout) {
			AccountFragment fragment = new AccountFragment();

			Bundle args = new Bundle(1);
			args.putParcelable(AccountFragment.ARG_ACCOUNT, account);
			fragment.setArguments(args);

			getFragmentManager().beginTransaction()
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.replace(R.id.right_pane, fragment)
					.commit();
		} else {  // phone layout
			Intent intent = new Intent(getApplicationContext(), AccountActivity.class);
			intent.putExtra(AccountActivity.EXTRA_ACCOUNT, account);
			startActivity(intent);
		}
	}
}
