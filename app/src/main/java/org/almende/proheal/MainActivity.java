package org.almende.proheal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ListCallback;
import com.strongloop.android.loopback.callbacks.ObjectCallback;
import com.strongloop.android.loopback.callbacks.VoidCallback;

import org.almende.proheal.cfg.Config;

import java.util.List;

import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.extended.BleDeviceFilter;
import nl.dobots.bluenet.ble.extended.BleExt;
import nl.dobots.bluenet.ble.extended.structs.BleDevice;
import nl.dobots.bluenet.ble.extended.structs.BleDeviceList;
import nl.dobots.bluenet.service.BleScanService;
import nl.dobots.bluenet.service.callbacks.EventListener;
import nl.dobots.bluenet.service.callbacks.IntervalScanListener;
import nl.dobots.loopback.CrownstoneRestAPI;
import nl.dobots.loopback.loopback.Beacon;
import nl.dobots.loopback.loopback.BeaconRepository;
import nl.dobots.loopback.loopback.Location;
import nl.dobots.loopback.loopback.LocationRepository;
import nl.dobots.loopback.loopback.User;
import nl.dobots.loopback.loopback.UserRepository;

/**
 * This example activity shows the use of the bluenet library through the BleScanService. The
 * service is created on startup. The service takes care of initialization of the bluetooth
 * adapter, listens to state changes of the adapter, notifies listeners about these changes
 * and provides an interval scan. This means the service scans for some time, then pauses for
 * some time before starting another scan (this reduces battery consumption)
 *
 * The following steps are shown:
 *
 * 1. Start and connect to the BleScanService
 * 2. Set the scan interval and scan pause time
 * 3. Scan for devices and set a scan device filter
 * 4a. Register as a listener to get an update for every scanned device, or
 * 4b. Register as a listener to get an event at the start and end of each scan interval
 * 5. How to get the list of scanned devices, sorted by RSSI.
 *
 * For an example of how to read the current PWM state and how to power On, power Off, or toggle
 * the device switch, see ControlActivity.java
 * For an example of how to use the library directly, without using the service, see MainActivity.java
 *
 * Created on 1-10-15
 * @author Dominik Egger
 */
public class MainActivity extends Activity implements IntervalScanListener, EventListener, AdapterView.OnItemSelectedListener {

	private static final String TAG = MainActivity.class.getCanonicalName();

	private BleScanService _service;

	private TextView txtLocation;
	private Button _btnScan;
	private ListView _lvScanList;
	private TextView _txtClosest;
	private Spinner _spFilter;
	private Spinner _spRoom;
	private ImageView _lightBulb;
	private RelativeLayout _laySwitch;
	private ProgressDialog _progressDlg;
	private RelativeLayout _layDebug;
	private TextView _txtTitle;

	private boolean _bound = false;

	private BleDeviceList _bleDeviceList;
//	private String _address = "";

	private long _lastUpdate;

	private RestAdapter _restAdapter;

	private UserRepository _userRepository;

	private List<Beacon> _trackedBeacons;
	private BeaconRepository _beaconRepository;

	private Location _currentLocation = new Location();
	private LocationRepository _locationRepository;
	private List<Location> _locations;

	private BleExt _ble;

//	private boolean _lightOn;

	private Beacon _selectedBeacon;
	private Location _selectedLocation;

	private Watchdog _watchdog;
	private boolean _connectionError;
	private boolean _stopped;

	private BleDevice _lastClosestDevice;

	private Inventory _inventory;

	private boolean _switching = false;
	private User _currentUser;

