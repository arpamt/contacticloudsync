/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.resource;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.granita.contacticloudsync.R;
import com.granita.contacticloudsync.webdav.DavException;
import com.granita.contacticloudsync.webdav.DavHttpClient;
import com.granita.contacticloudsync.webdav.DavIncapableException;
import com.granita.contacticloudsync.webdav.HttpPropfind.Mode;
import com.granita.contacticloudsync.webdav.NotAuthorizedException;
import com.granita.contacticloudsync.webdav.WebDavResource;
import ezvcard.VCardVersion;

public class DavResourceFinder implements Closeable {
	private final static String TAG = "contacticloudsync.ResourceFinder";
	
	protected Context context;
	protected CloseableHttpClient httpClient;
	
	
	public DavResourceFinder(Context context) {
		this.context = context;
		
		// disable compression and enable network logging for debugging purposes 
		httpClient = DavHttpClient.create();
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
	
	
	public void findResources(ServerInfo serverInfo) throws URISyntaxException, DavException, HttpException, IOException {
		// CardDAV
		Log.i(TAG, "*** Starting CardDAV resource detection");
		WebDavResource principal = getCurrentUserPrincipal(serverInfo, "carddav");
		URI uriAddressBookHomeSet = null;
		try {
			principal.propfind(Mode.HOME_SETS);
			uriAddressBookHomeSet = principal.getAddressbookHomeSet();
		} catch (Exception e) {
			Log.i(TAG, "Couldn't find address-book home set", e);
		}
		if (uriAddressBookHomeSet != null) {
			Log.i(TAG, "Found address-book home set: " + uriAddressBookHomeSet);

			WebDavResource homeSetAddressBooks = new WebDavResource(principal, uriAddressBookHomeSet);
			if (checkHomesetCapabilities(homeSetAddressBooks, "addressbook")) {
				serverInfo.setCardDAV(true);
				homeSetAddressBooks.propfind(Mode.CARDDAV_COLLECTIONS);

				List<WebDavResource> possibleAddressBooks = new LinkedList<>();
				possibleAddressBooks.add(homeSetAddressBooks);
				if (homeSetAddressBooks.getMembers() != null)
					possibleAddressBooks.addAll(homeSetAddressBooks.getMembers());

				List<ServerInfo.ResourceInfo> addressBooks = new LinkedList<>();
				for (WebDavResource resource : possibleAddressBooks)
					if (resource.isAddressBook()) {
						Log.i(TAG, "Found address book: " + resource.getLocation().getPath());
						ServerInfo.ResourceInfo info = new ServerInfo.ResourceInfo(
							ServerInfo.ResourceInfo.Type.ADDRESS_BOOK,
							resource.isReadOnly(),
							resource.getLocation().toString(),
							resource.getDisplayName(),
							resource.getDescription(), resource.getColor()
						);

						VCardVersion version = resource.getVCardVersion();
						if (version == null)
							version = VCardVersion.V3_0;	// VCard 3.0 MUST be supported
						info.setVCardVersion(version);

						addressBooks.add(info);
					}
				serverInfo.setAddressBooks(addressBooks);
			} else
				Log.w(TAG, "Found address-book home set, but it doesn't advertise CardDAV support");
		}

		// CalDAV
		Log.i(TAG, "*** Starting CalDAV resource detection");
		principal = getCurrentUserPrincipal(serverInfo, "caldav");
		URI uriCalendarHomeSet = null;
		try {
			principal.propfind(Mode.HOME_SETS);
			uriCalendarHomeSet = principal.getCalendarHomeSet();
		} catch(Exception e) {
			Log.i(TAG, "Couldn't find calendar home set", e);
		}
		if (uriCalendarHomeSet != null) {
			Log.i(TAG, "Found calendar home set: " + uriCalendarHomeSet);

			WebDavResource homeSetCalendars = new WebDavResource(principal, uriCalendarHomeSet);
			if (checkHomesetCapabilities(homeSetCalendars, "calendar-access")) {
				serverInfo.setCalDAV(true);
				homeSetCalendars.propfind(Mode.CALDAV_COLLECTIONS);

				List<WebDavResource> possibleCalendars = new LinkedList<>();
				possibleCalendars.add(homeSetCalendars);
				if (homeSetCalendars.getMembers() != null)
					possibleCalendars.addAll(homeSetCalendars.getMembers());

				List<ServerInfo.ResourceInfo> calendars = new LinkedList<>();
				for (WebDavResource resource : possibleCalendars)
					if (resource.isCalendar()) {
						Log.i(TAG, "Found calendar: " + resource.getLocation().getPath());
						if (resource.getSupportedComponents() != null) {
							// CALDAV:supported-calendar-component-set available
							boolean supportsEvents = false;
							for (String supportedComponent : resource.getSupportedComponents())
								if (supportedComponent.equalsIgnoreCase("VEVENT"))
									supportsEvents = true;
							if (!supportsEvents) {	// ignore collections without VEVENT support
								Log.i(TAG, "Ignoring this calendar because of missing VEVENT support");
								continue;
							}
						}
						ServerInfo.ResourceInfo info = new ServerInfo.ResourceInfo(
							ServerInfo.ResourceInfo.Type.CALENDAR,
							resource.isReadOnly(),
							resource.getLocation().toString(),
							resource.getDisplayName(),
							resource.getDescription(), resource.getColor()
						);
						info.setTimezone(resource.getTimezone());
						calendars.add(info);
					}
				serverInfo.setCalendars(calendars);
			} else
				Log.w(TAG, "Found calendar home set, but it doesn't advertise CalDAV support");
		}

		if (!serverInfo.isCalDAV() && !serverInfo.isCardDAV())
			throw new DavIncapableException(context.getString(R.string.setup_neither_caldav_nor_carddav));

	}
	
	
	/**
	 * Finds the initial service URL from a given base URI (HTTP[S] or mailto URI, user name, password)
	 * @param serverInfo	User-given service information (including base URI, i.e. HTTP[S] URL+user name+password or mailto URI and password)
	 * @param serviceName	Service name ("carddav" or "caldav")
	 * @return				Initial service URL (HTTP/HTTPS), without user credentials
	 * @throws URISyntaxException when the user-given URI is invalid
	 * @throws MalformedURLException when the user-given URI is invalid
	 */
	public URI getInitialContextURL(ServerInfo serverInfo, String serviceName) throws URISyntaxException, MalformedURLException {
		String	scheme = null,
				domain;
		int		port = -1;
		String	path = "/";
		
		URI baseURI = serverInfo.getBaseURI();
		if ("mailto".equalsIgnoreCase(baseURI.getScheme())) {
			// mailto URIs
			String mailbox = serverInfo.getBaseURI().getSchemeSpecificPart();

			// determine service FQDN
			int pos = mailbox.lastIndexOf("@");
			if (pos == -1)
				throw new URISyntaxException(mailbox, "Missing @ sign");
			
			scheme = "https";
			domain = mailbox.substring(pos + 1);
			if (domain.isEmpty())
				throw new URISyntaxException(mailbox, "Missing domain name");
		} else {
			// HTTP(S) URLs
			scheme = baseURI.getScheme();
			domain = baseURI.getHost();
			port = baseURI.getPort();
			path = baseURI.getPath();
		}

		// try to determine FQDN and port number using SRV records
		try {
			String name = "_" + serviceName + "s._tcp." + domain;
			Log.d(TAG, "Looking up SRV records for " + name);
			Record[] records = new Lookup(name, Type.SRV).run();
			if (records != null && records.length >= 1) {
				SRVRecord srv = selectSRVRecord(records);
				
				scheme = "https";
				domain = srv.getTarget().toString(true);
				port = srv.getPort();
				Log.d(TAG, "Found " + serviceName + "s service for " + domain + " -> " + domain + ":" + port);
				
				if (port == 443)	// no reason to explicitly give the default port
					port = -1;

				// SRV record found, look for TXT record too (for initial context path)
				records = new Lookup(name, Type.TXT).run();
				if (records != null && records.length >= 1) {
					TXTRecord txt = (TXTRecord)records[0];
					for (Object o : txt.getStrings().toArray()) {
						String segment = (String)o;
						if (segment.startsWith("path=")) {
							path = segment.substring(5);
							Log.d(TAG, "Found initial context path for " + serviceName + " at " + domain + " -> " + path);
							break;
						}
					}
				}
			}
		} catch (TextParseException e) {
			throw new URISyntaxException(domain, "Invalid domain name");
		}
		
		return new URI(scheme, null, domain, port, path, null, null);
	}
	
	
	/**
	 * Detects the current-user-principal for a given WebDavResource. At first, /.well-known/ is tried. Only
	 * if no current-user-principal can be detected for the .well-known location, the given location of the resource
	 * is tried.
	 * @param serverInfo	Location that will be queried
	 * @param serviceName	Well-known service name ("carddav", "caldav")
	 * @return	            WebDavResource of current-user-principal for the given service, or null if it can't be found
	 * 
	 * TODO: If a TXT record is given, always use it instead of trying .well-known first
	 */
	WebDavResource getCurrentUserPrincipal(ServerInfo serverInfo, String serviceName) throws URISyntaxException, IOException, NotAuthorizedException {
		URI initialURL = getInitialContextURL(serverInfo, serviceName);
		if (initialURL != null) {
			Log.i(TAG, "Looking up principal URL for service " + serviceName + "; initial context: " + initialURL);

			// determine base URL (host name and initial context path)
			WebDavResource base = new WebDavResource(httpClient,
					initialURL,
					serverInfo.getUserName(), serverInfo.getPassword(), serverInfo.isAuthPreemptive());
			
			// look for well-known service (RFC 5785)
			try {
				WebDavResource wellKnown = new WebDavResource(base, "/.well-known/" + serviceName);
				wellKnown.propfind(Mode.CURRENT_USER_PRINCIPAL);
				if (wellKnown.getCurrentUserPrincipal() != null) {
					URI principal = wellKnown.getCurrentUserPrincipal();
					Log.i(TAG, "Principal URL found from Well-Known URI: " + principal);
					return new WebDavResource(wellKnown, principal);
				}
			} catch (NotAuthorizedException e) {
				Log.w(TAG, "Not authorized for well-known " + serviceName + " service detection", e);
				throw e;
			} catch (URISyntaxException e) {
				Log.e(TAG, "Well-known" + serviceName + " service detection failed because of invalid URIs", e);
			} catch (HttpException e) {
				Log.d(TAG, "Well-known " + serviceName + " service detection failed with HTTP error", e);
			} catch (DavException e) {
				Log.w(TAG, "Well-known " + serviceName + " service detection failed with unexpected DAV response", e);
			} catch (IOException e) {
				Log.e(TAG, "Well-known " + serviceName + " service detection failed with I/O error", e);
			}

			// fall back to user-given initial context path
			Log.d(TAG, "Well-known service detection failed, trying initial context path " + initialURL);
			try {
				base.propfind(Mode.CURRENT_USER_PRINCIPAL);
				if (base.getCurrentUserPrincipal() != null) {
					URI principal = base.getCurrentUserPrincipal();
					Log.i(TAG, "Principal URL found from initial context path: " + principal);
					return new WebDavResource(base, principal);
				}
			} catch (NotAuthorizedException e) {
				Log.e(TAG, "Not authorized for querying principal", e);
				throw e;
			} catch (HttpException e) {
				Log.e(TAG, "HTTP error when querying principal", e);
			} catch (DavException e) {
				Log.e(TAG, "DAV error when querying principal", e);
			}

			Log.i(TAG, "Couldn't find current-user-principal for service " + serviceName + ", assuming initial context path is principal path");
			return base;
		}
		return null;
	}
	
	public static boolean checkHomesetCapabilities(WebDavResource resource, String davCapability) throws URISyntaxException, IOException {
		// check for necessary capabilities
		try {
			resource.options();
			if (resource.supportsDAV(davCapability) &&
				resource.supportsMethod("PROPFIND"))		// check only for methods that MUST be available for home sets
				return true;
		} catch(HttpException e) {
			// for instance, 405 Method not allowed
		}
		return false;
	}
	
	
	SRVRecord selectSRVRecord(Record[] records) {
		if (records.length > 1)
			Log.w(TAG, "Multiple SRV records not supported yet; using first one");
		return (SRVRecord)records[0];
	}

}
