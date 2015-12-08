package org.almende.proheal.loopback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public class UserRepository extends com.strongloop.android.loopback.UserRepository<User> {

	public interface LoginCallback extends com.strongloop.android.loopback.UserRepository.LoginCallback<User> {
	}

	public UserRepository() {
		super("user", null, User.class);
	}

	public boolean isLoggedIn() {
		try {
			if (getCurrentUserId() == null) {
				return false;
			} else {
				return getCurrentUserId() != new JSONArray("[null]").get(0);
			}
		} catch (JSONException e) {
			return false;
		}
	}

}
