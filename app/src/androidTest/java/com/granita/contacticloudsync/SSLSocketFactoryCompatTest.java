/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.granita.contacticloudsync;

import android.os.Build;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

public class SSLSocketFactoryCompatTest extends TestCase {

    SSLSocketFactoryCompat factory = new SSLSocketFactoryCompat(null);
    MockWebServer server = new MockWebServer();

    @Override
    protected void setUp() throws Exception {
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.shutdown();
    }


    public void testUpgradeTLS() throws IOException {
        Socket s = factory.createSocket(server.getHostName(), server.getPort());
        assertTrue(s instanceof SSLSocket);

        SSLSocket ssl = (SSLSocket)s;
        assertFalse(org.apache.commons.lang3.ArrayUtils.contains(ssl.getEnabledProtocols(), "SSLv3"));
        assertTrue(org.apache.commons.lang3.ArrayUtils.contains(ssl.getEnabledProtocols(), "TLSv1"));

        if (Build.VERSION.SDK_INT >= 16) {
            assertTrue(org.apache.commons.lang3.ArrayUtils.contains(ssl.getEnabledProtocols(), "TLSv1.1"));
            assertTrue(org.apache.commons.lang3.ArrayUtils.contains(ssl.getEnabledProtocols(), "TLSv1.2"));
        }
    }

}
