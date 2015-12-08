package org.almende.proheal.cfg;

/**
 * Copyright (c) 2015 Dominik Egger <dominik@dobots.nl>. All rights reserved.
 * <p/>
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3, as
 * published by the Free Software Foundation.
 * <p/>
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * <p/>
 * Created on 7-12-15
 *
 * @author Dominik Egger
 */
public class Config {

	public static final boolean DEBUG = true;


	public static final String REST_API_URL = "http://10.10.1.173:3000/api";
//		public static final String REST_API_URL = "http://crownstone-cloud.herokuapp.com/api";

	public static final int PRESENCE_THRESHOLD = -70;

	// scan for 1 second every 3 seconds
	public static final int LOW_SCAN_INTERVAL = 2000; // milli seconds scanning
	public static final int LOW_SCAN_PAUSE = 5000; //  milli seconds pause

	public static final int GUI_UPDATE_INTERVAL = 500;

	public static final long WATCHDOG_INTERVAL = 5000;
}
