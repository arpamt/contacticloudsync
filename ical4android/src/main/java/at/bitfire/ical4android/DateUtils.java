/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.ical4android;

import android.util.Log;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DateListProperty;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;

public class DateUtils {
    private final static String TAG = "ical4android.DateUtils";

    public final static TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();

    static {
        // disable automatic time-zone updates (causes unwanted network traffic)
        System.setProperty("net.fortuna.ical4j.timezone.update.enabled", "false");
    }


    // time zones

    public static String findAndroidTimezoneID(String tzID) {
        String deviceTZ = null;
        String availableTZs[] = SimpleTimeZone.getAvailableIDs();

        // first, try to find an exact match (case insensitive)
        for (String availableTZ : availableTZs)
            if (availableTZ.equalsIgnoreCase(tzID)) {
                deviceTZ = availableTZ;
                break;
            }

        // if that doesn't work, try to find something else that matches
        if (deviceTZ == null) {
            for (String availableTZ : availableTZs)
                if (StringUtils.indexOfIgnoreCase(tzID, availableTZ) != -1) {
                    deviceTZ = availableTZ;
                    Log.w(TAG, "Couldn't find system time zone \"" + tzID + "\", assuming " + deviceTZ);
                    break;
                }
        }

        // if that doesn't work, use UTC as fallback
        if (deviceTZ == null) {
            final String defaultTZ = TimeZone.getDefault().getID();
            Log.w(TAG, "Couldn't find system time zone \"" + tzID +"\", using system default (" + defaultTZ + ") as fallback");
            deviceTZ = defaultTZ;
        }

        return deviceTZ;
    }

    public static VTimeZone parseVTimeZone(String timezoneDef) {
        CalendarBuilder builder = new CalendarBuilder(tzRegistry);
        try {
            Calendar cal = builder.build(new StringReader(timezoneDef));
            return (VTimeZone)cal.getComponent(VTimeZone.VTIMEZONE);
        } catch (IOException|ParserException e) {
            Constants.log.warn("Couldn't parse timezone definition");
            return null;
        }
    }


    // recurrence sets

    /**
     * Concatenates, if necessary, multiple RDATE/EXDATE lists and converts them to
     * a formatted string which Android calendar provider can process.
     * Android expects this format: "[TZID;]date1,date2,date3" where date is "yyyymmddThhmmss" (when
     * TZID is given) or "yyyymmddThhmmssZ". We don't use the TZID format here because then we're limited
     * to one time-zone, while an iCalendar may contain multiple EXDATE/RDATE lines with different time zones.
     * @param dates		one more more lists of RDATE or EXDATE
     * @param allDay    indicates whether the event is an all-day event or not
     * @return			formatted string for Android calendar provider:
     *                  - in case of all-day events, all dates/times are returned as yyyymmddT000000Z
     *                  - in case of timed events, all dates/times are returned as UTC time: yyyymmddThhmmssZ
     */
    public static String recurrenceSetsToAndroidString(List<? extends DateListProperty> dates, boolean allDay) throws ParseException {
        List<String> strDates = new LinkedList<>();

		/*        rdate/exdate: DATE                                DATE_TIME
		    all-day             store as ...T000000Z                cut off time and store as ...T000000Z
		    event with time     (ignored)                           store as ...ThhmmssZ
		*/
        final DateFormat dateFormatUtcMidnight = new SimpleDateFormat("yyyyMMdd'T'000000'Z'", Locale.US);

        for (DateListProperty dateListProp : dates) {
            final Value type = dateListProp.getDates().getType();

            if (Value.DATE_TIME.equals(type)) {         // DATE-TIME values will be stored in UTC format for Android
                if (allDay) {
                    DateList dateList = dateListProp.getDates();
                    for (Date date : (Iterable<Date>)dateList)
                        strDates.add(dateFormatUtcMidnight.format(date));
                } else {
                    dateListProp.setUtc(true);
                    strDates.add(dateListProp.getValue());
                }

            } else if (Value.DATE.equals(type))       // DATE values have to be converted to DATE-TIME <date>T000000Z for Android
                for (Date date : (Iterable<Date>)dateListProp.getDates())
                    strDates.add(dateFormatUtcMidnight.format(date));
        }
        return StringUtils.join(strDates, ",");
    }

    /**
     * Takes a formatted string as provided by the Android calendar provider and returns a DateListProperty
     * constructed from these values.
     * @param dbStr     formatted string from Android calendar provider (RDATE/EXDATE field)
     *                  expected format: "[TZID;]date1,date2,date3" where date is "yyyymmddThhmmss[Z]"
     * @param type      subclass of DateListProperty, e.g. RDate or ExDate
     * @param allDay    true: list will contain DATE values; false: list will contain DATE_TIME values
     * @return          instance of "type" containing the parsed dates/times from the string
     */
    public static DateListProperty androidStringToRecurrenceSet(String dbStr, Class<? extends DateListProperty> type, boolean allDay) throws ParseException {
        // 1. split string into time zone and actual dates
        TimeZone timeZone;
        String datesStr;
        final int limiter = dbStr.indexOf(';');
        if (limiter != -1) {    // TZID given
            timeZone = DateUtils.tzRegistry.getTimeZone(dbStr.substring(0, limiter));
            datesStr = dbStr.substring(limiter + 1);
        } else {
            timeZone = null;
            datesStr = dbStr;
        }

        // 2. process date string and generate list of DATEs or DATE-TIMEs
        DateList dateList;
        if (allDay) {
            dateList = new DateList(Value.DATE);
            for (String s: StringUtils.split(datesStr, ','))
                dateList.add(new Date(new DateTime(s)));
        } else {
            dateList = new DateList(datesStr, Value.DATE_TIME, timeZone);
            if (timeZone == null)
                dateList.setUtc(true);
        }

        // 3. generate requested DateListProperty (RDate/ExDate) from list of DATEs or DATE-TIMEs
        DateListProperty list;
        try {
            list = (DateListProperty)type.getDeclaredConstructor(new Class[] { DateList.class } ).newInstance(dateList);
            if (dateList.getTimeZone() != null)
                list.setTimeZone(dateList.getTimeZone());
        } catch (Exception e) {
            throw new ParseException("Couldn't create date/time list by reflection", -1);
        }

        return list;
    }

}
