/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
	public static final String
		//custom start
		ACCOUNT_TYPE = "com.granita.contacticloudsync",
		WEB_URL_MAIN = "",
		WEB_URL_HELP = "";
		//custom end

    public static final Logger log = LoggerFactory.getLogger("contacticloudsync");

    // notification IDs
    public final static int
            NOTIFICATION_ANDROID_VERSION_UPDATED = 0,
            NOTIFICATION_ACCOUNT_SETTINGS_UPDATED = 1,
            NOTIFICATION_CONTACTS_SYNC = 10,
            NOTIFICATION_CALENDAR_SYNC = 11,
            NOTIFICATION_TASK_SYNC = 12;

}
