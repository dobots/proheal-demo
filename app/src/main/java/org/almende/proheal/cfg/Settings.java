package org.almende.proheal.cfg;

import android.content.Context;
import android.content.SharedPreferences;

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
public class Settings {

	private static final String SHARED_PREFS = "settings";

	private static final String PRESENCE_THRESHOLD_KEY = "presenceThresholdKey";

	private static Settings ourInstance;

	private final Context _context;
	private final SharedPreferences _sharedPreferences;

	private int _presenceThreshold;

	public static Settings getInstance(Context context) {

		if (ourInstance == null) {
			ourInstance = new Settings(context);
		}

		return ourInstance;
	}

	private Settings(Context context) {
		this._context = context;

		_sharedPreferences = _context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
		readPersistentSettings();
	}

	public int getPresenceThreshold() {
		return _presenceThreshold;
	}

	public void setPresenceThreshold(int threshold) {
		_presenceThreshold = threshold;
	}

	public void readPersistentSettings() {
		_presenceThreshold = _sharedPreferences.getInt(PRESENCE_THRESHOLD_KEY, Config.PRESENCE_THRESHOLD);
	}

	public void writePeristentSettings() {
		final SharedPreferences.Editor editor = _sharedPreferences.edit();
		editor.putFloat(PRESENCE_THRESHOLD_KEY, _presenceThreshold);
		editor.commit();
	}

	public void clearSettings() {
		final SharedPreferences.Editor editor = _sharedPreferences.edit();
		editor.clear().commit();
		_presenceThreshold = Config.PRESENCE_THRESHOLD;
	}

}
