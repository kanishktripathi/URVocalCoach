package com.example.urvocalcoach;

import java.util.Observable;

import org.jtransforms.fft.DoubleFFT_1D;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.example.urvocalcoach.AudioAnalyzer.AnalyzedSound.ReadingType;

/**
 * The Class AudioAnalyzer. This class uses observer
 */
public class AudioAnalyzer extends Observable implements AudioRecord.OnRecordPositionUpdateListener {
	
	/** The Constant TAG. */
	public static final String TAG = "RealGuitarTuner";
	
	/** The Constant AUDIO_SAMPLING_RATE. */
	private static final int AUDIO_SAMPLING_RATE = 44100;
	
	/** The audio data size. */
	private static int audioDataSize = 7200; // Length of sample to analyze.
	
	// Enough to pick up most of the frequencies.
	/** The Constant MPM. */
	private static final double MPM = 0.7;
	
	/** The Constant maxStDevOfMeanFrequency. */
	private static final double maxStDevOfMeanFrequency = 2.0; // if stdev bigger than that
	
	/** The Constant MaxPossibleFrequency. */
	private static final double MaxPossibleFrequency = 2700.0;     // result considered rubbish
	
	/** The Constant loudnessThreshold. */
	private static final double loudnessThreshold = 30.0; // below is too quiet
	
	/** The Constant PercentOfWavelenghSamplesToBeIgnored. */
	private static final double PercentOfWavelenghSamplesToBeIgnored = 0.2;


	/** The audio record. */
	private AudioRecord audioRecord;
	
	/** The buffer size. */
	private int bufferSize;
	
	/** The audio data. */
	private final CircularBuffer audioData;
	
	/** The audio data temp. */
	private short [] audioDataTemp;
	
	/** The audio data analyzis. */
	private double [] audioDataAnalyzis;
	
	/** The fft_method. */
	DoubleFFT_1D fft_method;
	
	/** The wavelengths. */
	private int wavelengths;
	
	/** The wavelength. */
	private double [] wavelength;
	
	/** The elements read. */
	private int elementsRead = 0;
	
	
	/** The should audio reader thread die. */
	private boolean shouldAudioReaderThreadDie;
	
	/** The audio reader thread. */
	private Thread audioReaderThread;
	
	/** The analyzis result. */
	private AnalyzedSound analyzisResult;
	
	/**
	 * The Class AnalyzedSound.
	 */
	public static class AnalyzedSound {
		
		/**
		 * The Enum ReadingType.
		 */
		public static enum ReadingType  {
			
			/** The no problems. */
			NO_PROBLEMS,
			
			/** The too quiet. */
			TOO_QUIET,
			
			/** The zero samples. */
			ZERO_SAMPLES,
			
			/** The big variance. */
			BIG_VARIANCE,
			
			/** The big frequency. */
			BIG_FREQUENCY
		};
		
		/** The loudness. */
		private int loudness;
		
		/** The frequency available. */
		private boolean frequencyAvailable;
		
		/** The frequency. */
		private double frequency;
		
		/** The error. */
		private ReadingType error;
		
		/**
		 * Instantiates a new analyzed sound.
		 *
		 * @param l the l
		 * @param e the e
		 */
		public AnalyzedSound(int l,ReadingType e) {
			loudness = l;
			frequencyAvailable = false;
			error = e;
		}
		
		/**
		 * Instantiates a new analyzed sound.
		 *
		 * @param l the l
		 * @param f the f
		 */
		public AnalyzedSound(int l, double f) {
			loudness = l;
			frequencyAvailable = true;
			frequency = f;
			error = ReadingType.NO_PROBLEMS;
		}
		
		/**
		 * Gets the loudness.
		 *
		 * @return the loudness
		 */
		public int getLoudness() {
			return loudness;
		}

		/**
		 * Checks if is frequency available.
		 *
		 * @return true, if is frequency available
		 */
		public boolean isFrequencyAvailable() {
			return frequencyAvailable;
		}

		/**
		 * Gets the frequency.
		 *
		 * @return the frequency
		 */
		public double getFrequency() {
			return frequency;
		}

		/**
		 * Gets the error.
		 *
		 * @return the error
		 */
		public ReadingType getError() {
			return error;
		}
	}
	
	/**
	 * Instantiates a new audio analyzer.
	 *
	 * @throws Exception the exception
	 */
	public AudioAnalyzer() throws Exception {
		// Setting up AudioRecord class.
		bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, 
				AudioFormat.ENCODING_PCM_16BIT) * 2;
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLING_RATE, 
									  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		
		audioRecord.setRecordPositionUpdateListener(this);
		
