/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.granita.contacticloudsync.ui.settings;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.CalendarContract;

import com.granita.contacticloudsync.R;
import com.granita.contacticloudsync.resource.LocalAddressBook;
import com.granita.contacticloudsync.syncadapter.AccountSettings;
import ezvcard.VCardVersion;

public class AccountFragment extends PreferenceFragment {
	final static String ARG_ACCOUNT = "account";

	Account account;

    //custom code start
    protected static Uri calendarsURI(Account account) {
        return CalendarContract.Calendars.CONTENT_URI.buildUpon().appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings_account_prefs);

		account = getArguments().getParcelable(ARG_ACCOUNT);
		readFromAccount();
	}
    //custom code end

	public void readFromAccount() {
		final AccountSettings settings = new AccountSettings(getActivity(), account);


        //custom code start
        PreferenceCategory prefCat = (PreferenceCategory)findPreference("cal_list");
        EditTextPreference preference = new EditTextPreference(getActivity());
        preference.setKey(settings.getAddressBookURL());
        preference.setTitle(settings.getAddressBookURL());
        preference.setSummary(settings.getAddressBookURL());
        preference.setText(settings.getAddressBookURL());
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                     @Override
                                                     public boolean onPreferenceChange(Preference preference1, Object newValue) {
                                                         settings.setAddressBookURL((String)newValue);
                                                         readFromAccount();
                                                         return true;
                                                     }
                                                 });
        preference.setPersistent(false);
        prefCat.addPreference(preference);
        //custom code end


		// category: authentication
		final EditTextPreference prefUserName = (EditTextPreference)findPreference("username");
		prefUserName.setSummary(settings.getUserName());
		prefUserName.setText(settings.getUserName());
		prefUserName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				settings.setUserName((String)newValue);
				readFromAccount();
				return true;
			}
		});

		final EditTextPreference prefPassword = (EditTextPreference)findPreference("password");
		prefPassword.setText(settings.getPassword());
		prefPassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				settings.setPassword((String)newValue);
				readFromAccount();
				return true;
			}
		});


        //custom start
        /*
		final SwitchPreference prefPreemptive = (SwitchPreference)findPreference("preemptive");
		prefPreemptive.setChecked(settings.getPreemptiveAuth());
		prefPreemptive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				settings.setPreemptiveAuth((Boolean)newValue);
				readFromAccount();
				return true;
			}
		});
		*/
        //custom end

		// category: synchronization
		final ListPreference prefSyncContacts = (ListPreference)findPreference("sync_interval_contacts");
		final Long syncIntervalContacts = settings.getContactsSyncInterval();
		if (syncIntervalContacts != null) {
			prefSyncContacts.setValue(syncIntervalContacts.toString());
			if (syncIntervalContacts == AccountSettings.SYNC_INTERVAL_MANUALLY)
				prefSyncContacts.setSummary(R.string.settings_sync_summary_manually);
			else
				prefSyncContacts.setSummary(getString(R.string.settings_sync_summary_periodically, syncIntervalContacts / 60));
			prefSyncContacts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					settings.setContactsSyncInterval(Long.parseLong((String)newValue));
					readFromAccount();
					return true;
				}
			});
		} else {
			prefSyncContacts.setEnabled(false);
			prefSyncContacts.setSummary(R.string.settings_sync_summary_not_available);
		}

        //custom start
        /*
		final ListPreference prefSyncCalendars = (ListPreference)findPreference("sync_interval_calendars");
		final Long syncIntervalCalendars = settings.getCalendarsSyncInterval();
		if (syncIntervalCalendars != null) {
			prefSyncCalendars.setValue(syncIntervalCalendars.toString());
			if (syncIntervalCalendars == AccountSettings.SYNC_INTERVAL_MANUALLY)
				prefSyncCalendars.setSummary(R.string.settings_sync_summary_manually);
			else
				prefSyncCalendars.setSummary(getString(R.string.settings_sync_summary_periodically, syncIntervalCalendars / 60));
			prefSyncCalendars.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					settings.setCalendarsSyncInterval(Long.parseLong((String)newValue));
					readFromAccount();
					return true;
				}
			});
		} else {
			prefSyncCalendars.setEnabled(false);
			prefSyncCalendars.setSummary(R.string.settings_sync_summary_not_available);
		}
		*/
        //custom end

		// category: address book
		final CheckBoxPreference prefVCard4 = (CheckBoxPreference) findPreference("vcard4_support");
		if (settings.getAddressBookURL() != null) {     // does this account even have an address book?
			final VCardVersion vCardVersion = settings.getAddressBookVCardVersion();
			prefVCard4.setChecked(vCardVersion == VCardVersion.V4_0);
			prefVCard4.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// don't change the value (it's not really a setting, only a display)
					return false;
				}
			});
		} else {
			// account doesn't have an adress book, disable contact settings
			prefVCard4.setEnabled(false);
		}

	}
}
