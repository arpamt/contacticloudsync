/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.webdav;

public class DavNoMultiStatusException extends DavException {
	private static final long serialVersionUID = -3600405724694229828L;
	
	private final static String message = "207 Multi-Status expected but not received";
	
	public DavNoMultiStatusException() {
		super(message);
	}
}
