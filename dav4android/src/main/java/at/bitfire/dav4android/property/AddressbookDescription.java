/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.dav4android.property;

import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import at.bitfire.dav4android.Constants;
import at.bitfire.dav4android.Property;
import at.bitfire.dav4android.PropertyFactory;
import at.bitfire.dav4android.XmlUtils;
import lombok.ToString;

@ToString
public class AddressbookDescription implements Property {
    public static final Property.Name NAME = new Name(XmlUtils.NS_CARDDAV, "addressbook-description");

    public String description;


    public static class Factory implements PropertyFactory {
        @Override
        public Name getName() {
            return NAME;
        }

        @Override
        public AddressbookDescription create(XmlPullParser parser) {
            AddressbookDescription description = new AddressbookDescription();

            try {
                // <!ELEMENT addressbook-description (#PCDATA)>
                final int depth = parser.getDepth();

                int eventType = parser.getEventType();
                while (!(eventType == XmlPullParser.END_TAG && parser.getDepth() == depth)) {
                    if (eventType == XmlPullParser.TEXT && parser.getDepth() == depth)
                        description.description = parser.getText();
                    eventType = parser.next();
                }
            } catch(XmlPullParserException|IOException e) {
                Constants.log.error("Couldn't parse <addressbook-description>", e);
                return null;
            }

            return description;
        }
    }
}
