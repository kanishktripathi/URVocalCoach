package com.example.urvocalcoach;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.example.urvocalcoach.AudioAnalyzer.AnalyzedSound;
import com.example.urvocalcoach.Tuning.MusicNote;


/**
 * The Class UiControllerMain.
 */
public class UiControllerMain implements Observer, OnItemSelectedListener, OnTouchListener, OnClickListener {

	/** The ui. */
	private MainActivity ui;
	
	/** The executor. */
	private ExecutorService executor;
	
	/** The tactile feed. */
	private boolean tactileFeed;
	
	/** The tone match. */
	private boolean toneMatch;
	
	/** The show. */
	private boolean show;
	
	/** The target note. */
	private MusicNote targetNote;
	
	/** The current note. */
	private MusicNote currentNote;
	
	/** The is target note. */
	private boolean isTargetNote;
		
	/**
	 * Instantiates a new ui controller main.
	 *
	 * @param u the u
	 */
	public UiControllerMain(MainActivity u) {
		ui = u;
		executor = Executors.newFixedThreadPool(4);
		targetNote = Tuning.getNote(262.5);
		currentNote = Tuning.getNote(0);
	}

	@Override
	public void update(Observable who, Object obj) {
		if(who instanceof AudioAnalyzer) {
			if(obj instanceof AnalyzedSound) {
				AnalyzedSound result = (AnalyzedSound)obj;
				double frequency = FrequencySmoothener.getSmoothFrequency(result);
				if(frequency > 0.0) {
					if(isTargetNote) {
						targetNote = Tuning.getNote(frequency);
						ui.updateTargetNote(targetNote);
					} else {
						currentNote = Tuning.getNote(frequency);
						ui.updateUserNote(currentNote, toneMatch, targetNote.getIndex() - currentNote.getIndex(), 
								result.getLoudness());
						toneMatch = false;
						if(!show) {
							show = true;
							ui.displayFeedBack(show);
						}						
					}
				} else {
					if(show) {
						show = false;
						ui.displayFeedBack(show);
					}
				}
			}
		}
	}
	
	/**
	 * Start tactile feed back. Constantly checks whether the user is in the perfect pith or not.
	 * If user stays in pitch for 1.5 seconds, it sends a vibrating signal.
	 */
	public void startTactileFeedBack() {
		tactileFeed = true;
		Thread tactileFeedBack = new Thread(new Runnable() {
			@Override
			public void run() {
				long start = System.currentTimeMillis(), end ;
				while(tactileFeed) {
					if(currentNote.getFrequency() == targetNote.getFrequency()) {
						end = System.currentTimeMillis();
						if(end - start >= 1500) {
							toneMatch = true;
							//tactileFeed = false;
						}
					} else {
						start = System.currentTimeMillis();
					}
				}
			}
		});
		executor.execute(tactileFeedBack);
	}
	
	/**
	 * Stop tactile feed back.
	 */
	public void stopTactileFeedBack() {
		tactileFeed = false;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		targetNote = Tuning.getNote(position);	
		ui.updateTargetNote(targetNote);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}

	@Override
	public void onClick(View v) {
		isTargetNote = false;
		startTactileFeedBack();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		stopTactileFeedBack();
		isTargetNote = true;		
		return false;
	}
}
