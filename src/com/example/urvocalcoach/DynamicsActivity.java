package com.example.urvocalcoach;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.urvocalcoach.Tuning.MusicNote;

/**
 * The Class DynamicsActivity. Activity for dynamics training.
 */
public class DynamicsActivity extends Activity {

		/** The ui controller. */
		private UiControllerDynamics uiController;
		
		/** The analyzer. */
		private AudioAnalyzer analyzer;
		
		/** The user volume img. */
		private ImageView userVolumeImg;
		
		/** The volume text. */
		private TextView volumeText;
		
		/** The target freq. */
		private TextView targetFreq;
		// private TextView userNote;
		/** The vibrator. */
		private Vibrator vibrator;
		
		/** The note selector. */
		private Spinner noteSelector;
		// private SeekBar volumeBar;
		/** The time. */
		private TextView time;

    	@Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_dynamics);
	        try {
	        	analyzer = new  AudioAnalyzer();
	        	uiController = new UiControllerDynamics(this);
	        	userVolumeImg = (ImageView)findViewById(R.id.user_volume);
	        	volumeText = (TextView)findViewById(R.id.volume_text);
	        	time = (TextView)findViewById(R.id.dynamics_time);
				noteSelector = (Spinner)findViewById(R.id.spinner_targetNote);
				analyzer.addObserver(uiController);
				String defaultNote = this.getResources().getString(R.string.default_note);
				Button button = (Button)findViewById(R.id.target_note_sing);
				button.setOnClickListener(uiController);
				button.setOnTouchListener(uiController);
				this.displayFeedBack(false);
				vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
				ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.spinner_targetNotes, 
						R.layout.spinner_layout);
				noteSelector.setAdapter(arrayAdapter);
				noteSelector.setOnItemSelectedListener(uiController);
				noteSelector.setSelection(Tuning.getNoteByName(defaultNote).getIndex());
				targetFreq = (TextView)findViewById(R.id.target_note_freq);
			} catch (Exception e) {
				Toast.makeText(this, "The are problems with your microphone :(" + e, Toast.LENGTH_LONG ).show();
			}
	    }

    	@Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        // Inflate the menu; this adds items to the action bar if it is present.
	        getMenuInflater().inflate(R.menu.main, menu);
	        return true;
	    }

    	@Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        // Handle action bar item clicks here. The action bar will
	        // automatically handle clicks on the Home/Up button, so long
	        // as you specify a parent activity in AndroidManifest.xml.
	        int id = item.getItemId();
	        if (id == R.id.action_settings) {
	            return true;
	        }
	        return super.onOptionsItemSelected(item);
	    }   
	    
	    /**
    	 * Gets the time.
    	 *
    	 * @param t the t
    	 * @return the time
    	 */
    	public void getTime(double t) {
	    	int t1 = (int)(t/100);
	    	time.setText(String.valueOf(t1));
	    }
	    
	    /**
    	 * Update user note.
    	 *
    	 * @param note the note
    	 * @param tactileFeed the tactile feed
    	 * @param position the position
    	 * @param volume the volume
    	 * @param t the t
    	 */
    	public void updateUserNote(MusicNote note, boolean tactileFeed, int position, int volume, double t) {
	    	int t1 = (int)(t/100);
	    	int targetVolume = -Math.abs((int)(2.3*t1/100)-9)+7;
	    	double dVolume = (double)volume;
	    	float fVolume = (float)(dVolume/1000.0);
	    	volume = (int)(volume/100);
	    	userVolumeImg.setTranslationX(t1);
	    	userVolumeImg.setTranslationY(position*25);
	    	userVolumeImg.setScaleY(fVolume);
	    	volumeText.setText(String.valueOf(volume));
	    	
	    	// Create the current volume image
	    	ImageView currentVolume = new ImageView(this);
	    	if (t1 < 100)
	    		currentVolume.setImageResource(R.drawable.dynamics_start);
	    	else if ((position < 4 && position > -4) && volume==targetVolume)
	    		currentVolume.setImageResource(R.drawable.dynamics_success);
	    	else
	    		currentVolume.setImageResource(R.drawable.dynamics_wrong);
	    	RelativeLayout rl = (RelativeLayout) findViewById(R.id.target_scale);
	    	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
	    	    RelativeLayout.LayoutParams.WRAP_CONTENT,
	    	    RelativeLayout.LayoutParams.WRAP_CONTENT);
	    	lp.addRule(RelativeLayout.BELOW, R.id.scale_marker_5);
	    	lp.topMargin = -16;
	    	lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    	currentVolume.setTranslationX(t1);
	    	currentVolume.setTranslationY(position*25);
	    	currentVolume.setScaleY(fVolume);
	    	rl.removeView(currentVolume);
	    	rl.addView(currentVolume, lp);
	    	if(tactileFeed) {
	    		vibrator.vibrate(200);
	    	}
	    }
	    
	    /**
    	 * Update target note.
    	 *
    	 * @param note the note
    	 */
    	public void updateTargetNote(MusicNote note) {
	    	targetFreq.setText(Double.toString(note.getFrequency()));
	    	noteSelector.setSelection(note.getIndex());
//	    	targetNote.setText(note.getNote());
	    }
	    
	    /**
    	 * Display feed back.
    	 *
    	 * @param show the show
    	 */
    	public void displayFeedBack(boolean show) {
	    	if(show) {
	    		userVolumeImg.setVisibility(View.VISIBLE);
	    	} else {
	    		userVolumeImg.setVisibility(View.INVISIBLE);
	    	}
	    }

    	@Override
		protected void onDestroy() {
			super.onDestroy();

		}

		/* (non-Javadoc)
		 * @see android.app.Activity#onPause()
		 */
		@Override
		protected void onPause() {
			super.onPause();
		}

		@Override
		protected void onRestart() {
			super.onRestart();
		}

		@Override
		protected void onResume() {
			super.onResume();
	        if(analyzer!=null) {
	        	analyzer.ensureStarted();
	        }
		}

		@Override
		protected void onStart() {
			super.onStart();
	        if(analyzer!=null) {
	        	analyzer.start();
	        }
	        if(uiController != null) {
	        	uiController.startTactileFeedBack();
	        }
		}

		@Override
		protected void onStop() {
			super.onStop();
	        if(analyzer!=null) {
	        	analyzer.stop();        	
	        }
	        if(uiController != null) {
	        	uiController.stopTactileFeedBack();
	        }
		}

		@Override
		public void onBackPressed() {
			super.onBackPressed();
			startActivity(new Intent(getApplicationContext(), MenuActivity.class));
			finish();
		}
	
}
