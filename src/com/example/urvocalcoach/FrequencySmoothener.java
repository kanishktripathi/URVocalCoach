package com.example.urvocalcoach;

import com.example.urvocalcoach.AudioAnalyzer.AnalyzedSound;

/**
 * The Class FrequencySmoothener.
 */
public class FrequencySmoothener {

	/** The Constant frequencyForgetting. how fast forget frequency */
	static final double FREQUENCYFORGETTING = 0.9;

	/** The Constant invalidDataAllowed. */
	static final int INVALIDDATAALLOWED = 6; 

	/** The smooth frequency. */
	static private double SMOOTHFREQUENCY = 0.0;

	/** The invalid data counter. */
	static private int INVALIDDATACOUNTER;

	/**
	 * Gets the smooth frequency.
	 * 
	 * @param result
	 *            the result
	 * @return the smooth frequency
	 */
	public static double getSmoothFrequency(AnalyzedSound result) {
		if (!result.isFrequencyAvailable()) {
			INVALIDDATACOUNTER = Math.min(INVALIDDATACOUNTER + 1,
					2 * INVALIDDATAALLOWED);
		} else {
			if (SMOOTHFREQUENCY == 0.0) {
				SMOOTHFREQUENCY = result.getFrequency();
			} else {
				SMOOTHFREQUENCY = (1 - FREQUENCYFORGETTING) * SMOOTHFREQUENCY
						+ FREQUENCYFORGETTING * result.getFrequency();
			}
			INVALIDDATACOUNTER = Math.max(INVALIDDATACOUNTER
					- INVALIDDATAALLOWED, 0);
		}
		if (INVALIDDATACOUNTER <= INVALIDDATAALLOWED) {
			return SMOOTHFREQUENCY;
		} else {
			SMOOTHFREQUENCY = 0.0;
			return Double.NaN;
		}
	}
}
