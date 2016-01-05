package org.almende.proheal;

import android.util.Log;

import java.util.ArrayList;

import nl.dobots.loopback.loopback.Beacon;

/**
 * Copyright (c) 2016 Dominik Egger <dominik@dobots.nl>. All rights reserved.
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
 * Created on 5-1-16
 *
 * @author Dominik Egger
 */
public class Inventory {

	private static final String TAG = Inventory.class.getCanonicalName();

	private static Inventory instance;

	private ArrayList<Beacon> _beaconList = new ArrayList<>();

	private Inventory() {

	}

	public static Inventory getInstance() {
		if (instance == null) {
			instance = new Inventory();
		}
		return instance;
	}

	public ArrayList<Beacon> getList() {
		return _beaconList;
	}

	public void updateBeaconSwitchState(Beacon beacon, boolean switchState) {
		for (Beacon oldBeacon : _beaconList) {
			if (oldBeacon.getAddress().equals(beacon.getAddress())) {
				Log.w(TAG, String.format("updated history: %s", beacon.getAddress()));
				oldBeacon.setSwitchState(switchState);
			}
		}
	}

	public Beacon findBeacon(Object id) {
		for (Beacon beacon : _beaconList) {
			if (beacon.getId().equals(id)) {
				return beacon;
			}
		}
		return null;
	}

	public void addBeacon(Beacon beacon) {
		_beaconList.add(beacon);
	}

}
