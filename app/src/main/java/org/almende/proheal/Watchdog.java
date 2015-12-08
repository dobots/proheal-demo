package org.almende.proheal;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ListCallback;

import org.almende.proheal.cfg.Config;
import org.almende.proheal.loopback.Beacon;
import org.almende.proheal.loopback.BeaconRepository;

import java.util.ArrayList;
import java.util.List;

import nl.dobots.bluenet.ble.base.callbacks.IIntegerCallback;
import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.extended.BleExt;
import nl.dobots.bluenet.service.BleScanService;

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

	private BleExt _ble;

	private Handler _watchdogHandler;

	private List<Beacon> _history = new ArrayList<>();

	private boolean _stopped = true;

	private Runnable updateSwitchState = new Runnable() {
		@Override
		public void run() {

			_beaconRepository.findAll(new ListCallback<Beacon>() {
				@Override
				public void onSuccess(List<Beacon> objects) {

//					while (_ble.isScanning()) {
//
//					}

					for (Beacon lastBeacon : _history) {
						for (final Beacon thisBeacon : objects) {
							if (lastBeacon.getId() == thisBeacon.getId()) {
								if (lastBeacon.getSwitchState() != thisBeacon.getSwitchState()) {
									_ble.readPwm(thisBeacon.getAddress(), new IIntegerCallback() {
										@Override
										public void onSuccess(int result) {
											if (thisBeacon.getSwitchState() && result == 0) {

												_ble.powerOn(thisBeacon.getAddress(), new IStatusCallback() {
													@Override
													public void onSuccess() {

													}

													@Override
													public void onError(int error) {
														thisBeacon.setSwitchState(false);
													}
												});
											} else if (!thisBeacon.getSwitchState() && result != 0) {
												_ble.powerOff(thisBeacon.getAddress(), new IStatusCallback() {
													@Override
													public void onSuccess() {

													}

													@Override
													public void onError(int error) {
														thisBeacon.setSwitchState(true);
													}
												});
											}
										}

										@Override
										public void onError(int error) {

										}
									});

								}
							}
						}
					}

					_history = objects;
					if (!_stopped) {
						_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
					}
				}

				@Override
				public void onError(Throwable t) {
					Log.i(TAG, "error: ", t);
					if (!_stopped) {
						_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
					}
				}
			});
		}
	};

	public Watchdog(Context context, RestAdapter adapter) {
		_ble = new BleExt();
		_ble.init(context, new IStatusCallback() {
			@Override
			public void onSuccess() {

			}

			@Override
			public void onError(int error) {

			}
		});

		_beaconRepository = adapter.createRepository(BeaconRepository.class);

		HandlerThread ht = new HandlerThread("watchdog");
		ht.start();
		_watchdogHandler = new Handler(ht.getLooper());
	}

	public void start() {
		if (_stopped) {
			Log.d(TAG, "watchdog started");
			_stopped = false;
			_watchdogHandler.removeCallbacksAndMessages(null);
			_watchdogHandler.post(updateSwitchState);
		}
	}

	public void stop() {
		if (!_stopped) {
			Log.d(TAG, "watchdog paused");
			_stopped = true;
			_watchdogHandler.removeCallbacksAndMessages(null);
		}
	}

}