		if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
			throw new Exception("Could not initialize microphone.");
		}
		audioDataTemp = new short[audioDataSize];
		audioDataAnalyzis = new double[4 * audioDataSize + 100];
		wavelength = new double[audioDataSize];
		audioData = new CircularBuffer(audioDataSize);
		fft_method = new DoubleFFT_1D(audioDataSize);
	}
	
	/**
	 * Start.
	 */
	public void start() { // onStart
		audioRecord.startRecording();
		startAudioReaderThread();
	}
	

	/**
	 * Start audio reader thread.
	 */
	private void startAudioReaderThread() {
		shouldAudioReaderThreadDie = false;
		audioReaderThread = new Thread(new Runnable() {
			@Override

			public void run() {
				while(!shouldAudioReaderThreadDie) {
					int shortsRead = audioRecord.read(audioDataTemp,0,audioDataSize);
					if(shortsRead < 0) {
						Log.e(TAG, "Could not read audio data.");
					} else {
						for(int i=0; i<shortsRead; ++i) {
							audioData.push(audioDataTemp[i]);
						}
					}
					analyzisResult = getFrequency();
					setChanged();
				}
			}
		});
		audioReaderThread.setDaemon(false);
		audioReaderThread.start();
	}
	
	/**
	 * Stop audio reader thread.
	 */
	private void stopAudioReaderThread() {
		shouldAudioReaderThreadDie = true;
		try {
			audioReaderThread.join();
		} catch(Exception e) {
			Log.e(TAG, "Could not join audioReaderThread: " + e.getMessage());
		}
	}
	
	/**
	 * Ensure started. Ensuring that the audio analyzer thread is active when the app
	 * resumes its operation.
	 */
	public void ensureStarted() {
		Log.d(TAG, "Ensuring recording is on...");
		if(audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
			Log.d(TAG, "I was worth ensuring recording is on.");
			audioRecord.startRecording();
		}
		if(audioReaderThread == null) {
			startAudioReaderThread();
		} else if(!audioReaderThread.isAlive()) {
			startAudioReaderThread();
		}
		
	}
	
	/**
	 * Stop. Stops the audio reader thread.
	 */
	public void stop() { // onStop
		stopAudioReaderThread();
		audioRecord.stop();
	}
	
	@Override
	public void onMarkerReached(AudioRecord recorder) {
		Log.e(TAG, "This should never heppen - check AudioRecorded set up (notifications).");
		// This should never happen.
	}
	
	/**
	 * The Class ArrayToDump.
	 */
	public class ArrayToDump {
		
		/** The arr. */
		public double [] arr;
		
		/** The elements. */
		int elements;
		
		/**
		 * Instantiates a new array to dump.
		 *
		 * @param a the a
		 * @param e the e
		 */
		public ArrayToDump(double [] a, int e) {
			arr = a;
			elements = e;
		}
	}

	@Override
	public void onPeriodicNotification(AudioRecord recorder) {
		notifyObservers(analyzisResult);
	}
	
	// square.
	/**
	 * Sq.
	 *
	 * @param a the a
	 * @return the double
	 */
	private double sq(double a) { return a*a; }

	/** The current fft method size. */
	private int currentFftMethodSize = -1;
	
	
    /**
     * Hanning.
     *
     * @param n the n
     * @param N the n
     * @return the double
     */
    private double hanning(int n, int N) {
        return 0.5*(1.0 -Math.cos(2*Math.PI*(double)n/(double)(N-1)));
	}
	
	/**
	 * Compute autocorrelation.
	 */
	private void computeAutocorrelation() {
		if(2*elementsRead != currentFftMethodSize) {
			fft_method = new DoubleFFT_1D(2*elementsRead);
			currentFftMethodSize = 2*elementsRead;
		}
		
		// Check out memory layout of fft methods in Jtransforms.
		for(int i=elementsRead-1; i>=0; i--) {
			audioDataAnalyzis[2*i]=audioDataAnalyzis[i] * hanning(i,elementsRead);
			audioDataAnalyzis[2*i+1] = 0;
		}
		for(int i=2*elementsRead; i<audioDataAnalyzis.length; ++i) 
			audioDataAnalyzis[i]=0;
		
		// Compute FORWARD fft transform.
		fft_method.complexInverse(audioDataAnalyzis, false);

		// Replace every frequency with it's magnitude.
		for(int i=0; i<elementsRead; ++i) {
			audioDataAnalyzis[2*i] = sq(audioDataAnalyzis[2*i]) + sq(audioDataAnalyzis[2*i+1]);
			audioDataAnalyzis[2*i+1] = 0;
		}
		for(int i=2*elementsRead; i<audioDataAnalyzis.length; ++i) 
			audioDataAnalyzis[i]=0;
				
		// Set first one on to 0.
		audioDataAnalyzis[0] = 0;
		
		// Compute INVERSE fft.
		fft_method.complexForward(audioDataAnalyzis);
		
		// Take real part of the result.
		for(int i=0; i<elementsRead; ++i) 
			audioDataAnalyzis[i] = audioDataAnalyzis[2*i];
		for(int i=elementsRead; i<audioDataAnalyzis.length; ++i) 
			audioDataAnalyzis[i]=0;
	}
	
		
	/**
	 * Gets the mean wavelength.
	 *
	 * @return the mean wavelength
	 */
	double getMeanWavelength() {
		double mean = 0;
		for(int i=0; i < wavelengths; ++i) 
			mean += wavelength[i];
		mean/=(double)(wavelengths);
		return mean;
	}
	
	/**
	 * Gets the st dev on wavelength.
	 *
	 * @return the st dev on wavelength
	 */
	double getStDevOnWavelength() {
		double variance = 0; double mean = getMeanWavelength();
		for(int i=1; i < wavelengths; ++i)
			variance+= Math.pow(wavelength[i]-mean,2);
		variance/=(double)(wavelengths-1);
		return Math.sqrt(variance);
	}
	
	/**
	 * Removes the false samples.
	 */
	void removeFalseSamples() {
		int samplesToBeIgnored = 
			(int)(PercentOfWavelenghSamplesToBeIgnored*wavelengths);
		if(wavelengths <=2) return;
		do {
			double mean = getMeanWavelength();
			// Looking for sample furthest away from mean.
			int best = -1;
			for(int i=0; i<wavelengths; ++i)
				if(best == -1 || Math.abs((double)wavelength[i]-mean) > 
						Math.abs((double)wavelength[best]-mean)) best = i;
			// Removing it.
			wavelength[best]=wavelength[wavelengths-1];
			--wavelengths;
		} while(getStDevOnWavelength() > maxStDevOfMeanFrequency && 
				samplesToBeIgnored-- > 0 && wavelengths > 2);
	}
	
	/**
	 * Gets the frequency.
	 *
	 * @return the frequency
	 */
	private AnalyzedSound getFrequency() {
		elementsRead =
			audioData.getElements(audioDataAnalyzis,0,audioDataSize);
		int loudness = 0;
		for(int i=0; i<elementsRead; ++i)
			loudness+=Math.abs(audioDataAnalyzis[i]);
		loudness/=elementsRead;
		// Check loudness first - If the sound is less than the loudness threshold.
		if(loudness<loudnessThreshold)
			return new AnalyzedSound(loudness,ReadingType.TOO_QUIET);
		
		computeAutocorrelation();
		double maximum=0;
		for(int i=1; i<elementsRead; ++i)
			maximum = Math.max(audioDataAnalyzis[i], maximum);
		
		int lastStart = -1;
		wavelengths = 0;
		boolean passedZero = true;
		for(int i=0; i<elementsRead; ++i) {
			if(audioDataAnalyzis[i]*audioDataAnalyzis[i+1] <=0) passedZero = true;
			if(passedZero && audioDataAnalyzis[i] > MPM*maximum &&
					audioDataAnalyzis[i] > audioDataAnalyzis[i+1]) {
				if(lastStart != -1)
					wavelength[wavelengths++]=i-lastStart;
				lastStart=i; passedZero = false;
				maximum = audioDataAnalyzis[i];
			}
		}
		if(wavelengths <2)
			return new AnalyzedSound(loudness,ReadingType.ZERO_SAMPLES);

		removeFalseSamples();
		
		double mean = getMeanWavelength(), stdv=getStDevOnWavelength();
		
		double calculatedFrequency = (double)AUDIO_SAMPLING_RATE/mean;
		
		if(stdv >= maxStDevOfMeanFrequency) 
			return new AnalyzedSound(loudness,ReadingType.BIG_VARIANCE);
		else if(calculatedFrequency>MaxPossibleFrequency)
			return new AnalyzedSound(loudness,ReadingType.BIG_FREQUENCY);
		else
			return new AnalyzedSound(loudness, calculatedFrequency);
		
	}	
}
