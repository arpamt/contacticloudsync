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
import android.app.FragmentManager;
import android.os.Bundle;

import com.granita.contacticloudsync.R;

public class AccountActivity extends Activity {
	public static final String EXTRA_ACCOUNT = "account";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_account_activity);

		final FragmentManager fm = getFragmentManager();

		AccountFragment fragment = (AccountFragment)fm.findFragmentById(R.id.account_fragment);
		if (fragment == null) {
			fragment = new AccountFragment();
			final Bundle args = new Bundle(1);
			Account account = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
			args.putParcelable(AccountFragment.ARG_ACCOUNT, account);
			fragment.setArguments(args);

			getFragmentManager().beginTransaction()
				.add(R.id.account_fragment, fragment)
				.commit();
		}
	}
}
