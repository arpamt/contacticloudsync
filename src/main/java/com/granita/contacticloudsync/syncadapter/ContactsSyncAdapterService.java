/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.syncadapter;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.granita.contacticloudsync.resource.CardDavAddressBook;
import com.granita.contacticloudsync.resource.LocalAddressBook;
import com.granita.contacticloudsync.resource.LocalCollection;
import com.granita.contacticloudsync.resource.RemoteCollection;

public class ContactsSyncAdapterService extends Service {
	private static ContactsSyncAdapter syncAdapter;

	
	@Override
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new ContactsSyncAdapter(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		syncAdapter.close();
		syncAdapter = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder();
	}
	

	private static class ContactsSyncAdapter extends DavSyncAdapter {
		private final static String TAG = "contacticloudsync.ContactsSyncAdapter";

		
		private ContactsSyncAdapter(Context context) {
			super(context);
		}

		@Override
		protected Map<LocalCollection<?>, RemoteCollection<?>> getSyncPairs(Account account, ContentProviderClient provider) {
			AccountSettings settings = new AccountSettings(getContext(), account);
			String	userName = settings.getUserName(),
					password = settings.getPassword();
			boolean preemptive = settings.getPreemptiveAuth();

			String addressBookURL = settings.getAddressBookURL();
			if (addressBookURL == null)
				return null;
			
			try {
				LocalCollection<?> database = new LocalAddressBook(account, provider, settings);
				RemoteCollection<?> dav = new CardDavAddressBook(settings, httpClient, addressBookURL, userName, password, preemptive);
				
				Map<LocalCollection<?>, RemoteCollection<?>> map = new HashMap<LocalCollection<?>, RemoteCollection<?>>();
				map.put(database, dav);
				
				return map;
			} catch (URISyntaxException ex) {
				Log.e(TAG, "Couldn't build address book URI", ex);
			}
			
			return null;
		}
	}
}
