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

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.test.InstrumentationTestCase;
import android.util.Log;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;

import at.bitfire.ical4android.impl.TestCalendar;
import at.bitfire.ical4android.impl.TestEvent;
import lombok.Cleanup;

public class AndroidEventTest extends InstrumentationTestCase {
    private static final String TAG = "ical4android.CalTest";

    private static final TimeZone tzVienna = DateUtils.tzRegistry.getTimeZone("Europe/Vienna");

    Context context;

    ContentProviderClient provider;
    final Account testAccount = new Account("ical4android.AndroidEventTest", CalendarContract.ACCOUNT_TYPE_LOCAL);

    Uri calendarUri;
    AndroidCalendar calendar;


    // helpers

    private Uri syncAdapterURI(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, testAccount.type)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, testAccount.name)
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").
                        build();
    }


    // initialization

    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    protected void setUp() throws RemoteException, FileNotFoundException, CalendarStorageException {
        context = getInstrumentation().getTargetContext();
        provider = context.getContentResolver().acquireContentProviderClient(CalendarContract.AUTHORITY);
        assertNotNull("Couldn't access calendar provider", provider);

        prepareTestCalendar();
    }

    private void prepareTestCalendar() throws RemoteException, FileNotFoundException, CalendarStorageException {
        calendar = TestCalendar.findOrCreate(testAccount, provider);
        assertNotNull("Coulnd't find/create test calendar", calendar);

        calendarUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calendar.getId());
        Log.i(TAG, "Prepared test calendar " + calendarUri);
    }

    @Override
    protected void tearDown() throws CalendarStorageException {
        Log.i(TAG, "Deleting test calendar");

        // all events should have been removed
        assertEquals(0, calendar.queryEvents(null, null).length);

        // remove test calendar, too
        calendar.delete();
    }


    // tests

    public void testAddEvent() throws URISyntaxException, ParseException, FileNotFoundException, CalendarStorageException {
        // build and write recurring event to calendar provider
        Event event = new Event();
        event.uid = "sample1@testAddEvent";
        event.summary = "Sample event";
        event.description = "Sample event with date/time";
        event.location = "Sample location";
        event.dtStart = new DtStart("20150501T120000", tzVienna);
        event.dtEnd = new DtEnd("20150501T130000", tzVienna);
        event.organizer = new Organizer(new URI("mailto:organizer@example.com"));
        event.rRule = new RRule("FREQ=DAILY;COUNT=10");
        event.forPublic = false;
        event.status = Status.VEVENT_CONFIRMED;
        assertFalse(event.isAllDay());

        // TODO test rDates, exDate, duration

        // set an alarm one day, two hours, three minutes and four seconds before begin of event
        event.getAlarms().add(new VAlarm(new Dur(-1, -2, -3, -4)));

        // add two attendees
        event.getAttendees().add(new Attendee(new URI("mailto:user1@example.com")));
        event.getAttendees().add(new Attendee(new URI("mailto:user2@example.com")));

        // add exception with alarm and attendee
        Event exception = new Event();
        exception.recurrenceId = new RecurrenceId("20150502T120000", tzVienna);
        exception.summary = "Exception for sample event";
        exception.dtStart = new DtStart("20150502T140000", tzVienna);
        exception.dtEnd = new DtEnd("20150502T150000", tzVienna);
        exception.getAlarms().add(new VAlarm(new Dur(-2, -3, -4, -5)));
        exception.getAttendees().add(new Attendee(new URI("mailto:only.here@today")));
        event.getExceptions().add(exception);

        // add to calendar
        Uri uri = new TestEvent(calendar, event).add();
        assertNotNull("Couldn't add event", uri);

        // read and parse event from calendar provider
        @Cleanup("delete") TestEvent testEvent = new TestEvent(calendar, ContentUris.parseId(uri));
        assertNotNull("Inserted event is not here", testEvent);
        Event event2 = testEvent.getEvent();
        assertNotNull("Inserted event is empty", event2);

        // compare with original event
        assertEquals(event.summary, event2.summary);
        assertEquals(event.description, event2.description);
        assertEquals(event.location, event2.location);
        assertEquals(event.dtStart, event2.dtStart);
        assertFalse(event2.isAllDay());
        assertEquals(event.organizer, event2.organizer);
        assertEquals(event.rRule, event2.rRule);
        assertEquals(event.forPublic, event2.forPublic);
        assertEquals(event.status, event2.status);

        // compare alarm
        assertEquals(1, event2.getAlarms().size());
        VAlarm alarm2 = event2.getAlarms().get(0);
        assertEquals(event.summary, alarm2.getDescription().getValue());  // should be built from event title
        assertEquals(new Dur(0, 0, -(24 * 60 + 60 * 2 + 3), 0), alarm2.getTrigger().getDuration());   // calendar provider stores trigger in minutes

        // compare attendees
        assertEquals(2, event2.getAttendees().size());
        assertEquals(event.getAttendees().get(0).getCalAddress(), event2.getAttendees().get(0).getCalAddress());
        assertEquals(event.getAttendees().get(1).getCalAddress(), event2.getAttendees().get(1).getCalAddress());

        // compare exception
        assertEquals(1, event2.getExceptions().size());
        Event exception2 = event2.getExceptions().get(0);
        assertEquals(exception.recurrenceId.getDate(), exception2.recurrenceId.getDate());
        assertEquals(exception.summary, exception2.summary);
        assertEquals(exception.dtStart, exception2.dtStart);
        assertEquals(exception.dtEnd, exception2.dtEnd);

        // compare exception alarm
        assertEquals(1, exception2.getAlarms().size());
        alarm2 = exception2.getAlarms().get(0);
        assertEquals(exception.summary, alarm2.getDescription().getValue());
        assertEquals(new Dur(0, 0, -(2 * 24 * 60 + 60 * 3 + 4), 0), alarm2.getTrigger().getDuration());   // calendar provider stores trigger in minutes

        // compare exception attendee
        assertEquals(1, exception2.getAttendees().size());
        assertEquals(exception.getAttendees().get(0).getCalAddress(), exception2.getAttendees().get(0).getCalAddress());
    }

    public void testUpdateEvent() throws URISyntaxException, ParseException, FileNotFoundException, CalendarStorageException {
        // add test event without reminder
        Event event = new Event();
        event.uid = "sample1@testAddEvent";
        event.summary = "Sample event";
        event.dtStart = new DtStart("20150502T120000Z");
        event.dtEnd = new DtEnd("20150502T130000Z");
        event.organizer = new Organizer(new URI("mailto:organizer@example.com"));
        Uri uri = new TestEvent(calendar, event).add();

        // update test event in calendar
        @Cleanup("delete") TestEvent testEvent = new TestEvent(calendar, ContentUris.parseId(uri));
        event = testEvent.getEvent();
        event.summary = "Updated event";
        // add data rows
        event.getAlarms().add(new VAlarm(new Dur(-1, -2, -3, -4)));
        event.getAttendees().add(new Attendee(new URI("mailto:user@example.com")));
        uri = testEvent.update(event);

        // read again and verify result
        testEvent = new TestEvent(calendar, ContentUris.parseId(uri));
        Event updatedEvent = testEvent.getEvent();
        assertEquals(event.summary, updatedEvent.summary);
        assertEquals(1, updatedEvent.getAlarms().size());
        assertEquals(1, updatedEvent.getAttendees().size());
    }

    public void testBuildAllDayEntry() throws ParseException, FileNotFoundException, CalendarStorageException {
        // add all-day event to calendar provider
        Event event = new Event();
        event.summary = "All-day event";
        event.description = "All-day event for testing";
        event.location = "Sample location testBuildAllDayEntry";
        event.dtStart = new DtStart(new Date("20150501"));
        event.dtEnd = new DtEnd(new Date("20150501"));  // "events on same day" are not understood by Android, so it should be changed to next day
        assertTrue(event.isAllDay());
        Uri uri = new TestEvent(calendar, event).add();
        assertNotNull("Couldn't add event", uri);

        // read again and verify result
        @Cleanup("delete") TestEvent testEvent = new TestEvent(calendar, ContentUris.parseId(uri));
        Event event2 = testEvent.getEvent();
        // compare with original event
        assertEquals(event.summary, event2.summary);
        assertEquals(event.description, event2.description);
        assertEquals(event.location, event2.location);
        assertEquals(event.dtStart, event2.dtStart);
        assertEquals(event.dtEnd.getDate(), new Date("20150502"));
        assertTrue(event2.isAllDay());
    }

}
