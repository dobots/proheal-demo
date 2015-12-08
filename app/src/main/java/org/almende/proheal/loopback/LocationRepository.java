package org.almende.proheal.loopback;

import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.strongloop.android.loopback.ModelRepository;
import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.JsonArrayParser;
import com.strongloop.android.loopback.callbacks.ObjectCallback;
import com.strongloop.android.remoting.JsonUtil;
import com.strongloop.android.remoting.adapters.Adapter;
import com.strongloop.android.remoting.adapters.RestContract;
import com.strongloop.android.remoting.adapters.RestContractItem;

import org.almende.proheal.cfg.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
public class LocationRepository extends ModelRepository<Location> {

	private static final String TAG = LocationRepository.class.getCanonicalName();

	public LocationRepository() {
		super("location", Location.class);
	}

	public void findBeaconsForId(Object id, final ObjectCallback<Beacon> callback) {

		RestAdapter restAdapter = getRestAdapter();
		final BeaconRepository beaconRepository = restAdapter.createRepository(BeaconRepository.class);

		Map<String, Object> filter = new HashMap<>();
		filter.put("where", ImmutableMap.of("id", id));
		filter.put("include", "beacons");
		invokeStaticMethod("all", ImmutableMap.of("filter", filter),
				new Adapter.JsonArrayCallback() {
					@Override
					public void onSuccess(JSONArray response) {
						Log.i(TAG, "success: " + response);
						try {
							JSONObject beaconJson = response.getJSONObject(0).getJSONArray("beacons").getJSONObject(0);
							Beacon beacon = beaconRepository.createObject(JsonUtil.fromJson(beaconJson));
							callback.onSuccess(beacon);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable t) {
						callback.onError(t);
					}
				}
		);
	}

	public boolean belongsTo(Location location, User user) {

		return false;



	}

//	@Override
//	public RestContract createContract() {
//		RestContract contract = super.createContract();
//
//		return contract;
//	}
}
