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

	private static final String SHARED_PREFS = "prefs";
	private static final String PROPERTY_LOGIN_STATE = "loginState";

	private static Settings ourInstance;

	private final Context _context;

	private boolean _loginState = false;

	public static Settings getInstance(Context context) {

		if (ourInstance == null) {
			ourInstance = new Settings(context);
		}

		return ourInstance;
	}

	private Settings(Context context) {
		this._context = context;

		loadLoginState();
	}

	private void saveLoginState() {
		final SharedPreferences.Editor editor = _context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE).edit();
		editor.putBoolean(PROPERTY_LOGIN_STATE, _loginState);
		editor.commit();
	}

	private void loadLoginState() {
//		_loginState = false;
		_loginState = _context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE).getBoolean(PROPERTY_LOGIN_STATE, false);
	}

	public boolean isLoggedIn() {
		return _loginState;
	}

	public void setLoginState(boolean newLoginState) {
		this._loginState = newLoginState;
		saveLoginState();
	}

}
