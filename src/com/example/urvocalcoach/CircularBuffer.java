package com.example.urvocalcoach;

/**
 * The Class CircularBuffer. The circular buffer is used to store the stream of audio data.
 */
public class CircularBuffer {
	
	/** The array. */
	private short [] array;
	
	/** The head. */
	private int head;
	
	/** The size. */
	private int size;
	
	/** The available elements. */
	private int availableElements;
	
	/**
	 * Instantiates a new circular buffer.
	 *
	 * @param s the s
	 */
	public CircularBuffer(int s) {
		size = s;
		array = new short[size];
		head = 0;
		availableElements = 0;
	}
	
	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * Push.
	 *
	 * @param x the x
	 */
	public synchronized void push(short x) {
		array[head++] = x;
		if(head>=size) head-=size;
		availableElements = Math.min(availableElements+1, size);
	}
	
	/**
	 * Gets the elements.
	 *
	 * @param result the result
	 * @param offset the offset
	 * @param maxElements the max elements
	 * @return the elements
	 */
	public synchronized int getElements(double [] result, int offset, int maxElements) {
		int toRead = Math.min(maxElements, availableElements);
		int current = head - 1;
		for(int i=offset+toRead-1; i>=offset; --i) {
			if(current < 0) current+=size;
			result[i]=array[current--];
		}
		return toRead;
	}
}
