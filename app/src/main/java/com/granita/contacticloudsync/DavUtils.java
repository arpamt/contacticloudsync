/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.granita.contacticloudsync;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DavUtils {
	public static final int calendarGreen = 0xFFC3EA6E;

	public static int CalDAVtoARGBColor(String davColor) {
		int color = calendarGreen;		// fallback: "green"
		if (davColor != null) {
			Pattern p = Pattern.compile("#?(\\p{XDigit}{6})(\\p{XDigit}{2})?");
			Matcher m = p.matcher(davColor);
			if (m.find()) {
				int color_rgb = Integer.parseInt(m.group(1), 16);
				int color_alpha = m.group(2) != null ? (Integer.parseInt(m.group(2), 16) & 0xFF) : 0xFF;
				color = (color_alpha << 24) | color_rgb;
			} else
				Constants.log.warn("Couldn't parse color " + davColor + ", green");
		}
		return color;
	}
}
