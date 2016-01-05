package org.almende.proheal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.strongloop.android.loopback.RestAdapter;

import org.almende.proheal.cfg.Config;
import org.almende.proheal.cfg.Settings;

import nl.dobots.loopback.loopback.UserRepository;

public class SettingsActivity extends Activity {

	private Button _btnLogIn;
	private TextView _txtLogInStatus;

	private Settings _settings;

	private RestAdapter _restAdapter;
	private UserRepository _userRepositiory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		_settings = Settings.getInstance(this);

		_restAdapter = new RestAdapter(getApplicationContext(), Config.REST_API_URL);
		_userRepositiory = _restAdapter.createRepository(UserRepository.class);

		initUI();
	}

	private void initUI() {

		_btnLogIn = (Button) findViewById(R.id.btnLogin);
		_btnLogIn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
			}
		});

		_txtLogInStatus = (TextView) findViewById(R.id.txtLogInStatus);

		final TextView presenceThresholdText = (TextView) findViewById(R.id.txtPresenceThreshold);
		presenceThresholdText.setText(String.valueOf(_settings.getPresenceThreshold()));

		SeekBar distanceBar= (SeekBar) findViewById(R.id.sbPresenceThreshold);
		distanceBar.setProgress(_settings.getPresenceThreshold() * -1);
		distanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			int progress = 0;

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				_settings.setPresenceThreshold(progress * -1);
				presenceThresholdText.setText(String.valueOf(_settings.getPresenceThreshold()));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		Button saveSettings = (Button) findViewById(R.id.btnSaveSettings);
		saveSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				_settings.writePeristentSettings();
				finish();
			}
		});

		Button clearSettings = (Button) findViewById(R.id.btnClearSettings);
		clearSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setTitle("Remove Settings");
				builder.setMessage("Your stored settings and locations will be removed! This cannot be undone!");
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						_settings.clearSettings();
						initUI();
					}
				});
				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						/* nothing to do */
					}
				});
				builder.show();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (_userRepositiory.isLoggedIn()) {
			_txtLogInStatus.setText("Logged In");
			_btnLogIn.setText("Change User");
		} else {
			_txtLogInStatus.setText("Not Logged In");
			_btnLogIn.setText("Log In");
		}

	}
}
