package com.example.urvocalcoach;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Class MenuActivity. The class for selecting the pitch or dynamics training option.
 */
public class MenuActivity extends Activity {
	
	/** The button_pitch. */
	private Button button_pitch;
	
	/** The button_dynamics. */
	private Button button_dynamics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		button_pitch = (Button) findViewById(R.id.button_pitch);
		button_pitch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(), MainActivity.class));
				finish();
			}
		});
		button_dynamics = (Button) findViewById(R.id.button_dynamics);
		button_dynamics.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(), DynamicsActivity.class));
				finish();
			}
		});
	}
	
}
