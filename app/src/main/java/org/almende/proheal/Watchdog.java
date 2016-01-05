package org.almende.proheal;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ListCallback;

import org.almende.proheal.cfg.Config;

import java.util.ArrayList;
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

	private BleScanService _service;

	private BleExt _ble;

	private Handler _watchdogHandler;

	private List<Beacon> _history = new ArrayList<>();

//	private boolean _stopped = true;
	private boolean _paused = false;

	private void pauseService() {
		if (_ble != null) {
			Log.i(TAG, "watchdog: update switch state");
			_service.stopIntervalScan();
		}
	}

	private void resumeService() {
		if (_ble != null) {
			_ble.disconnectAndClose(false, new IStatusCallback() {
				@Override
				public void onSuccess() {
					Log.i(TAG, "watchdog: resume");
					_service.startIntervalScan(BleDeviceFilter.crownstone);
					_executing = false;
				}

				@Override
				public void onError(int error) {
					Log.i(TAG, "watchdog: resume");
					_service.startIntervalScan(BleDeviceFilter.crownstone);
					_executing = false;
				}
			});
		}
	}

	private boolean _executing = false;

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

//					while (_ble.isScanning()) {
//
//					}

					boolean hasChange = false;

					for (Beacon lastBeacon : _history) {
						for (final Beacon thisBeacon : objects) {
							if (lastBeacon.getId() == thisBeacon.getId()) {
								if (lastBeacon.getSwitchState() != thisBeacon.getSwitchState()) {

									if (_paused) {
										_executing = false;
										return;
									}

									hasChange = true;

									Log.i(TAG, "watchdog: update");

									pauseService();
									Log.i(TAG, "watchdog: readPwm");
									_ble.readPwm(thisBeacon.getAddress(), new IIntegerCallback() {
										@Override
										public void onSuccess(int result) {
											if (thisBeacon.getSwitchState() && result == 0) {

												if (_paused) {
													_executing = false;
													return;
												}

												Log.i(TAG, "watchdog: pwmOn");
												_ble.powerOn(thisBeacon.getAddress(), new IStatusCallback() {
													@Override
													public void onSuccess() {
														resumeService();
													}

													@Override
													public void onError(int error) {
														thisBeacon.setSwitchState(false);
														resumeService();
													}
												});
											} else if (!thisBeacon.getSwitchState() && result != 0) {

												if (_paused) {
													_executing = false;
													return;
												}

												Log.i(TAG, "watchdog: pwmOff");
												_ble.powerOff(thisBeacon.getAddress(), new IStatusCallback() {
													@Override
													public void onSuccess() {
														resumeService();
													}

													@Override
													public void onError(int error) {
														thisBeacon.setSwitchState(true);
														resumeService();
													}
												});
											} else {
												resumeService();
											}
										}

										@Override
										public void onError(int error) {
											thisBeacon.setSwitchState(!thisBeacon.getSwitchState());
											resumeService();
										}
									});

								}
							}
						}
					}

					_history = objects;
//					if (!_stopped) {
						_watchdogHandler.postDelayed(updateSwitchState, Config.WATCHDOG_INTERVAL);
//					}

					if (!hasChange) {
						_executing = false;
					}

					Log.i(TAG, "watchdog: ... done");
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

	public Watchdog(RestAdapter adapter) {
		_beaconRepository = adapter.createRepository(BeaconRepository.class);

		HandlerThread ht = new HandlerThread("watchdog");
		ht.start();
		_watchdogHandler = new Handler(ht.getLooper());
	}

//	public void start() {
//		if (_stopped) {
//			Log.d(TAG, "watchdog started");
//			_stopped = false;
//			_watchdogHandler.removeCallbacksAndMessages(null);
//			_watchdogHandler.post(updateSwitchState);
//		}
//	}

//	public void stop() {
//		if (!_stopped) {
//			Log.d(TAG, "watchdog paused");
//			_stopped = true;
//			_watchdogHandler.removeCallbacksAndMessages(null);
//		}
//	}

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

	public List<Beacon> getHistory() {
		return _history;
	}

	public void updateBeaconSwitchState(Beacon beacon, boolean switchState) {
		for (Beacon oldBeacon : _history) {
			if (oldBeacon.getAddress().equals(beacon.getAddress())) {
				Log.w(TAG, String.format("updated history: %s", beacon.getAddress()));
				oldBeacon.setSwitchState(switchState);
			}
		}
	}

}