	private Handler _uiHandler = new Handler();
	private Runnable _uiUpdate = new Runnable() {
		@Override
		public void run() {
			if (!_switching && !_connectionError && _selectedBeacon != null) {
				_beaconRepository.findById(_selectedBeacon.getId(), new ObjectCallback<Beacon>() {
					@Override
					public void onSuccess(Beacon object) {
						_selectedBeacon = object;
						updateLightBulb(_selectedBeacon.getSwitchState());
						_uiHandler.postDelayed(_uiUpdate, Config.GUI_UPDATE_INTERVAL);
					}

					@Override
					public void onError(Throwable t) {
						_uiHandler.postDelayed(_uiUpdate, Config.GUI_UPDATE_INTERVAL);
					}
				});
			} else {
				_uiHandler.postDelayed(_uiUpdate, Config.GUI_UPDATE_INTERVAL);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();

//		_restAdapter = new RestAdapter(getApplicationContext(), Config.REST_API_URL);
		_restAdapter = CrownstoneRestAPI.getRestAdapter(this, Config.REST_API_URL);

		_watchdog = new Watchdog(_restAdapter);

		_uiHandler.postDelayed(_uiUpdate, Config.GUI_UPDATE_INTERVAL);

		_inventory = Inventory.getInstance();

		BleDevice.setExpirationTime(2000);

//		_ble = new BleExt();
//		_ble.init(this, new IStatusCallback() {
//			@Override
//			public void onSuccess() {
//
//			}
//
//			@Override
//			public void onError(int error) {
//
//			}
//		});

		// create and bind to the BleScanService
		Intent intent = new Intent(this, BleScanService.class);
		bindService(intent, _connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		_userRepository = _restAdapter.createRepository(UserRepository.class);
		_beaconRepository = _restAdapter.createRepository(BeaconRepository.class);
		_locationRepository = _restAdapter.createRepository(LocationRepository.class);

		if (!_userRepository.isLoggedIn()) {
			startActivity(new Intent(this, LoginActivity.class));
			return;
		}

		_userRepository.findCurrentUser(new ObjectCallback<User>() {
			@Override
			public void onSuccess(User user) {
				_currentUser = user;
				final String username = (String) user.get("username");
				if (username != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							_txtTitle.setText(String.format("Hi %s", username));
						}
					});
				}
			}

			@Override
			public void onError(Throwable t) {

			}
		});


		refresh();

		onStartWatch();
	}

	@Override
	protected void onPause() {
		stopScan();
//		_watchdog.stop();

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		_watchdog.stop();
		unbindService(_connection);
		_bound = false;
		_uiHandler.removeCallbacksAndMessages(null);
	}

