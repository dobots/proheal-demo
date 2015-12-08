package org.almende.proheal.loopback;

import com.strongloop.android.loopback.ModelRepository;
import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.JsonObjectParser;
import com.strongloop.android.loopback.callbacks.ObjectCallback;
import com.strongloop.android.remoting.JsonUtil;
import com.strongloop.android.remoting.adapters.Adapter;
import com.strongloop.android.remoting.adapters.RestContract;
import com.strongloop.android.remoting.adapters.RestContractItem;

import org.almende.proheal.cfg.Config;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

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
public class BeaconRepository extends ModelRepository<Beacon> {
	public BeaconRepository() {
		super("beacon", Beacon.class);
	}

	@Override
	public RestContract createContract() {
		RestContract contract = super.createContract();

		contract.addItem(new RestContractItem("/" + getNameForRestUrl() + "/findLocation", "GET"), getClassName() + ".findLocation");
		return contract;
	}

	public void findLocation(String address, final ObjectCallback<Location> callback) {

		RestAdapter restAdapter = getRestAdapter();
		final LocationRepository locationRepository = restAdapter.createRepository(LocationRepository.class);

		HashMap<String, String> params = new HashMap<>();
		params.put("address", address);
		invokeStaticMethod("findLocation", params, new Adapter.JsonObjectCallback() {
			@Override
			public void onSuccess(JSONObject response) {
				try {
					Location result = locationRepository.createObject(JsonUtil.fromJson(response.getJSONObject("location")));
//					Location result = new Location();
//					result.putAll(JsonUtil.fromJson(response.getJSONObject("location")));
					callback.onSuccess(result);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onError(Throwable t) {
				callback.onError(t);
			}
		});
	}

//			_beaconRepository.invokeStaticMethod("findLocation", params, new Adapter.Callback() {
//				@Override
//				public void onSuccess(String response) {
//					Log.i(TAG, "success: " + response);
//				}
//
//				@Override
//				public void onError(Throwable t) {
//					Log.e(TAG, "error: " + t);
//				}
//			});
}
