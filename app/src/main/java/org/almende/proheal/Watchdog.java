package org.almende.proheal;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ListCallback;

import org.almende.proheal.cfg.Config;

import java.util.Iterator;
import java.util.List;

import nl.dobots.bluenet.ble.base.callbacks.IIntegerCallback;
import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.extended.BleDeviceFilter;
import nl.dobots.bluenet.ble.extended.BleExt;
import nl.dobots.bluenet.service.BleScanService;
import nl.dobots.loopback.loopback.Beacon;
import nl.dobots.loopback.loopback.BeaconRepository;

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
 * Created on 8-12-15
 *
 * @author Dominik Egger
 */
public class Watchdog {

	private static final String TAG = Watchdog.class.getCanonicalName();

	private final BeaconRepository _beaconRepository;

	private final Inventory _inventory;

	private BleScanService _service;

	private BleExt _ble;

	private Handler _watchdogHandler;

//	private boolean _stopped = true;
	private boolean _paused = false;

	private boolean _scanPaused = false;

	private boolean _executing = false;
//	private boolean hasChange = false;

	public Watchdog(RestAdapter adapter) {
		_beaconRepository = adapter.createRepository(BeaconRepository.class);

		HandlerThread ht = new HandlerThread("watchdog");
		ht.start();
		_watchdogHandler = new Handler(ht.getLooper());

		_inventory = Inventory.getInstance();
	}

	private Runnable updateSwitchState = new Runnable() {
		@Override
		public void run() {

			if (_paused) {
				return;
			}

			if (_ble == null) {
				_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
				return;
			}

			_executing = true;

			Log.i(TAG, "watchdog: checking beacons ...");
			_beaconRepository.findAll(new ListCallback<Beacon>() {
				@Override
				public void onSuccess(List<Beacon> objects) {

					Iterator<Beacon> it = objects.iterator();
					if (it.hasNext()) {
						updateNextBeacon(it);
					} else {
						Log.i(TAG, "nothing to do!");
						_executing = false;
						_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
					}

				}

				@Override
				public void onError(Throwable t) {
					Log.i(TAG, "error: ", t);
//					if (!_stopped) {
						_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
//					}

					Log.i(TAG, "watchdog: ... done");

					_executing = false;
				}
			});
		}
	};

	private void pauseScan() {
		if (_ble != null) {
			Log.i(TAG, "watchdog: pause scan to update switch state");
			_service.stopIntervalScan();
			_scanPaused = true;
		}
	}

	private void resumeScan() {
		if (_ble != null) {
			Log.i(TAG, "watchdog: resume scan");
			_service.startIntervalScan(BleDeviceFilter.crownstone);
			_scanPaused = false;

		}
	}

	private void disconnectAndUpdateNextBeacon(final Iterator<Beacon> iterator) {
		_ble.disconnectAndClose(false, new IStatusCallback() {
			@Override
			public void onSuccess() {
				updateNextBeacon(iterator);
			}

			@Override
			public void onError(int error) {
				updateNextBeacon(iterator);
			}
		});
	}

	private void updateNextBeacon(final Iterator<Beacon> iterator) {
		if (iterator.hasNext()) {
			final Beacon thisBeacon = iterator.next();
			Beacon lastBeacon = _inventory.findBeacon(thisBeacon.getId());

			if (lastBeacon == null) {

				_inventory.addBeacon(thisBeacon);
				updateNextBeacon(iterator);

			} else if (lastBeacon.getSwitchState() != thisBeacon.getSwitchState()) {

				if (_paused) {
					finishUpdate(false);
					return;
				}

				Log.i(TAG, "watchdog: update");

				pauseScan();

				Log.i(TAG, "watchdog: readPwm");
				_ble.readPwm(thisBeacon.getAddress(), new IIntegerCallback() {
					@Override
					public void onSuccess(int result) {
						if (thisBeacon.getSwitchState() && result == 0) {

							if (_paused) {
								finishUpdate(false);
								return;
							}

							Log.i(TAG, "watchdog: pwmOn");
							_ble.powerOn(thisBeacon.getAddress(), new IStatusCallback() {
								@Override
								public void onSuccess() {
									_inventory.updateBeaconSwitchState(thisBeacon, true);
									disconnectAndUpdateNextBeacon(iterator);
								}

								@Override
								public void onError(int error) {
//									thisBeacon.setSwitchState(false);
									disconnectAndUpdateNextBeacon(iterator);
								}
							});
						} else if (!thisBeacon.getSwitchState() && result != 0) {

							if (_paused) {
								finishUpdate(false);
								return;
							}

							Log.i(TAG, "watchdog: pwmOff");
							_ble.powerOff(thisBeacon.getAddress(), new IStatusCallback() {
								@Override
								public void onSuccess() {
									_inventory.updateBeaconSwitchState(thisBeacon, false);
									disconnectAndUpdateNextBeacon(iterator);
								}

								@Override
								public void onError(int error) {
//									thisBeacon.setSwitchState(true);
									disconnectAndUpdateNextBeacon(iterator);
								}
							});
						} else {
							updateNextBeacon(iterator);
						}
					}

					@Override
					public void onError(int error) {
//						thisBeacon.setSwitchState(!thisBeacon.getSwitchState());
						disconnectAndUpdateNextBeacon(iterator);
					}
				});
			} else {
				updateNextBeacon(iterator);
			}
		} else {
			finishUpdate(true);
		}
	}

	private void finishUpdate(boolean done) {
		_executing = false;
		if (done && _scanPaused) {
			resumeScan();
		}
		Log.i(TAG, "... done");
		_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
	}

	public void setService(BleScanService service) {
		_service = service;
		_ble = service.getBleExt();
	}

	public void stop() {
		Log.d(TAG, "pausing watchdog ...");
		_paused = true;

		if (_executing) {
			Log.d(TAG, "waiting ...");
			while(_executing) {};
		}

		Log.d(TAG, "watchdog paused");
		_watchdogHandler.removeCallbacksAndMessages(null);
	}

	public void start() {
		Log.d(TAG, "watchdog started");
		_paused = false;
		_watchdogHandler.removeCallbacksAndMessages(null);
		_watchdogHandler.post(updateSwitchState);
	}

}