	// if the service was connected successfully, the service connection gives us access to the service
	private ServiceConnection _connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "connected to ble scan service ...");
			// get the service from the binder
			BleScanService.BleScanBinder binder = (BleScanService.BleScanBinder) service;
			_service = binder.getService();

			// register as event listener. Events, like bluetooth initialized, and bluetooth turned
			// off events will be triggered by the service, so we know if the user turned bluetooth
			// on or off
			_service.registerEventListener(MainActivity.this);

			// register as an interval scan listener. If you only need to know the list of scanned
			// devices at every end of an interval, then this is better. additionally it also informs
			// about the start of an interval.
			_service.registerIntervalScanListener(MainActivity.this);

			// set the scan interval (for how many ms should the service scan for devices)
			_service.setScanInterval(Config.LOW_SCAN_INTERVAL);
			// set the scan pause (how many ms should the service wait before starting the next scan)
			_service.setScanPause(Config.LOW_SCAN_PAUSE);

			_ble = _service.getBleExt();

			_bound = true;

			_watchdog.setService(_service);

			onStartWatch();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "disconnected from service");
			_bound = false;
		}
	};

	private void onStartWatch() {
		if (!_stopped) {
			_watchdog.start();
			startScan(BleDeviceFilter.crownstone);
		}
	}

	private void onStopWatch() {
		_watchdog.stop();
		stopScan();
	}

	private void onConnectionError() {

		if (!_connectionError) {
			_connectionError = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Connection Error")
					.setMessage("Please check your internet connection and refresh")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							_connectionError = false;
						}
					});
			// Create the AlertDialog object and return it
			builder.create().show();
			// stop watching, since we don't have connection anyway.
			_stopped = true;
			onStopWatch();
		}
	}

	private void refresh() {
		// clear stopped flag, and try connecting again
		_stopped = false;

		_beaconRepository.findAll(new ListCallback<Beacon>() {
			@Override
			public void onSuccess(List<Beacon> objects) {
				_trackedBeacons = objects;
				Log.i(TAG, "success: " + objects);
			}

			@Override
			public void onError(Throwable t) {
				Log.i(TAG, "error: ", t);
				onConnectionError();
			}
		});

		_locationRepository.findAll(new ListCallback<Location>() {
			@Override
			public void onSuccess(List<Location> objects) {
				_locations = objects;
				final String[] locationNames = new String[_locations.size() + 1];
				locationNames[0] = "None";
				for (int i = 0; i < _locations.size(); ++i) {
					locationNames[i + 1] = _locations.get(i).getDescription();
				}
				_spRoom.post(new Runnable() {
					@Override
					public void run() {
						_spRoom.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, locationNames));
						_spRoom.setOnItemSelectedListener(MainActivity.this);
					}
				});
			}

			@Override
			public void onError(Throwable t) {
				Log.i(TAG, "error: ", t);
				onConnectionError();
			}
		});
	}

	// is scanning returns true if the service is "running", not if it is currently in a
	// scan interval or a scan pause
	private boolean isScanning() {
		if (_bound) {
			return _service.isScanning();
		}
		return false;
	}

	private void initUI() {
		setContentView(R.layout.activity_main);

		_txtTitle = (TextView) findViewById(R.id.title);

		txtLocation = (TextView) findViewById(R.id.txtLocation);

		_spRoom = (Spinner) findViewById(R.id.spRoom);
		_spRoom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[] { "Please wait ..." }));

		_laySwitch = (RelativeLayout) findViewById(R.id.laySwitch);

		_lightBulb = (ImageView) findViewById(R.id.imgLightBulb);
		_lightBulb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				togglePWM();
			}
		});

		Button btnPowerOn = (Button) findViewById(R.id.btnPowerOn);
		btnPowerOn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				powerOn();
			}
		});

		Button btnPowerOff = (Button) findViewById(R.id.btnPowerOff);
		btnPowerOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				powerOff();
			}
		});

		_btnScan = (Button) findViewById(R.id.btnScan);
		_btnScan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// using the scan filter, we can tell the library to return only specific device
				// types. we are currently distinguish between Crownstones, DoBeacons, iBeacons,
				// and FridgeBeacons
				BleDeviceFilter selectedItem = (BleDeviceFilter) _spFilter.getSelectedItem();

				if (!isScanning()) {
					// start a scan with the given filter
					startScan(selectedItem);
				} else {
					stopScan();
				}
			}
		});

		// create a spinner element with the device filter options
		_spFilter = (Spinner) findViewById(R.id.spFilter);
		_spFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, BleDeviceFilter.values()));
		_spFilter.setSelection(1); // crownstones

		_layDebug = (RelativeLayout) findViewById(R.id.layDebug);
		if (Config.DEBUG) {
			_layDebug.setVisibility(View.VISIBLE);
		} else {
			_layDebug.setVisibility(View.GONE);
		}

		// create an empty list to assign to the list view. this will be updated whenever a
		// device is scanned
		_bleDeviceList = new BleDeviceList();
		DeviceListAdapter adapter = new DeviceListAdapter(this, _bleDeviceList);

		_lvScanList = (ListView) findViewById(R.id.lvScanList);
		_lvScanList.setAdapter(adapter);

		_txtClosest = (TextView) findViewById(R.id.txtClosest);
	}

	private void stopScan() {
		if (_bound) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_btnScan.setText(getString(R.string.main_scan));
				}
			});
			// stop scanning for devices
			_service.stopIntervalScan();
		}
	}

	private void startScan(BleDeviceFilter filter) {
		if (_bound) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_btnScan.setText(getString(R.string.main_stop_scan));
				}
			});
			// start scanning for devices, only return devices defined by the filter
			_service.startIntervalScan(filter);
		}
	}

	private void onBleEnabled() {
		_btnScan.setEnabled(true);
	}

	private void onBleDisabled() {
		_btnScan.setEnabled(false);
	}

	private void updateDeviceList() {
		if (_trackedBeacons == null) return;

		// update the device list. since we are not keeping up a list of devices ourselves, we
		// get the list of devices from the service

		BleDeviceList tmp = _ble.getDeviceMap().getRssiSortedList();

		// filter devices based on tracked beacons
		_bleDeviceList.clear();
		for (BleDevice device : tmp) {
			for (Beacon beacon : _trackedBeacons) {
				if (device.getAddress().equals(beacon.getAddress())) {
					_bleDeviceList.add(device);
					break;
				}
			}
		}

		if (_bleDeviceList.size() > 0) {
			final BleDevice closestDevice = _bleDeviceList.get(0);
			int averageRssi = closestDevice.getAverageRssi();

			if (averageRssi != 0 && averageRssi > Config.PRESENCE_THRESHOLD) {
				if (!closestDevice.equals(_lastClosestDevice)) {
					updateCurrentLocation(closestDevice);
					_lastClosestDevice = closestDevice;
				}
			} else {
				onLocationUnknown();
			}

			if (Config.DEBUG) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// the closest device is the first device in the list (because we asked for the
						// rssi sorted list)
						_txtClosest.setText(getString(R.string.main_closest_device, closestDevice.getName()));

						// update the list view
						DeviceListAdapter adapter = ((DeviceListAdapter) _lvScanList.getAdapter());
						adapter.updateList(_bleDeviceList);
						adapter.notifyDataSetChanged();
					}
				});
			}
		} else {
			onLocationUnknown();
		}
	}

	private void onLocationUnknown() {
		if (_currentLocation != null) {
			_lastClosestDevice = null;
			_currentLocation = null;

			_currentUser.setLocationId(null);
			_currentUser.save(new VoidCallback() {
				@Override
				public void onSuccess() {
					Log.i(TAG, "current location updated");
				}

				@Override
				public void onError(Throwable t) {
					Log.e(TAG, "failed to update current location!");
				}
			});

			txtLocation.post(new Runnable() {
				@Override
				public void run() {
					txtLocation.setText("Unknown");
				}
			});
		}
	}

	private void updateCurrentLocation(BleDevice device) {
		_beaconRepository.findLocation(device.getAddress(), new ObjectCallback<Location>() {
			@Override
			public void onSuccess(Location object) {
				_currentLocation = object;

				_currentUser.setLocationId(_currentLocation.getId());
				_currentUser.save(new VoidCallback() {
					@Override
					public void onSuccess() {
						Log.i(TAG, "current location updated");
					}

					@Override
					public void onError(Throwable t) {
						Log.e(TAG, "failed to update current location!");
					}
				});

				txtLocation.post(new Runnable() {
					@Override
					public void run() {
						if (_currentLocation != null) {
							txtLocation.setText(_currentLocation.getDescription());
						}
					}
				});
			}

			@Override
			public void onError(Throwable t) {
				onConnectionError();
			}
		});
	}

	@Override
	public void onScanStart() {
		// by registering to the service as an IntervalScanListener, the service informs us
		// whenever a new scan interval is started.

		// but we don't really care about that here
	}

	@Override
	public void onScanEnd() {
		// by registering to the service as an IntervalScanListener, the service informs us
		// whenever a scan interval ends.

		// at this point we can obtain the list of scanned devices from the library to update
		// the list view.
		// Note: this happens much less frequently than the onDeviceScanned event. if you need
		// instant updates for scanned devices, use the ScanDeviceListener instead.
		updateDeviceList();
	}

	@Override
	public void onEvent(Event event) {
		// by registering to the service as an EventListener, we will be informed whenever the
		// user turns bluetooth on or off, or even refuses to enable bluetooth
		switch (event) {
			case BLUETOOTH_INITIALIZED: {
				onBleEnabled();
				break;
			}
			case BLUETOOTH_TURNED_OFF: {
				onBleDisabled();
				break;
			}
			case BLUETOOTH_START_SCAN_ERROR: {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						_btnScan.setText(getString(R.string.main_scan));
					}
				});
			}
		}
	}

	private void updateSelectedLocation(Location location) {
		// find Beacon for this location
		hideSwitch();
		_locationRepository.findBeaconsForId(location.getId(), new ObjectCallback<Beacon>() {
			@Override
			public void onSuccess(Beacon object) {
				Log.i(TAG, "success: " + object.toString());
				showSwitch();
				_selectedBeacon = object;
				updateLightBulb(object.getSwitchState());
			}

			@Override
			public void onError(Throwable t) {
				Log.e(TAG, "error: ", t);
				onConnectionError();
			}
		});
	}

	private void updateSwitchState(final Beacon beacon, final boolean newSwitchState, final VoidCallback callback) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				beacon.setSwitchState(newSwitchState);
				beacon.save(new VoidCallback() {
					@Override
					public void onSuccess() {
						Log.i(TAG, "switch state updated successfully");
						callback.onSuccess();
					}

					@Override
					public void onError(Throwable t) {
						Log.i(TAG, "error: ", t);
						onConnectionError();
						callback.onError(t);
					}
				});
			}
		});
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (position != 0) {
			// room select
			// use position - 1 as index (position 0 is for None)
			_selectedLocation = _locations.get(position - 1);
			updateSelectedLocation(_selectedLocation);
		} else {
			_selectedLocation = null;
			_selectedBeacon = null;
			hideSwitch();
		}
	}

	private void showSwitch() {
		_laySwitch.setVisibility(View.VISIBLE);
	}

	private void hideSwitch() {
		_laySwitch.setVisibility(View.GONE);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	public boolean checkPermission() {
		return (_currentLocation != null && _currentLocation.getId() == _selectedLocation.getId()) || ownsLocation(_selectedLocation);
	}

	public boolean ownsLocation(Location location) {
		return location.getOwnerId() == _userRepository.getCurrentUserId();
	}

	private void onPermissionDenied() {

		Log.w(TAG, "permission denied");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Permission denied")
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void powerOff() {
		Log.i(TAG, "switching power off ...");
		if (checkPermission()) {

			pause();
			_progressDlg = ProgressDialog.show(MainActivity.this, "Switching power", "Please wait...");

			updateSwitchState(_selectedBeacon, false, new VoidCallback() {
				@Override
				public void onSuccess() {
					// switch the device off. this function will check first if the device is connected
					// (and connect if it is not), then it switches the device off, and disconnects again
					// afterwards (once the disconnect timeout expires)
					_ble.powerOff(_selectedBeacon.getAddress(), new IStatusCallback() {
						@Override
						public void onSuccess() {
							Log.i(TAG, "power off success");
							// power was switch off successfully, update the light bulb
							_progressDlg.dismiss();
							_inventory.updateBeaconSwitchState(_selectedBeacon, false);
							updateLightBulb(false);
							resume();
						}

						@Override
						public void onError(int error) {
							Log.i(TAG, "power off failed: " + error);
							_progressDlg.dismiss();
							resume();
						}
					});
				}

				@Override
				public void onError(Throwable t) {
					Log.i(TAG, "failed to update switch state");
					_progressDlg.dismiss();
					resume();
				}
			});
//			updateLightBulb(false);

		} else {
			onPermissionDenied();
		}
	}

	private void powerOn() {
		Log.i(TAG, "switching power on ...");
		if (checkPermission()) {

			pause();
			_progressDlg = ProgressDialog.show(MainActivity.this, "Switching power", "Please wait...");

			updateSwitchState(_selectedBeacon, true, new VoidCallback() {
				@Override
				public void onSuccess() {
					// switch the device on. this function will check first if the device is connected
					// (and connect if it is not), then it switches the device on, and disconnects again
					// afterwards (once the disconnect timeout expires)
					_ble.powerOn(_selectedBeacon.getAddress(), new IStatusCallback() {
						@Override
						public void onSuccess() {
							Log.i(TAG, "power on success");
							// power was switch on successfully, update the light bulb
							_progressDlg.dismiss();
							_inventory.updateBeaconSwitchState(_selectedBeacon, true);
							updateLightBulb(true);
							resume();
						}

						@Override
						public void onError(int error) {
							Log.i(TAG, "power on failed: " + error);
							_progressDlg.dismiss();
							resume();
						}
					});
				}

				@Override
				public void onError(Throwable t) {
					Log.i(TAG, "failed to update switch state");
					_progressDlg.dismiss();
					resume();
				}
			});
//			updateLightBulb(true);

		} else {
			onPermissionDenied();
		}
	}

	private void pause() {
		_switching = true;
		Log.i(TAG, "main: pausing to update ...");
		_watchdog.stop();
		_service.stopIntervalScan();
		SystemClock.sleep(500);
		Log.i(TAG, "main: ... ok");
	}

	private void resume() {
		_ble.disconnectAndClose(false, new IStatusCallback() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "main: resume with scan");
				_switching = false;
				_watchdog.start();
				_service.startIntervalScan(BleDeviceFilter.crownstone);
			}

			@Override
			public void onError(int error) {
				Log.i(TAG, "main error: resume with scan");
				_switching = false;
				_watchdog.start();
				_service.startIntervalScan(BleDeviceFilter.crownstone);
			}
		});
	}

	private void togglePWM() {
		if (_selectedBeacon.getSwitchState()) {
			powerOff();
		} else {
			powerOn();
		}

//		if (checkPermission()) {
//			boolean on = !_selectedBeacon.getSwitchState();
//			updateSwitchState(_selectedBeacon, on);
//			updateLightBulb(on);
//
//			pause();
//			_progressDlg = ProgressDialog.show(this, "Switching power", "Please wait...");
//			// toggle the device switch, without needing to know the current state. this function will
//			// check first if the device is connected (and connect if it is not), then it reads the
//			// current PWM state, and depending on the state, decides if it needs to switch it on or
//			// off. in the end it disconnects again (once the disconnect timeout expires)
//			_ble.togglePower(_selectedBeacon.getAddress(), new IStatusCallback() {
//				@Override
//				public void onSuccess() {
//					Log.i(TAG, "toggle success");
//					// power was toggled successfully, update the light bulb
//					_progressDlg.dismiss();
//					resume();
//				}
//
//				@Override
//				public void onError(int error) {
//					Log.e(TAG, "toggle failed: " + error);
//					_progressDlg.dismiss();
//					resume();
//				}
//			});
//		} else {
//			onPermissionDenied();
//		}
	}

	private void updateLightBulb(final boolean on) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (on) {
					_lightBulb.setImageResource(getResources().getIdentifier("light_bulb_on", "drawable", getPackageName()));
				} else {
					_lightBulb.setImageResource(getResources().getIdentifier("light_bulb_off", "drawable", getPackageName()));
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch(id) {
			case R.id.action_settings: {
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			}
			case R.id.action_refresh: {
				refresh();
				onStartWatch();
				break;
			}
		}

		return super.onOptionsItemSelected(item);
	}

}
